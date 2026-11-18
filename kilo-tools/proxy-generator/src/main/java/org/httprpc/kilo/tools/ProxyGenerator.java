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
import org.httprpc.kilo.web.WebServiceProxy;

import java.net.URI;
import java.util.List;
import java.util.TreeMap;

public class ProxyGenerator {
    public interface ServiceDescriptor {
        String getPath();
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

        boolean isDeprecated();

        TypeDescriptor getProduces();

        boolean getParameters();

        List<ParameterDescriptor> getPathParameters();
        List<ParameterDescriptor> getQueryParameters();

        ParameterDescriptor getBodyParameter();
    }

    public interface ParameterDescriptor {
        String getName();
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
        String getDescription();

        boolean isDeprecated();
    }

    public interface StructureDescriptor {
        String getName();
        String getDescription();

        List<TypeDescriptor> getSupertypes();
        List<PropertyDescriptor> getProperties();
    }

    public interface PropertyDescriptor {
        String getName();
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
            System.out.println("Base URI is required.");
            return;
        }

        if (args.length < 2) {
            System.out.println("Package name is required.");
            return;
        }

        var baseURI = URI.create(args[0]);

        var packageName = args[1];

        var proxyTypeNames = new TreeMap<String, String>();
        var serviceDescriptors = new TreeMap<String, ServiceDescriptor>();

        var enumerations = new TreeMap<String, EnumerationDescriptor>();
        var structures = new TreeMap<String, StructureDescriptor>();

        for (var i = 2; i < args.length; i++) {
            var components = args[i].split(":");

            if (components.length != 2) {
                System.out.println("Invalid service mapping.");
                return;
            }

            var servicePath = components[0];
            var proxyTypeName = components[1];

            proxyTypeNames.put(servicePath, proxyTypeName);

            var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve(String.format("%s.json", servicePath)));

            var serviceDescriptor = BeanAdapter.coerce(webServiceProxy.invoke(), ServiceDescriptor.class);

            serviceDescriptors.put(servicePath, serviceDescriptor);

            for (var enumerationDescriptor : serviceDescriptor.getEnumerations()) {
                enumerations.put(enumerationDescriptor.getName(), enumerationDescriptor);
            }

            for (var structureDescriptor : serviceDescriptor.getStructures()) {
                structures.put(structureDescriptor.getName(), structureDescriptor);
            }
        }

        for (var entry : serviceDescriptors.entrySet()) {
            generateProxyType(packageName, proxyTypeNames.get(entry.getKey()), entry.getValue());
        }

        for (var value : enumerations.values()) {
            generateEnumeration(packageName, value);
        }

        for (var value : structures.values()) {
            generateStructure(packageName, value);
        }
    }

    private static void generateProxyType(String packageName, String proxyTypeName, ServiceDescriptor serviceDescriptor) {
        // TODO
    }

    private static void generateEnumeration(String packageName, EnumerationDescriptor enumerationDescriptor) {
        // TODO
    }

    private static void generateStructure(String packageName, StructureDescriptor structureDescriptor) {
        // TODO
    }
}
