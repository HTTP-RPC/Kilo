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

import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.exceptions.JCodeModelException;
import com.helger.jcodemodel.writer.JCMWriter;
import org.httprpc.kilo.Name;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.web.WebServiceProxy;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import static org.httprpc.kilo.util.Optionals.*;

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
            generate(packageName, service);
        }

        for (var enumeration : enumerations.values()) {
            generate(packageName, enumeration);
        }

        for (var structure : structures.values()) {
            generate(packageName, structure);
        }
    }

    private static void generate(String packageName, ServiceDescriptor service) throws JCodeModelException, IOException {
        // TODO
    }

    private static void generate(String packageName, EnumerationDescriptor enumeration) throws JCodeModelException, IOException {
        var codeModel = new JCodeModel();

        var typeName = enumeration.getName();

        var enumType = codeModel._package(packageName)._enum(typeName);

        enumType.headerComment().add(String.format("Generated by %s.", ProxyGenerator.class.getName()));

        perform(enumeration.getDescription(), description -> enumType.javadoc().add(description));

        for (var constant : enumeration.getValues()) {
            var name = constant.getName();
            var identifier = constant.getIdentifier();

            var enumConstant = enumType.enumConstant(identifier);

            perform(constant.getDescription(), description -> enumConstant.javadoc().add(description));

            if (!name.equals(identifier)) {
                enumConstant.annotate(Name.class).param(name);
            }
        }

        build(packageName, typeName, codeModel);
    }

    private static void generate(String packageName, StructureDescriptor structure) throws JCodeModelException, IOException {
        // TODO
    }

    private static void build(String packageName, String typeName, JCodeModel codeModel) throws IOException{
        var workingPath = Path.of(System.getProperty("user.dir"));
        var packagePath = workingPath.resolve(packageName.replace(".", "/"));

        var filePath = packagePath.resolve(String.format("%s.java", typeName));

        var file = filePath.toFile();

        file.mkdirs();

        var writer = new JCMWriter(codeModel);

        writer.setJavaFeature(Runtime.version().feature());

        writer.build(workingPath.toFile());
    }
}
