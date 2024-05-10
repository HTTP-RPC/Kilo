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
import java.util.Map;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentationTest extends AbstractTest {
    @Test
    public void testDocumentation() throws IOException {
        testDocumentation("math");
        testDocumentation("file-upload");
        testDocumentation("catalog");
    }

    private void testDocumentation(String name) throws IOException {
        Map<?, ?> expected;
        try (var inputStream = getClass().getResourceAsStream(String.format("%s.json", name))) {
            var jsonDecoder = new JSONDecoder();

            expected = (Map<?, ?>)jsonDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, name);

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", "application/json")
        ));

        webServiceProxy.setArguments(mapOf(
            entry("api", "json")
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke();

        assertEquals(expected, actual);
    }
}
