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
        String getDescription();

        String getIdentifier();

        boolean isDeprecated();

        TypeDescriptor getProduces();

        int getParameterCount();

        List<ParameterDescriptor> getPathParameters();
        List<ParameterDescriptor> getQueryParameters();

        ParameterDescriptor getBodyParameter();
    }

    public interface ParameterDescriptor {
        String getName();
        String getDescription();

        String getIdentifier();

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
        String getDescription();

        String getIdentifier();

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
        String getDescription();

        String getReadIdentifier();
        String getWriteIdentifier();

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

    private static class MethodDeclarationModifier implements TemplateEncoder.Modifier<OperationDescriptor> {
        @Override
        public Object apply(OperationDescriptor operation, String argument, Locale locale, TimeZone timeZone, boolean last) {
            // TODO
            return "";
        }
    }

    private static class ConstantDeclarationModifier implements TemplateEncoder.Modifier<ConstantDescriptor> {
        @Override
        public Object apply(ConstantDescriptor constant, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var resultBuilder = new StringBuilder(constant.getName());

            if (!last) {
                resultBuilder.append(", ");
            }

            return resultBuilder.toString();
        }
    }

    private static class SupertypeDeclarationModifier implements TemplateEncoder.Modifier<TypeDescriptor> {
        @Override
        public Object apply(TypeDescriptor type, String argument, Locale locale, TimeZone timeZone, boolean last) {
            var resultBuilder = new StringBuilder(type.getName());

            if (!last) {
                resultBuilder.append(", ");
            }

            return resultBuilder.toString();
        }
    }

    private static class AccessorDeclarationModifier implements TemplateEncoder.Modifier<PropertyDescriptor> {
        @Override
        public Object apply(PropertyDescriptor property, String argument, Locale locale, TimeZone timeZone, boolean last) {
            // TODO
            return "";
        }
    }

    private static class MutatorDeclarationModifier implements TemplateEncoder.Modifier<PropertyDescriptor> {
        @Override
        public Object apply(PropertyDescriptor property, String argument, Locale locale, TimeZone timeZone, boolean last) {
            // TODO
            return "";
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Base URI is required.");
            return;
        }

        if (args.length < 2) {
            System.out.println("Package name is required.");
            return;
        }

        var baseURI = URI.create(args[0]);

        var packageName = args[1];

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
                entry("methodDeclaration", new MethodDeclarationModifier())
            ), "service", service);
        }

        for (var enumeration : enumerations.values()) {
            generate(packageName, enumeration.getName(), "enumeration.java", mapOf(
                entry("constantDeclaration", new ConstantDeclarationModifier())
            ), "enumeration", enumeration);
        }

        for (var structure : structures.values()) {
            generate(packageName, structure.getName(), "structure.java", mapOf(
                entry("supertypeDeclaration", new SupertypeDeclarationModifier()),
                entry("accessorDeclaration", new AccessorDeclarationModifier()),
                entry("mutatorDeclaration", new MutatorDeclarationModifier())
            ), "structure", structure);
        }
    }

    private static void generate(String packageName, String typeName, String templateName,
        Map<String, TemplateEncoder.Modifier<?>> modifiers,
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
                entry("proxyGeneratorTypeName", ProxyGenerator.class.getSimpleName()),
                entry("packageName", packageName),
                entry(descriptorKey, descriptor)
            ), outputStream);
        }
    }
}
