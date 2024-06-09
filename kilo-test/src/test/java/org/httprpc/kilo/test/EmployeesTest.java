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

package org.httprpc.kilo.test;

import org.httprpc.kilo.WebServiceProxy;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.*;

public class EmployeesTest {
    public static void main(String[] args) throws IOException {
        var baseURL = new URL("http://localhost:8080/kilo-test/");

        logTiming(baseURL, "employees");
        logTiming(baseURL, "employees/stream");

        logTiming(baseURL, "employees/stream-custom", mapOf(
            entry("propertyNames", listOf(
                "firstName",
                "lastName",
                "gender",
                "birthDate",
                "hireDate"
            ))
        ));

        logTiming(baseURL, "employees/stream-custom", mapOf(
            entry("propertyNames", listOf(
                "firstName",
                "lastName"
            ))
        ));

        logTiming(baseURL, "employees/hibernate");
        logTiming(baseURL, "employees/hibernate-stream");

        logTiming(baseURL, "employees/hibernate-stream-custom", mapOf(
            entry("propertyNames", listOf(
                "firstName",
                "lastName",
                "gender",
                "birthDate",
                "hireDate"
            ))
        ));

        logTiming(baseURL, "employees/hibernate-stream-custom", mapOf(
            entry("propertyNames", listOf(
                "firstName",
                "lastName"
            ))
        ));
    }

    private static void logTiming(URL baseURL, String path) throws IOException {
        logTiming(baseURL, path, mapOf());
    }

    private static void logTiming(URL baseURL, String path, Map<String, Object> arguments) throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, path);

        webServiceProxy.setArguments(arguments);

        var t0 = System.currentTimeMillis();

        var result = (List<?>)webServiceProxy.invoke();

        var t1 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d rows from %s in %.1fs", result.size(), path, (t1 - t0) / 1000.0));
    }
}
