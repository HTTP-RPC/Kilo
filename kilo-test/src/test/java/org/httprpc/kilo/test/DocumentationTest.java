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
import org.httprpc.kilo.io.TextDecoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentationTest {
    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testBulkUploadService() throws IOException {
        testDocumentation("bulk-upload");
    }

    @Test
    public void testCatalogService() throws IOException {
        testDocumentation("catalog");
    }

    @Test
    public void testEmployeeService() throws IOException {
        testDocumentation("employees");
    }

    @Test
    public void testFilmService() throws IOException {
        testDocumentation("films");
    }

    @Test
    public void testMathService() throws IOException {
        testDocumentation("math");
    }

    @Test
    public void testMemberService() throws IOException {
        testDocumentation("members");
    }

    @Test
    public void testPetService() throws IOException {
        testDocumentation("pets");
    }

    @Test
    public void testSalaryService() throws IOException {
        testDocumentation("salaries");
    }

    @Test
    public void testTestService() throws IOException {
        testDocumentation("test");
    }

    private void testDocumentation(String name) throws IOException {
        String expected;
        try (var inputStream = getClass().getResourceAsStream(String.format("api/%s.html", name))) {
            var textDecoder = new TextDecoder();

            expected = textDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve(String.format("%s.html", name)));

        webServiceProxy.setResponseHandler((inputStream, contentType) -> {
            var textDecoder = new TextDecoder();

            return textDecoder.read(inputStream);
        });

        var actual = webServiceProxy.invoke();

        assertEquals(expected, actual);
    }
}
