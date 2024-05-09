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

import java.io.IOException;
import java.util.List;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.io.CSVDecoder;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.TextDecoder;
import org.junit.jupiter.api.Test;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class PetsTest extends AbstractTest {
    @Test
    public void testPets() throws IOException {
        testPetsJSON();

        testPetsStreamJSON();
        testPetsStreamCSV();
        testPetsStreamHTML();
        testPetsStreamXML();
    }

    private void testPetsJSON() throws IOException {
        List<?> expected;
        try (var inputStream = getClass().getResourceAsStream("pets.json")) {
            var jsonDecoder = new JSONDecoder();

            expected = (List<?>)jsonDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets");

        webServiceProxy.setArguments(mapOf(
            entry("owner", "Gwen")
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke();

        assertEquals(expected, actual);
    }

    private void testPetsStreamJSON() throws IOException {
        List<?> expected;
        try (var inputStream = getClass().getResourceAsStream("pets.json")) {
            var jsonDecoder = new JSONDecoder();

            expected = (List<?>)jsonDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets/stream");

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", "application/json")
        ));

        webServiceProxy.setArguments(mapOf(
            entry("owner", "Gwen")
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke();

        assertEquals(expected, actual);
    }

    private void testPetsStreamCSV() throws IOException {
        List<?> expected;
        try (var inputStream = getClass().getResourceAsStream("pets.csv")) {
            var csvDecoder = new CSVDecoder();

            expected = csvDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets/stream");

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", "text/csv")
        ));

        webServiceProxy.setArguments(mapOf(
            entry("owner", "Gwen")
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke((inputStream, contentType) -> {
            var csvDecoder = new CSVDecoder();

            return csvDecoder.read(inputStream);
        });

        assertEquals(expected, actual);

    }

    private void testPetsStreamHTML() throws IOException {
        testPetsStreamMarkup("pets.html", "text/html");
    }

    private void testPetsStreamXML() throws IOException {
        testPetsStreamMarkup("pets.xml", "text/xml");
    }

    private void testPetsStreamMarkup(String name, String mimeType) throws IOException {
        String expected;
        try (var inputStream = getClass().getResourceAsStream(name)) {
            var textDecoder = new TextDecoder();

            expected = textDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets/stream");

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", mimeType)
        ));

        webServiceProxy.setArguments(mapOf(
            entry("owner", "Gwen")
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke((inputStream, contentType) -> {
            var textDecoder = new TextDecoder();

            return textDecoder.read(inputStream);
        });

        assertEquals(expected, actual);
    }
}
