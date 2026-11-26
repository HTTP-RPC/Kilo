/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo.tools;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.exceptions.JCodeModelException;
import com.helger.jcodemodel.vars.JFieldVar;
import com.helger.jcodemodel.writer.JCMWriter;
import org.httprpc.kilo.Name;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.web.RequestMethod;
import org.httprpc.kilo.web.ResourcePath;
import org.httprpc.kilo.web.ServicePath;
import org.httprpc.kilo.web.WebServiceProxy;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

public class ProxyGenerator {
    public interface ServiceDescriptor {
        String getPath();

        String getName();
        String getDescription();

        List<EndpointDescriptor> getEndpoints();
        List<EnumerationDescriptor> getEnumerations();
        List<StructureDescriptor> getStructures();
    }

    public interface EndpointDescriptor {
        String getPath();

        List<OperationDescriptor> getOperations();
    }

    public interface OperationDescriptor {
        String getMethod();
        String getIdentifier();

        String getDescription();

        boolean isDeprecated();

        TypeDescriptor getProduces();

        int getParameterCount();

        List<ParameterDescriptor> getPathParameters();
        List<ParameterDescriptor> getQueryParameters();

        ParameterDescriptor getBodyParameter();
    }

    public interface ParameterDescriptor {
        String getName();
        String getIdentifier();

        String getDescription();

        boolean isRequired();

        TypeDescriptor getType();
    }

    public interface EnumerationDescriptor {
        String getName();
        String getDescription();

        List<ConstantDescriptor> getValues();
    }

    public interface ConstantDescriptor {
        String getName();
        String getIdentifier();

        String getDescription();

        boolean isDeprecated();
    }

    public interface StructureDescriptor {
        String getName();
        String getDescription();

        boolean isConcrete();

        List<TypeDescriptor> getSupertypes();
        List<PropertyDescriptor> getProperties();
    }

    public interface PropertyDescriptor {
        String getName();
        String getIdentifier();

        String getDescription();

        boolean isRequired();
        boolean isDeprecated();

        TypeDescriptor getType();
    }

    public interface TypeDescriptor {
        String getName();

        boolean isIntrinsic();

        boolean isIterable();
        TypeDescriptor getElementType();

        boolean isUnique();

        boolean isMap();
        TypeDescriptor getKeyType();
        TypeDescriptor getValueType();

        default boolean isBoolean() {
            var name = getName();

            return name.equals(Boolean.TYPE.getSimpleName()) || name.equals(Boolean.class.getSimpleName());
        }

        default AbstractJClass describe(JCodeModel codeModel) {
            if (isIterable()) {
                return codeModel.ref(isUnique() ? Set.class : List.class).narrow(getElementType().describe(codeModel));
            } else if (isMap()) {
                return codeModel.ref(Map.class).narrow(getKeyType().describe(codeModel), getValueType().describe(codeModel));
            } else {
                var name = getName();

                var intrinsicType = intrinsicTypes.get(name);

                if (intrinsicType != null) {
                    return codeModel.ref(intrinsicType);
                } else {
                    return codeModel.directClass(String.format("%s.%s", packageName, name));
                }
            }
        }
    }

    private static Map<String, Class<?>> intrinsicTypes = immutableMapOf(mapAll(listOf(
        Void.class,
        Object.class,
        Number.class,
        Byte.class,
        Short.class,
        Integer.class,
        Long.class,
        Float.class,
        Double.class,
        Boolean.class,
        Character.class,
        CharSequence.class,
        String.class,
        Date.class,
        Instant.class,
        LocalDate.class,
        LocalTime.class,
        LocalDateTime.class,
        Duration.class,
        Period.class,
        UUID.class,
        Path.class,
        Document.class
    ), type -> entry(type.getSimpleName(), type)));

    private static String packageName = null;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Package name is required.");
            return;
        }

        if (args.length < 2) {
            System.out.println("Base URI is required.");
            return;
        }

        packageName = args[0];

        var baseURI = URI.create(args[1]);

        var services = new TreeMap<String, ServiceDescriptor>();
        var enumerations = new TreeMap<String, EnumerationDescriptor>();
        var structures = new TreeMap<String, StructureDescriptor>();

        for (var i = 2; i < args.length; i++) {
            var servicePath = args[i];

            var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve(String.format("%s.json", servicePath)));

            var serviceDescriptor = BeanAdapter.coerce(webServiceProxy.invoke(), ServiceDescriptor.class);

            services.put(servicePath, serviceDescriptor);

            for (var enumerationDescriptor : serviceDescriptor.getEnumerations()) {
                enumerations.put(enumerationDescriptor.getName(), enumerationDescriptor);
            }

            for (var structureDescriptor : serviceDescriptor.getStructures()) {
                structures.put(structureDescriptor.getName(), structureDescriptor);
            }
        }

        for (var service : services.values()) {
            buildServiceProxy(packageName, service);
        }

        for (var enumeration : enumerations.values()) {
            buildEnumeration(packageName, enumeration);
        }

        for (var structure : structures.values()) {
            if (structure.isConcrete()) {
                buildConcreteStructure(packageName, structure);
            } else {
                buildAbstractStructure(packageName, structure);
            }
        }
    }

    private static void buildServiceProxy(String packageName, ServiceDescriptor service) throws JCodeModelException, IOException {
        var codeModel = new JCodeModel();

        var typeName = service.getName();

        var proxyType = codeModel._package(packageName)._interface(String.format("%sProxy", typeName));

        addHeaderComment(proxyType);

        var typeDescription = service.getDescription();

        if (typeDescription != null) {
            proxyType.javadoc().add(typeDescription);
        }

        var servicePath = service.getPath();

        var n = servicePath.length();

        proxyType.annotate(ServicePath.class).param(servicePath);

        for (var endpoint : service.getEndpoints()) {
            var endpointPath = endpoint.getPath();

            if (endpointPath.length() > n) {
                endpointPath = endpoint.getPath().substring(n + 1);
            } else {
                endpointPath = "";
            }

            for (var operation : endpoint.getOperations()) {
                var method = proxyType.method(0, operation.getProduces().describe(codeModel), operation.getIdentifier());

                var methodDescription = operation.getDescription();

                if (methodDescription != null) {
                    method.javadoc().add(methodDescription);
                }

                method.annotate(RequestMethod.class).param(operation.getMethod());

                if (!endpointPath.isEmpty()) {
                    method.annotate(ResourcePath.class).param(endpointPath);
                }

                for (var parameter : operation.getPathParameters()) {
                    addParameter(method, parameter, codeModel);
                }

                for (var parameter : operation.getQueryParameters()) {
                    addParameter(method, parameter, codeModel);
                }

                var bodyParameter = operation.getBodyParameter();

                if (bodyParameter != null) {
                    addParameter(method, bodyParameter, codeModel);
                }

                method._throws(IOException.class);
            }
        }

        build(packageName, typeName, codeModel);
    }

    private static void addParameter(JMethod method, ParameterDescriptor parameter, JCodeModel codeModel) {
        var name = parameter.getName();
        var description = parameter.getDescription();

        var identifier = parameter.getIdentifier();

        var variable = method.param(parameter.getType().describe(codeModel), getVariableName(identifier));

        if (description != null) {
            method.javadoc().addParam(variable).add(description);
        }

        if (!name.equals(identifier)) {
            variable.annotate(Name.class).param(name);
        }

        if (parameter.isRequired()) {
            variable.annotate(Required.class);
        }
    }

    private static void buildEnumeration(String packageName, EnumerationDescriptor enumeration) throws JCodeModelException, IOException {
        var codeModel = new JCodeModel();

        var typeName = enumeration.getName();

        var enumType = codeModel._package(packageName)._enum(typeName);

        addHeaderComment(enumType);

        var typeDescription = enumeration.getDescription();

        if (typeDescription != null) {
            enumType.javadoc().add(typeDescription);
        }

        for (var constant : enumeration.getValues()) {
            var name = constant.getName();
            var identifier = constant.getIdentifier();

            var enumConstant = enumType.enumConstant(identifier);

            var constantDescription = constant.getDescription();

            if (constantDescription != null) {
                enumConstant.javadoc().add(constantDescription);
            }

            if (!name.equals(identifier)) {
                enumConstant.annotate(Name.class).param(name);
            }
        }

        build(packageName, typeName, codeModel);
    }

    private static void buildConcreteStructure(String packageName, StructureDescriptor structure) throws JCodeModelException, IOException {
        var codeModel = new JCodeModel();

        var typeName = structure.getName();

        var beanType = codeModel._package(packageName)._class(typeName);

        addHeaderComment(beanType);

        var supertypes = structure.getSupertypes();

        if (!supertypes.isEmpty()) {
            beanType._extends(supertypes.getFirst().describe(codeModel));
        }

        var typeDescription = structure.getDescription();

        if (typeDescription != null) {
            beanType.javadoc().add(typeDescription);
        }

        var fields = new LinkedHashMap<String, JFieldVar>();

        for (var property : structure.getProperties()) {
            var identifier = property.getIdentifier();

            var propertyType = property.getType();

            var fieldName = getVariableName(identifier);

            fields.put(fieldName, (JFieldVar)beanType.field(JMod.PRIVATE, propertyType.describe(codeModel), fieldName));
        }

        for (var property : structure.getProperties()) {
            var name = property.getName();
            var identifier = property.getIdentifier();

            var propertyType = property.getType();

            var modelType = propertyType.describe(codeModel);

            var fieldName = getVariableName(identifier);

            var field = fields.get(fieldName);

            var accessorMethod = beanType.method(JMod.PUBLIC, modelType, getMethodName(propertyType.isBoolean() ? "is" : "get", identifier));

            accessorMethod.body()._return(field);

            var mutatorMethod = beanType.method(JMod.PUBLIC, Void.TYPE, getMethodName("set", identifier));

            var variable = mutatorMethod.param(modelType, fieldName);

            mutatorMethod.body().assign(JExpr._this().ref(field), variable);

            var propertyDescription = property.getDescription();

            if (propertyDescription != null) {
                accessorMethod.javadoc().add(String.format("%s (read)", propertyDescription));

                mutatorMethod.javadoc().add(String.format("%s (write)", propertyDescription));
                mutatorMethod.javadoc().addParam(variable).add(propertyDescription);
            }

            if (!name.equals(identifier)) {
                accessorMethod.annotate(Name.class).param(name);
            }

            if (property.isRequired()) {
                accessorMethod.annotate(Required.class);
            }
        }

        build(packageName, typeName, codeModel);
    }

    private static void buildAbstractStructure(String packageName, StructureDescriptor structure) throws JCodeModelException, IOException {
        var codeModel = new JCodeModel();

        var typeName = structure.getName();

        var beanType = codeModel._package(packageName)._interface(typeName);

        addHeaderComment(beanType);

        for (var supertype : structure.getSupertypes()) {
            beanType._implements(supertype.describe(codeModel));
        }

        var typeDescription = structure.getDescription();

        if (typeDescription != null) {
            beanType.javadoc().add(typeDescription);
        }

        for (var property : structure.getProperties()) {
            var name = property.getName();
            var identifier = property.getIdentifier();

            var propertyType = property.getType();

            var modelType = propertyType.describe(codeModel);

            var accessorMethod = beanType.method(0, modelType, getMethodName(propertyType.isBoolean() ? "is" : "get", identifier));

            var mutatorMethod = beanType.method(0, Void.TYPE, getMethodName("set", identifier));

            var variable = mutatorMethod.param(modelType, getVariableName(identifier));

            var propertyDescription = property.getDescription();

            if (propertyDescription != null) {
                accessorMethod.javadoc().add(String.format("%s (read)", propertyDescription));

                mutatorMethod.javadoc().add(String.format("%s (write)", propertyDescription));
                mutatorMethod.javadoc().addParam(variable).add(propertyDescription);
            }

            if (!name.equals(identifier)) {
                accessorMethod.annotate(Name.class).param(name);
            }

            if (property.isRequired()) {
                accessorMethod.annotate(Required.class);
            }
        }

        build(packageName, typeName, codeModel);
    }

    private static void addHeaderComment(JDefinedClass type) {
        type.headerComment().add(String.format("Generated by %s.", ProxyGenerator.class.getName()));
    }

    private static String getVariableName(String identifier) {
        var variableNameBuilder = new StringBuilder();

        var n = identifier.length();

        var i = 0;

        while (i < n) {
            var c = identifier.charAt(i++);

            if (Character.isUpperCase(c) && (i == n || Character.isUpperCase(identifier.charAt(i)))) {
                variableNameBuilder.append(Character.toLowerCase(c));
            } else {
                variableNameBuilder.append(c);

                break;
            }
        }

        variableNameBuilder.append(identifier.substring(i));

        return variableNameBuilder.toString();
    }

    private static String getMethodName(String prefix, String identifier) {
        var methodNameBuilder = new StringBuilder(prefix);

        methodNameBuilder.append(Character.toUpperCase(identifier.charAt(0)));
        methodNameBuilder.append(identifier.substring(1));

        return methodNameBuilder.toString();
    }

    private static void build(String packageName, String typeName, JCodeModel codeModel) throws IOException{
        var writer = new JCMWriter(codeModel);

        writer.setJavaFeature(Runtime.version().feature());

        writer.build(new File(System.getProperty("user.dir")));
    }
}
