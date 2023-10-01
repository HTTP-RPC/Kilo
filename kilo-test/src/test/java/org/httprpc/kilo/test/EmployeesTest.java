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

public class EmployeesTest {
    public static void main(String[] args) throws IOException {
        var baseURL = new URL("http://localhost:8080/kilo-test/");

        var t0 = System.currentTimeMillis();

        logTiming(baseURL, "employees", t0);

        var t1 = System.currentTimeMillis();

        logTiming(baseURL, "employees/stream", t1);

        var t2 = System.currentTimeMillis();

        logTiming(baseURL, "employees/hibernate", t2);

        var t3 = System.currentTimeMillis();

        logTiming(baseURL, "employees/hibernate-stream", t3);
    }

    private static void logTiming(URL baseURL, String path, long start) throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, path);

        var result = (List<?>)webServiceProxy.invoke();

        var current = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d rows from %s in %.1fs", result.size(), path, (current - start) / 1000.0));
    }
}
