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
import java.net.URI;
import java.util.List;

public class EmployeesTest {
    public static void main(String[] args) throws IOException {
        var baseURI = URI.create("http://localhost:8080/kilo-test/");

        logTiming(baseURI, "employees");
        logTiming(baseURI, "employees/stream");
        logTiming(baseURI, "employees/hibernate");
        logTiming(baseURI, "employees/hibernate-stream");
    }

    private static void logTiming(URI baseURI, String path) throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve(path));

        var t0 = System.currentTimeMillis();

        var result = (List<?>)webServiceProxy.invoke();

        var t1 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d rows from %s in %.1fs", result.size(), path, (t1 - t0) / 1000.0));
    }
}
