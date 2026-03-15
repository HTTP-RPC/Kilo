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
import org.httprpc.kilo.io.JSONDecoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

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

    private static void loadEmployees(boolean stream) throws IOException {
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
}
