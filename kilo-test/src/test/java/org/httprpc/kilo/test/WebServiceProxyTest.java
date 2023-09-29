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
import java.net.HttpURLConnection;
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
        var result = WebServiceProxy.get(baseURL, "test").setArguments(mapOf(
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
        )).setMonitorStream(System.out).invoke();

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
    public void testGetKeys() throws IOException {
        var result = WebServiceProxy.get(baseURL, "test/a/%d/b/%s/c/%d/d/%s",
            123,
            URLEncoder.encode("héllo", StandardCharsets.UTF_8),
            456,
            URLEncoder.encode("göodbye", StandardCharsets.UTF_8)
        ).setMonitorStream(System.out).invoke();

        assertEquals(mapOf(
            entry("list", listOf("123", "héllo", "456", "göodbye")),
            entry("map", mapOf(
                entry("a", "123"),
                entry("b", null),
                entry("c", "456"),
                entry("d", null)
            ))
        ), result);
    }

    @Test
    public void testGetFibonacci() throws IOException {
        var result = WebServiceProxy.get(baseURL, "test/fibonacci").setArguments(
            mapOf(
                entry("count", 8)
            )
        ).setMonitorStream(System.out).invoke();

        assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13), result);
    }

    @Test
    public void testURLEncodedPost() throws IOException {
        var result = WebServiceProxy.post(baseURL, "test").setArguments(mapOf(
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
        )).setMonitorStream(System.out).invoke();

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

        var response = WebServiceProxy.post(baseURL, "test").setEncoding(WebServiceProxy.Encoding.MULTIPART_FORM_DATA).setArguments(mapOf(
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
        )).invoke(result -> BeanAdapter.coerce(result, TestService.Response.class));

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

        var result = WebServiceProxy.post(baseURL, "test").setBody(body).setMonitorStream(System.out).invoke();

        assertEquals(body, BeanAdapter.coerceList((List<?>)result, Integer.class));
    }

    @Test
    public void testInvalidListPost() throws IOException {
        try {
            WebServiceProxy.post(baseURL, "test").setBody("xyz").setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testMalformedListPost() throws IOException {
        try {
            WebServiceProxy.post(baseURL, "test").setRequestHandler(new WebServiceProxy.RequestHandler() {
                @Override
                public String getContentType() {
                    return "application/json";
                }

                @Override
                public void encodeRequest(OutputStream outputStream) throws IOException {
                    var textEncoder = new TextEncoder();

                    textEncoder.write("xyz", outputStream);
                }
            }).setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testCustomBodyPost() throws IOException {
        var requestBody = BeanAdapter.coerce(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true)
        ), TestService.Body.class);

        var responseBody = WebServiceProxy.post(baseURL, "test").setArguments(mapOf(
            entry("id", 101)
        )).setBody(requestBody).setMonitorStream(System.out).invoke(result -> BeanAdapter.coerce(result, TestService.Body.class));

        assertEquals(requestBody, responseBody);
    }

    @Test
    public void testCoordinatesPost() throws IOException {
        var coordinates = listOf(
            mapOf(
                entry("x", 1),
                entry("y", 2)
            ),
            mapOf(
                entry("x", 3),
                entry("y", 4)
            )
        );

        var result = WebServiceProxy.post(baseURL, "test/coordinates")
            .setBody(coordinates)
            .setMonitorStream(System.out)
            .invoke();

        assertEquals(coordinates, result);
    }

    @Test
    public void testImagePost() throws IOException {
        var imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        var image = WebServiceProxy.post(baseURL, "test").setRequestHandler(new WebServiceProxy.RequestHandler() {
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
        }).setArguments(mapOf(
            entry("name", imageTestURL.getFile())
        )).invoke((inputStream, contentType) -> ImageIO.read(inputStream));

        assertNotNull(image);
    }

    @Test
    public void testPut() throws IOException {
        var textTestURL = WebServiceProxyTest.class.getResource("test.txt");

        var text = WebServiceProxy.put(baseURL, "test").setRequestHandler(new WebServiceProxy.RequestHandler() {
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
        }).setArguments(mapOf(
            entry("id", 101)
        )).setMonitorStream(System.out).invoke((inputStream, contentType) -> {
            var textDecoder = new TextDecoder();

            return textDecoder.read(inputStream);
        });

        assertNotNull(text);
    }

    @Test
    public void testDelete() throws IOException {
        WebServiceProxy.delete(baseURL, "test").setArguments(mapOf(
            entry("id", 101)
        )).setMonitorStream(System.out).invoke();

        assertTrue(true);
    }

    @Test
    public void testHeaders() throws IOException {
        var result = WebServiceProxy.get(baseURL, "test/headers").setHeaders(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", 123)
        )).setMonitorStream(System.out).invoke();

        assertEquals(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", "123")
        ), result);
    }

    @Test
    public void testUnauthorized() throws IOException {
        try {
            WebServiceProxy.get(baseURL, "test/unauthorized").setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(HttpURLConnection.HTTP_FORBIDDEN, exception.getStatusCode());
        }
    }

    @Test
    public void testException() throws IOException {
        try {
            WebServiceProxy.get(baseURL, "test/error").setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(500, exception.getStatusCode());
        }
    }

    @Test
    public void testInvalidNumberArgument() throws IOException {
        try {
            WebServiceProxy.get(baseURL, "test").setArguments(mapOf(
                entry("string", "abc"),
                entry("number", "x")
            )).setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testInvalidDayOfWeekArgument() throws IOException {
        try {
            WebServiceProxy.get(baseURL, "test").setArguments(mapOf(
                entry("string", "abc"),
                entry("dayOfWeek", "y")
            )).setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testInvalidLocalDateArgument() throws IOException {
        try {
            WebServiceProxy.get(baseURL, "test").setArguments(mapOf(
                entry("string", "abc"),
                entry("localDate", "z")
            )).setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testMissingRequiredParameter() throws IOException {
        try {
            WebServiceProxy.get(baseURL, "test").setArguments(mapOf(
                entry("number", 5)
            )).setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testMissingRequiredProperty() throws IOException {
        try {
            WebServiceProxy.post(baseURL, "test").setArguments(mapOf(
                entry("id", 101)
            )).setBody(mapOf()).setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(403, exception.getStatusCode());
        }
    }

    @Test
    public void testTimeout() {
        try {
            WebServiceProxy.get(baseURL, "test").setArguments(mapOf(
                entry("value", 123),
                entry("delay", 6000)
            )).setConnectTimeout(500).setReadTimeout(4000).setMonitorStream(System.out).invoke();

            fail();
        } catch (IOException exception) {
            assertTrue(exception instanceof SocketTimeoutException);
        }
    }

    @Test
    public void testCustomException() throws IOException {
        var webServiceProxy = WebServiceProxy.get(new URL(baseURL, "test/error"));

        webServiceProxy.setErrorHandler((errorStream, contentType, statusCode) -> {
            var textDecoder = new TextDecoder();

            throw new CustomException(textDecoder.read(errorStream));
        });

        webServiceProxy.setMonitorStream(System.out);

        assertThrows(CustomException.class, webServiceProxy::invoke);
    }

    @Test
    public void testMathDelegation() throws IOException {
        assertEquals(6.0, WebServiceProxy.get(baseURL, "test/math/sum").setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        )).setMonitorStream(System.out).invoke());

        assertEquals(6.0, WebServiceProxy.get(baseURL, "test/math/sum").setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        )).setMonitorStream(System.out).invoke());
    }

    @Test
    public void testMathPost() throws IOException {
        assertEquals(6.0, WebServiceProxy.post(baseURL, "math/sum")
            .setBody(listOf(1, 2, 3))
            .setMonitorStream(System.out).invoke());
    }

    @Test
    public void testFileUpload() throws IOException {
        var textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        var imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        assertEquals(26, WebServiceProxy.post(baseURL, "upload").setArguments(mapOf(
            entry("file", textTestURL)
        )).invoke());

        assertEquals(10418, WebServiceProxy.post(baseURL, "upload").setArguments(mapOf(
            entry("files", listOf(textTestURL, imageTestURL))
        )).invoke());
    }

    @Test
    public void testCatalog() throws IOException {
        var item = WebServiceProxy.post(baseURL, "catalog/items").setBody(mapOf(
            entry("description", "abc"),
            entry("price", 150.00)
        )).setExpectedStatus(WebServiceProxy.Status.CREATED).invoke(result -> BeanAdapter.coerce(result, Item.class));

        assertNotNull(item);
        assertNotNull(item.getID());
        assertEquals("abc", item.getDescription());
        assertEquals(150.00, item.getPrice());

        assertNotNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        item.setDescription("xyz");
        item.setPrice(300.00);

        WebServiceProxy.put(baseURL, "catalog/items/%s", item.getID()).setBody(item).invoke();

        assertNotNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        WebServiceProxy.delete(baseURL, "catalog/items/%s", item.getID()).invoke();

        assertNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        assertEquals(Arrays.asList(Size.values()), getCatalogSizes());
    }

    private List<Item> getCatalogItems() throws IOException {
        return WebServiceProxy.get(baseURL, "catalog/items").invoke(result -> BeanAdapter.coerceList((List<?>)result, Item.class));
    }

    private List<Size> getCatalogSizes() throws IOException {
        return WebServiceProxy.get(baseURL, "catalog/sizes").invoke(result -> BeanAdapter.coerceList((List<?>)result, Size.class));
    }

    @Test
    public void testPets() throws IOException {
        testPetsJSON(false);
        testPetsJSON(true);
        testPetsCSV();
        testPetsHTML();

        var averageAge = WebServiceProxy.get(baseURL, "pets/average-age").invoke();

        assertNotNull(averageAge);
    }

    private void testPetsJSON(boolean stream) throws IOException {
        List<?> expected;
        try (var inputStream = getClass().getResourceAsStream("pets.json")) {
            var jsonDecoder = new JSONDecoder();

            expected = (List<?>)jsonDecoder.read(inputStream);
        }

        var actual = WebServiceProxy.get(baseURL, "pets").setHeaders(mapOf(
            entry("Accept", stream ? "application/json" : "*/*")
        )).setArguments(mapOf(
            entry("owner", "Gwen"),
            entry("stream", stream)
        )).invoke();

        assertEquals(expected, actual);
    }

    private void testPetsCSV() throws IOException {
        List<?> expected;
        try (var inputStream = getClass().getResourceAsStream("pets.csv")) {
            var csvDecoder = new CSVDecoder();

            expected = csvDecoder.read(inputStream);
        }

        var actual = WebServiceProxy.get(baseURL, "pets").setHeaders(mapOf(
            entry("Accept", "text/csv")
        )).setArguments(mapOf(
            entry("owner", "Gwen"),
            entry("stream", true)
        )).invoke((inputStream, contentType) -> {
            var csvDecoder = new CSVDecoder();

            return csvDecoder.read(inputStream);
        });

        assertEquals(expected, actual);

    }

    private void testPetsHTML() throws IOException {
        String expected;
        try (var inputStream = getClass().getResourceAsStream("pets.html")) {
            var textDecoder = new TextDecoder();

            expected = textDecoder.read(inputStream);
        }

        var actual = WebServiceProxy.get(baseURL, "pets").setHeaders(mapOf(
            entry("Accept", "text/html")
        )).setArguments(mapOf(
            entry("owner", "Gwen"),
            entry("stream", true)
        )).invoke((inputStream, contentType) -> {
            var textDecoder = new TextDecoder();

            return textDecoder.read(inputStream);
        });

        assertEquals(expected, actual);
    }

    @Test
    public void testAPIDocumentation() throws IOException {
        Map<?, ?> expected;
        try (var inputStream = getClass().getResourceAsStream("math.json")) {
            var jsonDecoder = new JSONDecoder();

            expected = (Map<?, ?>)jsonDecoder.read(inputStream);
        }

        var actual = WebServiceProxy.get(baseURL, "math").setHeaders(
            mapOf(
                entry("Accept", "application/json")
            )
        ).setArguments(mapOf(
            entry("api", "json")
        )).setMonitorStream(System.out).invoke();

        assertEquals(expected, actual);
    }
}