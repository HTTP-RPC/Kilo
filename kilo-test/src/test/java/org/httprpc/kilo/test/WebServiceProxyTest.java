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
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.io.TextEncoder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebServiceProxyTest {
    public static class CustomException extends IOException {
        public CustomException(String message) {
            super(message);
        }
    }

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

    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testGet() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("numbers", listOf(1, 2, 2, 3, 3, 3)),
            entry("flag", true),
            entry("character", "abc"),
            entry("dayOfWeek", dayOfWeek),
            entry("instant", instant),
            entry("date", date),
            entry("dates", listOf(date)),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("duration", duration),
            entry("period", period),
            entry("uuid", uuid)
        ));

        var result = BeanAdapter.coerce(webServiceProxy.invoke(), TestService.Response.class);

        assertEquals(200, webServiceProxy.getStatusCode());

        assertEquals(result.getString(), "héllo&gøod+bye?");
        assertEquals(result.getStrings(), listOf("a", "b", "c"));
        assertEquals(result.getNumber(), 123);
        assertEquals(result.getNumbers(), setOf(1, 2, 3));
        assertTrue(result.getFlag());
        assertEquals(result.getCharacter(), 'a');
        assertEquals(result.getDayOfWeek(), dayOfWeek);
        assertEquals(result.getDate(), date);
        assertEquals(result.getDates(), listOf(date));
        assertEquals(result.getInstant(), instant);
        assertEquals(result.getLocalDate(), localDate);
        assertEquals(result.getLocalTime(), localTime);
        assertEquals(result.getLocalDateTime(), localDateTime);
        assertEquals(result.getDuration(), duration);
        assertEquals(result.getPeriod(), period);
        assertEquals(result.getUUID(), uuid);
    }

    @Test
    public void testGetProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        var result = testServiceProxy.testGet("héllo&gøod+bye?", listOf("a", "b", "c"), 123, setOf(1, 2, 3), 'a');

        assertEquals("héllo&gøod+bye?", result.getString());
        assertEquals(listOf("a", "b", "c"), result.getStrings());
        assertEquals(123, result.getNumber());
        assertEquals(setOf(1, 2, 3), result.getNumbers());
        assertEquals('a', result.getCharacter());
    }

    @Test
    public void testKeys() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/a%23/123/b*/h%2Bllo/c@/456/d=/g%3Fodbye"));

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("a", 123),
            entry("b", "h+llo"),
            entry("c", 456),
            entry("d", "g?odbye")
        ), result);
    }

    @Test
    public void testKeysProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        var result = testServiceProxy.testKeys(123, "h+llo", 456,"g?odbye");

        assertEquals(mapOf(
            entry("a", 123),
            entry("b", "h+llo"),
            entry("c", 456),
            entry("d", "g?odbye")
        ), result);
    }

    @Test
    public void testParameters() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/foo/1/bar/2"));

        webServiceProxy.setArguments(mapOf(
            entry("a", 3),
            entry("b", 4)
        ));

        webServiceProxy.setBody(listOf(5.0, 6.0, 7.0));

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
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

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
    public void testVarargs() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/varargs"));

        webServiceProxy.setArguments(mapOf(
            entry("numbers", listOf(1, 2, 3))
        ));

        webServiceProxy.setBody(listOf("abc", "def", "ghi"));

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("numbers", listOf(1, 2, 3)),
            entry("strings", listOf("abc", "def", "ghi"))
        ), result);
    }

    @Test
    public void testVarargsProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        var result = testServiceProxy.testVarargs(new int[] {1, 2, 3}, "abc", "def", "ghi");

        assertEquals(mapOf(
            entry("numbers", listOf(1, 2, 3)),
            entry("strings", listOf("abc", "def", "ghi"))
        ), result);
    }

    @Test
    public void testGetFibonacci() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/fibonacci"));

        webServiceProxy.setArguments(mapOf(
            entry("count", 8)
        ));

        var result = webServiceProxy.invoke();

        assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13), result);
    }

    @Test
    public void testPost() {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test"));

        var body = listOf("a", "b", "c");

        webServiceProxy.setArguments(mapOf(
            entry("number", 0)
        ));

        webServiceProxy.setBody(body);

        assertThrows(WebServiceException.class, webServiceProxy::invoke);
    }

    @Test
    public void testListPost() throws IOException {
        var body = listOf(1, 2, 3);

        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/list"));

        webServiceProxy.setBody(body);

        var result = webServiceProxy.invoke();

        assertEquals(body, BeanAdapter.coerceList((List<?>)result, Integer.class));
    }

    @Test
    public void testUnsupportedListPost() {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/list"));

        webServiceProxy.setArguments(mapOf(
            entry("list", listOf(1, 2, 3))
        ));

        webServiceProxy.setBody(listOf(4, 5, 6));

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertEquals(405, exception.getStatusCode());
    }

    @Test
    public void testInvalidListPost() {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/list"));

        webServiceProxy.setBody("xyz");

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testMalformedListPost() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/list"));

        webServiceProxy.setBody("xyz");

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return "application/json";
            }

            @Override
            public void encodeRequest(Object body, OutputStream outputStream) throws IOException {
                var textEncoder = new TextEncoder();

                textEncoder.write(body, outputStream);
            }
        });

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testMapPost() throws IOException {
        var body = mapOf(
            entry("a", 1.0),
            entry("b", 2.0),
            entry("c", 3.0)
        );

        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/map"));

        webServiceProxy.setBody(body);

        var result = webServiceProxy.invoke();

        assertEquals(body, result);
    }

    @Test
    public void testBodyPost() throws IOException {
        var body = BeanAdapter.coerce(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("numbers", listOf(1, 2, 2, 3, 3, 3)),
            entry("flag", true)
        ), TestService.Body.class);

        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/body"));

        webServiceProxy.setBody(body);

        var result = BeanAdapter.coerce(webServiceProxy.invoke(), TestService.Body.class);

        assertEquals(201, webServiceProxy.getStatusCode());

        assertEquals("héllo&gøod+bye?", result.getString());
        assertEquals(listOf("a", "b", "c"), result.getStrings());
        assertEquals(123, result.getNumber());
        assertEquals(setOf(1, 2, 3), result.getNumbers());
        assertTrue(result.getFlag());
    }

    @Test
    public void testCoordinatesPost() throws IOException {
        var body = new Coordinates(1, 2);

        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/coordinates"));

        webServiceProxy.setBody(body);

        var result = BeanAdapter.coerce(webServiceProxy.invoke(), Coordinates.class);

        assertEquals(body.x(), result.x());
        assertEquals(body.y(), result.y());
    }

    @Test
    public void testFormDataPost() throws IOException {
        var textURL = getClass().getResource("test.txt");
        var imageURL = getClass().getResource("test.jpg");

        var body = mapOf(
            entry("string", "abc"),
            entry("numbers", listOf(1, 2, 3)),
            entry("date", new Date()),
            entry("file", textURL.getPath()),
            entry("files", listOf(textURL.getPath(), imageURL.getPath()))
        );

        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/form-data"));

        webServiceProxy.setBody(body);

        var result = webServiceProxy.invoke();

        // TODO
    }

    @Test
    public void testFormDataPostProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        var textURL = getClass().getResource("test.txt");
        var imageURL = getClass().getResource("test.jpg");

        var formData = BeanAdapter.coerce(mapOf(
            entry("string", "abc"),
            entry("numbers", listOf(1, 2, 3)),
            entry("date", new Date()),
            entry("file", textURL.getPath()),
            entry("files", listOf(textURL.getPath(), imageURL.getPath()))
        ), TestServiceProxy.FormData.class);

        var result = testServiceProxy.testFormDataPost(formData);

        // TODO
    }

    @Test
    public void testImagePost() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/image"));

        webServiceProxy.setBody(getClass().getResource("test.jpg"));

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public void encodeRequest(Object body, OutputStream outputStream) throws IOException {
                try (var inputStream = ((URL)body).openStream()) {
                    int b;
                    while ((b = inputStream.read()) != EOF) {
                        outputStream.write(b);
                    }
                }
            }
        });

        webServiceProxy.setResponseHandler((inputStream, contentType) -> ImageIO.read(inputStream));

        var result = webServiceProxy.invoke();

        assertNotNull(result);
    }

    @Test
    public void testImagePostProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        var result = testServiceProxy.testImagePost(getClass().getResource("test.jpg"));

        assertNotNull(result);
    }

    @Test
    public void testPut() throws IOException {
        var webServiceProxy = new WebServiceProxy("PUT", baseURI.resolve("test"));

        webServiceProxy.setBody(getClass().getResource("test.txt"));

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public void encodeRequest(Object body, OutputStream outputStream) throws IOException {
                try (var inputStream = ((URL)body).openStream()) {
                    int b;
                    while ((b = inputStream.read()) != EOF) {
                        outputStream.write(b);
                    }
                }
            }
        });

        webServiceProxy.setResponseHandler((inputStream, contentType) -> {
            var textDecoder = new TextDecoder();

            return textDecoder.read(inputStream);
        });

        var result = webServiceProxy.invoke();

        assertNotNull(result);
    }

    @Test
    public void testEmptyPut() throws IOException {
        var webServiceProxy = new WebServiceProxy("PUT", baseURI.resolve("test/101"));

        webServiceProxy.setArguments(mapOf(
            entry("value", "abc")
        ));

        var result = webServiceProxy.invoke();

        assertEquals(101, result);
    }

    @Test
    public void testEmptyPutProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        var result = testServiceProxy.testEmptyPut(101, "abc", null);

        assertEquals(101, result);
    }

    @Test
    public void testDelete() throws IOException {
        var webServiceProxy = new WebServiceProxy("DELETE", baseURI.resolve("test/101"));

        var result = webServiceProxy.invoke();

        assertEquals(101, result);
    }

    @Test
    public void testHeaders() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/headers"));

        webServiceProxy.setHeaders(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", 123)
        ));

        var result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", "123")
        ), result);
    }

    @Test
    public void testHeadersProxy() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI, mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", 123)
        ));

        var result = testServiceProxy.testHeaders();

        assertEquals(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", "123")
        ), result);
    }

    @Test
    public void testException() {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/error"));

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertNotNull(exception.getMessage());
        assertEquals(500, exception.getStatusCode());
    }

    @Test
    public void testInvalidNumberArgument() {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", "abc"),
            entry("number", "x")
        ));

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testInvalidDayOfWeekArgument() {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", "abc"),
            entry("dayOfWeek", "y")
        ));

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testInvalidLocalDateArgument() {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", "abc"),
            entry("localDate", "z")
        ));

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testMissingRequiredParameter() {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", null)
        ));

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertNotNull(exception.getMessage());
        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testMissingRequiredParameterProxy() {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        assertThrows(IllegalArgumentException.class, () -> testServiceProxy.testGet(null, null, null, null, '\0'));
    }

    @Test
    public void testMissingProxyException() {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        assertThrows(UnsupportedOperationException.class, testServiceProxy::testMissingException);
    }

    @Test
    public void testInvalidProxyException() {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        assertThrows(UnsupportedOperationException.class, testServiceProxy::testInvalidException);
    }

    @Test
    public void testMissingRequiredProperty() {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("test/body"));

        webServiceProxy.setBody(mapOf());

        var exception = assertThrows(WebServiceException.class, webServiceProxy::invoke);

        assertNotNull(exception.getMessage());
        assertEquals(403, exception.getStatusCode());
    }

    @Test
    public void testTimeout() {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test"));

        webServiceProxy.setArguments(mapOf(
            entry("value", 123),
            entry("delay", 6000)
        ));

        webServiceProxy.setConnectTimeout(500);
        webServiceProxy.setReadTimeout(4000);

        assertThrows(SocketTimeoutException.class, webServiceProxy::invoke);
    }

    @Test
    public void testTimeoutProxy() {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        assertThrows(SocketTimeoutException.class, () -> testServiceProxy.testTimeout(123, 6000));
    }

    @Test
    public void testCustomErrorHandler() {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/error"));

        webServiceProxy.setErrorHandler((errorStream, contentType, statusCode) -> {
            var textDecoder = new TextDecoder();

            throw new CustomException(textDecoder.read(errorStream));
        });

        assertThrows(CustomException.class, webServiceProxy::invoke);
    }

    @Test
    public void testCustomErrorHandlerProxy() {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        assertThrows(CustomException.class, testServiceProxy::testCustomErrorHandler);
    }

    @Test
    public void testMathDelegation1() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        var result = webServiceProxy.invoke();

        assertEquals(6.0, result);
    }

    @Test
    public void testMathDelegation2() throws IOException {
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("test/math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        ));

        var result = webServiceProxy.invoke();

        assertEquals(6.0, result);
    }

    @Test
    public void testMembersProxy() throws IOException {
        var memberServiceProxy = WebServiceProxy.of(MemberServiceProxy.class, baseURI);

        var result = memberServiceProxy.getMembers("foo", "bar");

        assertFalse(result.isEmpty());

        var first = result.get(0);

        assertEquals("foo", first.getFirstName());
        assertEquals("bar", first.getLastName());
    }

    @Test
    public void testGreeting() throws IOException {
        var contextPath = baseURI.getPath();

        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve(contextPath.substring(0, contextPath.length() - 1)));

        var result = webServiceProxy.invoke();

        assertEquals("Hello, World!", result);
    }

    @Test
    public void testDefaultProxyMethod() throws IOException {
        var testServiceProxy = WebServiceProxy.of(TestServiceProxy.class, baseURI);

        var result = testServiceProxy.getFibonacciSum(8);

        assertEquals(33, result);
    }
}
