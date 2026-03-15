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
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.JSONDecoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

public class EmployeeServiceTest {
    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testEmployeesList() throws IOException {
        loadEmployees(false);
    }

    @Test
    public void testEmployeesStream() throws IOException {
        loadEmployees(true);
    }

    private void loadEmployees(boolean stream) throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("employees"));

        webServiceProxy.setArguments(mapOf(
            entry("stream", stream)
        ));

        webServiceProxy.setResponseHandler((inputStream, contentType) -> {
            var jsonDecoder = new JSONDecoder();

            return countOf(jsonDecoder.readAll(inputStream));
        });

        assertEquals(300024, webServiceProxy.invoke());
    }

    @Test
    public void testEmployeeDetail() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve(String.format("employees/%d", 10001)));

        var employeeDetail = BeanAdapter.coerce(webServiceProxy.invoke(), EmployeeDetail.class);

        assertEquals(10001, employeeDetail.getEmployeeNumber());

        assertEquals("Georgi", employeeDetail.getFirstName());
        assertEquals("Facello", employeeDetail.getLastName());

        var salaries = employeeDetail.getSalaries();

        assertEquals(17, salaries.size());

        var first = salaries.getFirst();

        assertEquals(60117, first.salary());
        assertEquals(LocalDate.of(1986, 6, 26), first.fromDate());
        assertEquals(LocalDate.of(1987, 6, 26), first.toDate());

        var last = salaries.getLast();

        assertEquals(88958, last.salary());
        assertEquals(LocalDate.of(2002, 6, 22), last.fromDate());
        assertEquals(LocalDate.of(9999, 1, 1), last.toDate());
    }
}
