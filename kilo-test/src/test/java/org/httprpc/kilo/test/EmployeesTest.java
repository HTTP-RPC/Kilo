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
        var baseURL = new URL("http://localhost:8080/kilo-test/employees/");

        var t0 = System.currentTimeMillis();

        var list1 = (List<?>)WebServiceProxy.get(baseURL).invoke();

        var t1 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d rows in %.1fs", list1.size(), (t1 - t0) / 1000.0));

        var list2 = (List<?>)WebServiceProxy.get(baseURL, "stream").invoke();

        var t2 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d streamed rows in %.1fs", list2.size(), (t2 - t1) / 1000.0));

        var list3 = (List<?>)WebServiceProxy.get(baseURL, "hibernate").invoke();

        var t3 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d Hibernate rows in %.1fs", list3.size(), (t3 - t2) / 1000.0));

        var list4 = (List<?>)WebServiceProxy.get(baseURL, "hibernate-stream").invoke();

        var t4 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d streamed Hibernate rows in %.1fs", list4.size(), (t4 - t3) / 1000.0));
    }
}
