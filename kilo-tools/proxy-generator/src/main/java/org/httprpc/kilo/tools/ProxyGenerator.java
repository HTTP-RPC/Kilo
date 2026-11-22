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

import org.httprpc.kilo.Name;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.web.WebServiceProxy;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.httprpc.kilo.util.Collections.*;

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

        boolean isMap();
        TypeDescriptor getKeyType();
        TypeDescriptor getValueType();
    }

    private static class MethodDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            // TODO
            return "";
        }
    }

    private static class TypeDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var type = BeanAdapter.coerce(value, TypeDescriptor.class);

            if (type.isIterable()) {
                var elementTypeName = type.getElementType().getName();

                return String.format("%s<%s>", Iterable.class.getSimpleName(), elementTypeName);
            } else if (type.isMap()) {
                var keyTypeName = type.getKeyType().getName();
                var valueTypeName = type.getValueType().getName();

                return String.format("%s<%s, %s>", Map.class.getSimpleName(), keyTypeName, valueTypeName);
            } else {
                return type.getName();
            }
        }
    }

    private static class ParameterDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            // TODO
            return "";
        }
    }

    private static class ConstantDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var constant = BeanAdapter.coerce(value, ConstantDescriptor.class);

            var resultBuilder = new StringBuilder(constant.getName());

            if (!last) {
                resultBuilder.append(", ");
            }

            return resultBuilder.toString();
        }
    }

    private static class SupertypeDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var resultBuilder = new StringBuilder(value.toString());

            if (!last) {
                resultBuilder.append(", ");
            }

            return resultBuilder.toString();
        }
    }

    private static class VariableDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var property = BeanAdapter.coerce(value, PropertyDescriptor.class);

            var identifer = property.getIdentifier();

            if (isUppercase(identifer)) {
                return identifer.toLowerCase();
            } else {
                return identifer;
            }
        }

        static boolean isUppercase(String value) {
            var n = value.length();

            for (var i = 0; i < n; i++) {
                if (Character.isLowerCase(value.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    private static class PropertyNameAnnotationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var property = BeanAdapter.coerce(value, PropertyDescriptor.class);

            var name = property.getName();
            var identifier = property.getIdentifier();

            if (!name.equals(identifier)) {
                return String.format("%s(\"%s\")", Name.class.getSimpleName(), name);
            } else {
                return "";
            }
        }
    }

    private static class RequiredPropertyAnnotationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var property = BeanAdapter.coerce(value, PropertyDescriptor.class);

            if (property.isRequired()) {
                return String.format("@%s", Required.class.getSimpleName());
            } else {
                return "";
            }
        }
    }

    private static class AccessorDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var property = BeanAdapter.coerce(value, PropertyDescriptor.class);

            var typeName = property.getType().getName();

            var resultBuilder = new StringBuilder();

            if (typeName.equals(Boolean.TYPE.getSimpleName()) || typeName.equals(Boolean.class.getSimpleName())) {
                resultBuilder.append("is");
            } else {
                resultBuilder.append("get");
            }

            var identifier = property.getIdentifier();

            resultBuilder.append(Character.toUpperCase(identifier.charAt(0)));
            resultBuilder.append(identifier.substring(1));

            return resultBuilder.toString();
        }
    }

    private static class MutatorDeclarationModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var property = BeanAdapter.coerce(value, PropertyDescriptor.class);

            var resultBuilder = new StringBuilder();

            resultBuilder.append("set");

            var identifier = property.getIdentifier();

            resultBuilder.append(Character.toUpperCase(identifier.charAt(0)));
            resultBuilder.append(identifier.substring(1));

            return resultBuilder.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Package name is required.");
            return;
        }

        if (args.length < 2) {
            System.out.println("Base URI is required.");
            return;
        }

        var packageName = args[0];

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
            generate(packageName, service.getName(), "service-proxy.java", mapOf(
                entry("methodDeclaration", new MethodDeclarationModifier()),
                entry("typeDeclaration", new TypeDeclarationModifier()),
                entry("parameterDeclaration", new ParameterDeclarationModifier())
            ), "service", service);
        }

        for (var enumeration : enumerations.values()) {
            generate(packageName, enumeration.getName(), "enumeration.java", mapOf(
                entry("constantDeclaration", new ConstantDeclarationModifier())
            ), "enumeration", enumeration);
        }

        for (var structure : structures.values()) {
            var templateName = structure.isConcrete() ? "concrete-structure.java" : "abstract-structure.java";

            generate(packageName, structure.getName(), templateName, mapOf(
                entry("supertypeDeclaration", new SupertypeDeclarationModifier()),
                entry("typeDeclaration", new TypeDeclarationModifier()),
                entry("variableDeclaration", new VariableDeclarationModifier()),
                entry("propertyNameAnnotation", new PropertyNameAnnotationModifier()),
                entry("requiredPropertyAnnotation", new RequiredPropertyAnnotationModifier()),
                entry("accessorDeclaration", new AccessorDeclarationModifier()),
                entry("mutatorDeclaration", new MutatorDeclarationModifier())
            ), "structure", structure);
        }
    }

    private static void generate(String packageName, String typeName, String templateName,
        Map<String, TemplateEncoder.Modifier> modifiers,
        String descriptorKey,
        Object descriptor) throws IOException {
        var workingPath = Path.of(System.getProperty("user.dir"));

        var packagePath = workingPath.resolve(packageName.replace(".", "/"));
        var filePath = packagePath.resolve(String.format("%s.java", typeName));

        var templateEncoder = new TemplateEncoder(ProxyGenerator.class, templateName);

        for (var entry : modifiers.entrySet()) {
            templateEncoder.bind(entry.getKey(), entry.getValue());
        }

        try (var outputStream = Files.newOutputStream(filePath,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
            templateEncoder.write(mapOf(
                entry("proxyGeneratorTypeName", ProxyGenerator.class.getName()),
                entry("packageName", packageName),
                entry(descriptorKey, descriptor)
            ), outputStream);
        }
    }
}
