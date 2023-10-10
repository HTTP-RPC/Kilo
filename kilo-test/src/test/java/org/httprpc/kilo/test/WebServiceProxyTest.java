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

import org.httprpc.kilo.WebServiceException;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.CSVDecoder;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.io.TextEncoder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class WebServiceProxyTest {
    public static class CustomException extends IOException {
        public CustomException(String message) {
            super(message);
        }
    }

    private URL baseURL;

    private DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
    private Date date = new Date();
    private Instant instant = Instant.ofEpochMilli(1);
    private LocalDate localDate = LocalDate.now();
    private LocalTime localTime = LocalTime.now();
    private LocalDateTime localDateTime = LocalDateTime.now();
    private Duration duration = Duration.ofHours(2);
    private Period period = Period.ofDays(4);
    private UUID uuid = UUID.randomUUID();

    private static final int EOF = -1;

    public WebServiceProxyTest() throws IOException {
        baseURL = new URL("http://localhost:8080/kilo-test/");
    }

    @Test
    public void testGet() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test");

        webServiceProxy.setArguments(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("dayOfWeek", dayOfWeek),
            entry("instant", instant),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("duration", duration),
            entry("period", period),
            entry("uuid", uuid)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("dayOfWeek", dayOfWeek.toString()),
            entry("date", date.getTime()),
            entry("instant", instant.toString()),
            entry("localDate", localDate.toString()),
            entry("localTime", localTime.toString()),
            entry("localDateTime", localDateTime.toString()),
            entry("duration", duration.toString()),
            entry("period", period.toString()),
            entry("uuid", uuid.toString())
        ), result);
    }

    @Test
    public void testKeys() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test/a/%d/b/%s/c/%d/d/%s",
            123,
            URLEncoder.encode("héllo", StandardCharsets.UTF_8),
            456,
            URLEncoder.encode("göodbye", StandardCharsets.UTF_8)
        );

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("a", 123),
            entry("b", "héllo"),
            entry("c", 456),
            entry("d", "göodbye")
        ), result);
    }

    @Test
    public void testKeysProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, new URL(baseURL, "test/"));

        var result = testServiceProxy.testKeys(123, "héllo", 456,"göodbye");

        assertEquals(mapOf(
            entry("a", 123),
            entry("b", "héllo"),
            entry("c", 456),
            entry("d", "göodbye")
        ), result);
    }

    @Test
    public void testParameters() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test/foo/%d/bar/%d", 1, 2);

        webServiceProxy.setArguments(mapOf(
            entry("a", 3),
            entry("b", 4)
        ));

        webServiceProxy.setBody(listOf(5.0, 6.0, 7.0));

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("x", 1),
            entry("y", 2),
            entry("a", 3),
            entry("b", 4),
            entry("values", listOf(5.0, 6.0, 7.0))
        ), result);
    }

    @Test
    public void testParametersProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, new URL(baseURL, "test/"));

        var result = testServiceProxy.testParameters(1, 2, 3, 4, listOf(5.0, 6.0, 7.0));

        assertEquals(mapOf(
            entry("x", 1),
            entry("y", 2),
            entry("a", 3),
            entry("b", 4),
            entry("values", listOf(5.0, 6.0, 7.0))
        ), result);
    }

    @Test
    public void testGetFibonacci() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test/fibonacci");

        webServiceProxy.setArguments(mapOf(
            entry("count", 8)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13), result);
    }

    @Test
    public void testURLEncodedPost() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test");

        webServiceProxy.setArguments(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("dayOfWeek", dayOfWeek),
            entry("date", date),
            entry("instant", instant),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("duration", duration),
            entry("period", period),
            entry("uuid", uuid)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("dayOfWeek", dayOfWeek.toString()),
            entry("date", date.getTime()),
            entry("instant", instant.toString()),
            entry("localDate", localDate.toString()),
            entry("localTime", localTime.toString()),
            entry("localDateTime", localDateTime.toString()),
            entry("duration", duration.toString()),
            entry("period", period.toString()),
            entry("uuid", uuid.toString()),
            entry("attachmentInfo", listOf())
        ), result);
    }

    @Test
    public void testMultipartPost() throws IOException {
        var textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        var imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test");

        webServiceProxy.setEncoding(WebServiceProxy.Encoding.MULTIPART_FORM_DATA);

        webServiceProxy.setArguments(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("dayOfWeek", dayOfWeek),
            entry("date", date),
            entry("instant", instant),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("duration", duration),
            entry("period", period),
            entry("uuid", uuid),
            entry("attachments", listOf(textTestURL, imageTestURL))
        ));

        var response = webServiceProxy.invoke(result -> BeanAdapter.coerce(result, TestService.Response.class));

        assertTrue(response.getString().equals("héllo&gøod+bye?")
            && response.getStrings().equals(listOf("a", "b", "c"))
            && response.getNumber() == 123
            && response.getFlag()
            && response.getDayOfWeek().equals(dayOfWeek)
            && response.getDate().equals(date)
            && response.getInstant().equals(instant)
            && response.getLocalDate().equals(localDate)
            && response.getLocalTime().equals(localTime)
            && response.getLocalDateTime().equals(localDateTime)
            && response.getDuration().equals(duration)
            && response.getPeriod().equals(period)
            && response.getUUID().equals(uuid)
            && response.getAttachmentInfo().get(0).getBytes() == 26
            && response.getAttachmentInfo().get(0).getChecksum() == 2412
            && response.getAttachmentInfo().get(1).getBytes() == 10392
            && response.getAttachmentInfo().get(1).getChecksum() == 1038036);
    }

    @Test
    public void testListPost() throws IOException {
        var body = listOf(1, 2, 3);

        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/list");

        webServiceProxy.setBody(body);

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(body, BeanAdapter.coerceList((List<?>)result, Integer.class));
    }

    @Test
    public void testInvalidListPost() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/list");

        webServiceProxy.setBody("xyz");

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testMalformedListPost() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/list");

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return "application/json";
            }

            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                var textEncoder = new TextEncoder();

                textEncoder.write("xyz", outputStream);
            }
        });

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testMapPost() throws IOException {
        var body = mapOf(
            entry("a", 1.0),
            entry("b", 2.0),
            entry("c", 3.0)
        );

        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/map");

        webServiceProxy.setBody(body);

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(body, result);
    }

    @Test
    public void testBodyPost() throws IOException {
        var request = mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true)
        );

        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/body");

        webServiceProxy.setBody(request);

        webServiceProxy.setMonitorStream(System.out);

        var body = webServiceProxy.invoke(result -> BeanAdapter.coerce(result, TestService.Body.class));

        assertEquals(request.get("string"), body.getString());
        assertEquals(request.get("strings"), body.getStrings());
        assertEquals(request.get("number"), body.getNumber());
        assertEquals(request.get("flag"), body.getFlag());
    }

    @Test
    public void testCoordinatesPost() throws IOException {
        var request = mapOf(
            entry("x", 1),
            entry("y", 2)
        );

        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/coordinates");

        webServiceProxy.setBody(request);

        webServiceProxy.setMonitorStream(System.out);

        var coordinates = webServiceProxy.invoke(result -> BeanAdapter.coerce(result, Coordinates.class));

        assertEquals(request.get("x"), coordinates.x());
        assertEquals(request.get("y"), coordinates.y());
    }

    @Test
    public void testImagePost() throws IOException {
        var imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/image");

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                try (var inputStream = imageTestURL.openStream()) {
                    int b;
                    while ((b = inputStream.read()) != EOF) {
                        outputStream.write(b);
                    }
                }
            }
        });

        var image = webServiceProxy.invoke((inputStream, contentType) -> ImageIO.read(inputStream));

        assertNotNull(image);
    }

    @Test
    public void testPut() throws IOException {
        var textTestURL = WebServiceProxyTest.class.getResource("test.txt");

        var webServiceProxy = new WebServiceProxy("PUT", baseURL, "test");

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                try (var inputStream = textTestURL.openStream()) {
                    int b;
                    while ((b = inputStream.read()) != EOF) {
                        outputStream.write(b);
                    }
                }
            }
        });

        webServiceProxy.setMonitorStream(System.out);

        var text = webServiceProxy.invoke((inputStream, contentType) -> {
            var textDecoder = new TextDecoder();

            return textDecoder.read(inputStream);
        });

        assertNotNull(text);
    }

    @Test
    public void testDelete() throws IOException {
        var id = 101;

        var webServiceProxy = new WebServiceProxy("DELETE", baseURL, "test/%d", id);

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(id, result);
    }

    @Test
    public void testHeaders() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test/headers");

        webServiceProxy.setHeaders(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", 123)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", "123")
        ), result);
    }

    @Test
    public void testHeadersProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, new URL(baseURL, "test/"), webServiceProxy -> {
            webServiceProxy.setHeaders(mapOf(
                entry("X-Header-A", "abc"),
                entry("X-Header-B", 123)
            ));

            webServiceProxy.setMonitorStream(System.out);
        });

        var result = testServiceProxy.testHeaders();

        assertEquals(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", "123")
        ), result);
    }

    @Test
    public void testException() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test/error");

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(500, exception.getStatusCode());
        }
    }

    @Test
    public void testInvalidNumberArgument() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test");

        webServiceProxy.setArguments(mapOf(
            entry("string", "abc"),
            entry("number", "x")
        ));

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testInvalidDayOfWeekArgument() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test");

        webServiceProxy.setArguments(mapOf(
            entry("string", "abc"),
            entry("dayOfWeek", "y")
        ));

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testInvalidLocalDateArgument() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test");

        webServiceProxy.setArguments(mapOf(
            entry("string", "abc"),
            entry("localDate", "z")
        ));

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testMissingRequiredParameter() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test");

        webServiceProxy.setArguments(mapOf(
            entry("number", 5)
        ));

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testMissingRequiredParameterProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, new URL(baseURL, "test/"));

        assertThrows(IllegalArgumentException.class, () -> testServiceProxy.testGet(null, null, 0));
    }

    @Test
    public void testMissingRequiredProperty() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURL, "test/body");

        webServiceProxy.setBody(mapOf());

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (WebServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testTimeout() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test");

        webServiceProxy.setArguments(mapOf(
            entry("value", 123),
            entry("delay", 6000)
        ));

        webServiceProxy.setConnectTimeout(500);
        webServiceProxy.setReadTimeout(4000);

        webServiceProxy.setMonitorStream(System.out);

        try {
            webServiceProxy.invoke();

            fail();
        } catch (IOException exception) {
            assertTrue(exception instanceof SocketTimeoutException);
        }
    }

    @Test
    public void testCustomException() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", new URL(baseURL, "test/error"));

        webServiceProxy.setErrorHandler((errorStream, contentType, statusCode) -> {
            var textDecoder = new TextDecoder();

            throw new CustomException(textDecoder.read(errorStream));
        });

        webServiceProxy.setMonitorStream(System.out);

        assertThrows(CustomException.class, webServiceProxy::invoke);
    }

    @Test
    public void testMathDelegation1() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test/math/sum");

        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(6.0, result);
    }

    @Test
    public void testMathDelegation2() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "test/math/sum");

        webServiceProxy.setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        ));

        webServiceProxy.setMonitorStream(System.out);

        var result = webServiceProxy.invoke();

        assertEquals(6.0, result);
    }

    @Test
    public void testMathProxy() throws IOException {
        var mathServiceProxy = WebServiceProxy.of(MathServiceProxy.class, new URL(baseURL, "math/"));

        assertEquals(6.0, mathServiceProxy.getSum(4, 2));
        assertEquals(6.0, mathServiceProxy.getSum(listOf(1.0, 2.0, 3.0)));
    }

    @Test
    public void testFileUpload1() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURL, "upload");

        webServiceProxy.setArguments(mapOf(
            entry("file", WebServiceProxyTest.class.getResource("test.txt"))
        ));

        var result = webServiceProxy.invoke();

        assertEquals(26, result);
    }

    @Test
    public void testFileUpload2() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURL, "upload");

        webServiceProxy.setArguments(mapOf(
            entry("files", listOf(
                WebServiceProxyTest.class.getResource("test.txt"),
                WebServiceProxyTest.class.getResource("test.jpg")
            ))
        ));

        var result = webServiceProxy.invoke();

        assertEquals(10418, result);
    }

    @Test
    public void testCatalog() throws IOException {
        var item = addItem();

        assertNotNull(item);
        assertNotNull(item.getID());
        assertEquals("abc", item.getDescription());
        assertEquals(150.00, item.getPrice());

        assertNotNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        updateItem(item);

        assertNotNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        deleteItem(item);

        assertNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        assertEquals(Arrays.asList(Size.values()), getCatalogSizes());
    }

    private Item addItem() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURL, "catalog/items");

        webServiceProxy.setBody(mapOf(
            entry("description", "abc"),
            entry("price", 150.00)
        ));

        webServiceProxy.setExpectedStatus(WebServiceProxy.Status.CREATED);

        return webServiceProxy.invoke(result -> BeanAdapter.coerce(result, Item.class));
    }

    private void updateItem(Item item) throws IOException {
        item.setDescription("xyz");
        item.setPrice(300.00);

        var webServiceProxy = new WebServiceProxy("PUT", baseURL, "catalog/items/%s", item.getID());

        webServiceProxy.setBody(item);

        webServiceProxy.invoke();
    }

    private void deleteItem(Item item) throws IOException {
        var webServiceProxy = new WebServiceProxy("DELETE", baseURL, "catalog/items/%s", item.getID());

        webServiceProxy.invoke();
    }

    private List<Item> getCatalogItems() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "catalog/items");

        return webServiceProxy.invoke(result -> BeanAdapter.coerceList((List<?>)result, Item.class));
    }

    private List<Size> getCatalogSizes() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURL, "catalog/sizes");

        return webServiceProxy.invoke(result -> BeanAdapter.coerceList((List<?>)result, Size.class));
    }

    @Test
    public void testCatalogProxy() throws IOException {
        var catalogServiceProxy = WebServiceProxy.of(CatalogServiceProxy.class, new URL(baseURL, "catalog/"));

        var item = BeanAdapter.coerce(mapOf(), Item.class);

        item.setDescription("abc");
        item.setPrice(150.0);

        item = catalogServiceProxy.addItem(item);

        assertNotNull(item);
        assertNotNull(item.getID());
        assertEquals("abc", item.getDescription());
        assertEquals(150.00, item.getPrice());

        assertNotNull(catalogServiceProxy.getItems().stream().filter(item::equals).findAny().orElse(null));

        item.setDescription("xyz");
        item.setPrice(300.00);

        catalogServiceProxy.updateItem(item.getID(), item);

        assertNotNull(catalogServiceProxy.getItems().stream().filter(item::equals).findAny().orElse(null));

        catalogServiceProxy.deleteItem(item.getID());

        assertNull(catalogServiceProxy.getItems().stream().filter(item::equals).findAny().orElse(null));

        assertEquals(Arrays.asList(Size.values()), catalogServiceProxy.getSizes());
    }

    @Test
    public void testPets() throws IOException {
        testPetsJSON(false);
        testPetsJSON(true);
        testPetsCSV();
        testPetsHTML();
        testPetsXML();

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets/average-age");

        var averageAge = webServiceProxy.invoke();

        assertNotNull(averageAge);
    }

    private void testPetsJSON(boolean stream) throws IOException {
        List<?> expected;
        try (var inputStream = getClass().getResourceAsStream("pets.json")) {
            var jsonDecoder = new JSONDecoder();

            expected = (List<?>)jsonDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets");

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", stream ? "application/json" : "*/*")
        ));

        webServiceProxy.setArguments(mapOf(
            entry("owner", "Gwen"),
            entry("stream", stream)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke();

        assertEquals(expected, actual);
    }

    private void testPetsCSV() throws IOException {
        List<?> expected;
        try (var inputStream = getClass().getResourceAsStream("pets.csv")) {
            var csvDecoder = new CSVDecoder();

            expected = csvDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets");

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", "text/csv")
        ));

        webServiceProxy.setArguments(mapOf(
            entry("owner", "Gwen"),
            entry("stream", true)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke((inputStream, contentType) -> {
            var csvDecoder = new CSVDecoder();

            return csvDecoder.read(inputStream);
        });

        assertEquals(expected, actual);

    }

    private void testPetsHTML() throws IOException {
        testPetsMarkup("pets.html", "text/html");
    }

    private void testPetsXML() throws IOException {
        testPetsMarkup("pets.xml", "text/xml");
    }

    private void testPetsMarkup(String name, String mimeType) throws IOException {
        String expected;
        try (var inputStream = getClass().getResourceAsStream(name)) {
            var textDecoder = new TextDecoder();

            expected = textDecoder.read(inputStream);
        }

        var webServiceProxy = new WebServiceProxy("GET", baseURL, "pets");

        webServiceProxy.setHeaders(mapOf(
            entry("Accept", mimeType)
        ));

        webServiceProxy.setArguments(mapOf(
            entry("owner", "Gwen"),
            entry("stream", true)
        ));

        webServiceProxy.setMonitorStream(System.out);

        var actual = webServiceProxy.invoke((inputStream, contentType) -> {
            var textDecoder = new TextDecoder();

            return textDecoder.read(inputStream);
        });

        assertEquals(expected, actual);
    }

    @Test
    public void testAPIDocumentation() throws IOException {
        testAPIDocumentation("math");
        testAPIDocumentation("upload");
        testAPIDocumentation("catalog");
    }

    private void testAPIDocumentation(String name) throws IOException {
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