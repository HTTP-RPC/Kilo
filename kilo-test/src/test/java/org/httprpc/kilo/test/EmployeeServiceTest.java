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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeServiceTest {
    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testEmployees() throws IOException {
        var t0 = System.currentTimeMillis();

        loadEmployees("employees");

        var t1 = System.currentTimeMillis();

        loadEmployees("employees/stream");

        var t2 = System.currentTimeMillis();

        assertTrue(t2 - t1 < t1 - t0);
    }

    private static void loadEmployees(String path) throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve(path));

        webServiceProxy.invoke();
    }

    @Test
    public void testAverageSalary() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("employees/average-salary"));

        var result = webServiceProxy.invoke();

        assertEquals(63810.74, result);
    }
}
