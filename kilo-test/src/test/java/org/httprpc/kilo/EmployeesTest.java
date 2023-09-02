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

package org.httprpc.kilo;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

public class EmployeesTest {
    public static void main(String[] args) throws IOException {
        var baseURL = new URL("http://localhost:8080/kilo-test/employees");

        var t0 = System.currentTimeMillis();

        var list1 = WebServiceProxy.get(baseURL).setArguments(mapOf(
            entry("stream", false)
        )).invoke(List.class, Object.class);

        var t1 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d unstreamed rows in %.1fs", list1.size(), (t1 - t0) / 1000.0));

        var list2 = WebServiceProxy.get(baseURL).setArguments(mapOf(
            entry("stream", true)
        )).invoke(List.class, Object.class);

        var t2 = System.currentTimeMillis();

        System.out.println(String.format("Retrieved %d streamed rows in %.1fs", list1.size(), (t2 - t1) / 1000.0));
    }
}
