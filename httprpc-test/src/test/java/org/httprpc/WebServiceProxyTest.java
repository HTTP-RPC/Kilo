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

package org.httprpc;

import org.httprpc.beans.BeanAdapter;
import org.httprpc.beans.Key;
import org.httprpc.io.TextDecoder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
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

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class WebServiceProxyTest {
    public interface Response {
        String getString();
        List<String> getStrings();
        int getNumber();
        boolean getFlag();
        DayOfWeek getDayOfWeek();
        Date getDate();
        Instant getInstant();
        LocalDate getLocalDate();
        LocalTime getLocalTime();
        LocalDateTime getLocalDateTime();
        Duration getDuration();
        Period getPeriod();
        @Key("uuid")
        UUID getUUID();
        List<AttachmentInfo> getAttachmentInfo();
    }

    public interface AttachmentInfo {
        int getBytes();
        int getChecksum();
        URL getAttachment();
    }

    public interface Body {
        String getString();
        List<String> getStrings();
        int getNumber();
        boolean getFlag();
    }

    public interface TreeNode {
        String getName();
        List<TreeNode> getChildren();
    }

    public static class Item {
        private Integer id;
        private String description;
        private Double price;

        @Key("id")
        public Integer getID() {
            return id;
        }

        @Key("id")
        public void setID(int id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Item)) {
                return false;
            }

            Item item = (Item)object;

            return id != null && id.equals(item.id)
                && description != null && description.equals(item.description)
                && price != null && price.equals(item.price);
        }
    }

    public enum Size {
        SMALL,
        MEDIUM,
        LARGE
    }

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
        baseURL = new URL("http://localhost:8080/httprpc-test-1.0/");
    }

    @Test
    public void testGet() throws IOException {
        Map<String, ?> result = WebServiceProxy.get(baseURL, "test").setArguments(mapOf(
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
        Map<String, ?> result = WebServiceProxy.get(baseURL, "test/a/%d/b/%s/c/%d/d/%s",
            123,
            URLEncoder.encode("héllo", "UTF-8"),
            456,
            URLEncoder.encode("göodbye", "UTF-8")
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
        List<Integer> result = WebServiceProxy.get(baseURL, "test/fibonacci").setArguments(
            mapOf(
                entry("count", 8)
            )
        ).setMonitorStream(System.out).invoke(BeanAdapter.typeOf(List.class, Integer.class));

        assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13), result);
    }

    @Test
    public void testURLEncodedPost() throws IOException {
        Map<String, ?> result = WebServiceProxy.post(baseURL, "test").setArguments(mapOf(
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
        )).invoke();

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
        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        Response response = WebServiceProxy.post(baseURL, "test").setEncoding(WebServiceProxy.Encoding.MULTIPART_FORM_DATA).setArguments(mapOf(
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
        )).invoke(Response.class);

        assertNotNull(response);

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
    public void testCustomBodyPost() throws IOException {
        Body body = BeanAdapter.coerce(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true)
        ), Body.class);

        Body result = WebServiceProxy.post(baseURL, "test").setArguments(mapOf(
            entry("id", 101)
        )).setBody(body).setMonitorStream(System.out).invoke(Body.class);

        assertEquals(body, result);
    }

    @Test
    public void testCustomImagePost() throws IOException {
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        BufferedImage image = WebServiceProxy.post(baseURL, "test").setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                try (InputStream inputStream = imageTestURL.openStream()) {
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
        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");

        String text = WebServiceProxy.put(baseURL, "test").setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return null;
            }

            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                try (InputStream inputStream = textTestURL.openStream()) {
                    int b;
                    while ((b = inputStream.read()) != EOF) {
                        outputStream.write(b);
                    }
                }
            }
        }).setArguments(mapOf(
            entry("id", 101)
        )).setMonitorStream(System.out).invoke((inputStream, contentType) -> {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

            StringBuilder textBuilder = new StringBuilder();

            int c;
            while ((c = inputStreamReader.read()) != EOF) {
                textBuilder.append((char)c);
            }

            return textBuilder.toString();
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
        Map<String, ?> result = WebServiceProxy.get(baseURL, "test/headers").setHeaders(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", 123)
        )).invoke();

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
    public void testError() throws IOException {
        try {
            WebServiceProxy.get(baseURL, "test/error").setMonitorStream(System.out).invoke();

            fail();
        } catch (WebServiceException exception) {
            assertNotNull(exception.getMessage());
            assertEquals(500, exception.getStatusCode());
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
    public void testMath() throws IOException {
        assertEquals(6.0, WebServiceProxy.get(baseURL, "test/math/sum").setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        )).setMonitorStream(System.out).invoke(Double.class));

        assertEquals(6.0, WebServiceProxy.get(baseURL, "test/math/sum").setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        )).setMonitorStream(System.out).invoke(Double.class));
    }

    @Test
    public void testTree() throws IOException {
        TreeNode seasons = WebServiceProxy.get(baseURL, "tree").setMonitorStream(System.out).invoke(TreeNode.class);

        assertNotNull(seasons);
        assertEquals("Seasons", seasons.getName());

        TreeNode winter = seasons.getChildren().get(0);

        assertNotNull(winter);
        assertEquals("Winter", winter.getName());

        TreeNode january = winter.getChildren().get(0);

        assertNotNull(january);
        assertEquals("January", january.getName());
    }

    @Test
    public void testCatalog() throws IOException {
        Item item = WebServiceProxy.post(baseURL, "catalog/items").setBody(mapOf(
            entry("description", "abc"),
            entry("price", 150.00)
        )).setExpectedStatus(WebServiceProxy.Status.CREATED).invoke(Item.class);

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
        return WebServiceProxy.get(baseURL, "catalog/items").invoke(BeanAdapter.typeOf(List.class, Item.class));
    }

    private List<Size> getCatalogSizes() throws IOException {
        return WebServiceProxy.get(baseURL, "catalog/sizes").invoke(BeanAdapter.typeOf(List.class, Size.class));
    }

    @Test
    public void testCustomException() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(baseURL, "test/error"));

        webServiceProxy.setErrorHandler((errorStream, contentType, statusCode) -> {
            TextDecoder textDecoder = new TextDecoder();

            throw new CustomException(textDecoder.read(errorStream));
        });

        webServiceProxy.setMonitorStream(System.out);

        assertThrows(CustomException.class, webServiceProxy::invoke);
    }
}