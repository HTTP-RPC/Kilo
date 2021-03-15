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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }

    private URL serverURL;

    private DayOfWeek dayOfWeek = DayOfWeek.MONDAY;

    private Date date = new Date();
    private Instant instant = Instant.ofEpochMilli(1);

    private LocalDate localDate = LocalDate.now();
    private LocalTime localTime = LocalTime.now();
    private LocalDateTime localDateTime = LocalDateTime.now();

    private static final int EOF = -1;

    public WebServiceProxyTest() throws IOException {
        serverURL = new URL("http://localhost:8080/httprpc-test-1.0/");
    }

    @Test
    public void testGet() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test"));

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
            entry("localDateTime", localDateTime)
        ));

        Map<String, ?> result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123L),
            entry("flag", true),
            entry("dayOfWeek", dayOfWeek.toString()),
            entry("date", date.getTime()),
            entry("instant", instant.toString()),
            entry("localDate", localDate.toString()),
            entry("localTime", localTime.toString()),
            entry("localDateTime", localDateTime.toString())
        ), result);
    }

    @Test
    public void testGetKeys() throws IOException {
        String path = String.format("test/a/%d/b/%s/c/%d/d/%s",
            123,
            URLEncoder.encode("héllo", "UTF-8"),
            456,
            URLEncoder.encode("göodbye", "UTF-8")
        );

        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, path));

        Map<String, ?> result = webServiceProxy.invoke();

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
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test/fibonacci"));

        webServiceProxy.setArguments(mapOf(
            entry("count", 8)
        ));

        List<Integer> result = BeanAdapter.adaptList(webServiceProxy.invoke(), Integer.class);

        assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13), result);
    }

    @Test
    public void testURLEncodedPost() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(serverURL, "test"));

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
            entry("localDateTime", localDateTime)
        ));

        Map<String, ?> result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123L),
            entry("flag", true),
            entry("dayOfWeek", dayOfWeek.toString()),
            entry("date", date.getTime()),
            entry("instant", instant.toString()),
            entry("localDate", localDate.toString()),
            entry("localTime", localTime.toString()),
            entry("localDateTime", localDateTime.toString()),
            entry("attachmentInfo", listOf())
        ), result);
    }

    @Test
    public void testMultipartPost() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(serverURL, "test"));

        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

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
            entry("attachments", listOf(textTestURL, imageTestURL))
        ));

        Response response = BeanAdapter.adapt(webServiceProxy.invoke(), Response.class);

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
            && response.getAttachmentInfo().get(0).getBytes() == 26
            && response.getAttachmentInfo().get(0).getChecksum() == 2412
            && response.getAttachmentInfo().get(1).getBytes() == 10392
            && response.getAttachmentInfo().get(1).getChecksum() == 1038036);
    }

    @Test
    public void testCustomBodyPost() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(serverURL, "test"));

        webServiceProxy.setArguments(mapOf(
            entry("id", 101)
        ));

        Body body = BeanAdapter.adapt(mapOf(
            entry("string", "héllo&gøod+bye?"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123L),
            entry("flag", true)
        ), Body.class);

        webServiceProxy.setBody(body);

        Body result = BeanAdapter.adapt(webServiceProxy.invoke(), Body.class);

        assertEquals(body, result);
    }

    @Test
    public void testCustomImagePost() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(serverURL, "test"));

        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
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
        });

        webServiceProxy.setArguments(mapOf(
            entry("name", imageTestURL.getFile())
        ));

        BufferedImage image = webServiceProxy.invoke((inputStream, contentType) -> ImageIO.read(inputStream));

        assertNotNull(image);
    }

    @Test
    public void testPut() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("PUT", new URL(serverURL, "test"));

        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
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
        });

        webServiceProxy.setArguments(mapOf(
            entry("id", 101)
        ));

        String text = webServiceProxy.invoke((inputStream, contentType) -> {
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
        WebServiceProxy webServiceProxy = new WebServiceProxy("DELETE", new URL(serverURL, "test"));

        webServiceProxy.setArguments(mapOf(
            entry("id", 101)
        ));

        webServiceProxy.invoke();

        assertTrue(true);
    }

    @Test
    public void testHeaders() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test/headers"));

        webServiceProxy.setHeaders(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", 123)
        ));

        Map<String, ?> result = webServiceProxy.invoke();

        assertEquals(mapOf(
            entry("X-Header-A", "abc"),
            entry("X-Header-B", "123")
        ), result);
    }

    @Test
    public void testUnauthorized() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test/unauthorized"));

        int statusCode;
        try {
            webServiceProxy.invoke();

            statusCode = HttpURLConnection.HTTP_OK;
        } catch (WebServiceException exception) {
            statusCode = exception.getStatusCode();
        }

        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, statusCode);
    }

    @Test
    public void testError() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test/error"));

        boolean error;
        try {
            webServiceProxy.invoke();

            error = false;
        } catch (WebServiceException exception) {
            error = true;
        }

        assertTrue(error);
    }

    @Test
    public void testTimeout() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test"));

        webServiceProxy.setConnectTimeout(500);
        webServiceProxy.setReadTimeout(4000);

        webServiceProxy.setArguments(mapOf(
            entry("value", 123),
            entry("delay", 6000)
        ));

        boolean timeout;
        try {
            webServiceProxy.invoke();

            timeout = false;
        } catch (IOException exception) {
            timeout = (exception instanceof SocketTimeoutException);
        }

        assertTrue(timeout);
    }

    @Test
    public void testMath() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        assertEquals(6.0, BeanAdapter.adapt(webServiceProxy.invoke(), Double.class));

        webServiceProxy.setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        ));

        assertEquals(6.0, BeanAdapter.adapt(webServiceProxy.invoke(), Double.class));
    }

    @Test
    public void testTree() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "tree"));

        TreeNode seasons = BeanAdapter.adapt(webServiceProxy.invoke(), TreeNode.class);

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
        Item item = addCatalogItem("abc", 150.00);

        assertNotNull(item);
        assertNotNull(item.getID());
        assertEquals("abc", item.getDescription());
        assertEquals(150.00, item.getPrice());

        assertNotNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        item.setDescription("xyz");
        item.setPrice(300.00);

        updateCatalogItem(item);

        assertNotNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        deleteCatalogItem(item.getID());

        assertNull(getCatalogItems().stream().filter(item::equals).findAny().orElse(null));

        assertEquals(Arrays.asList(Size.values()), getCatalogSizes());
    }

    private List<Item> getCatalogItems() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "catalog/items"));

        return BeanAdapter.adaptList(webServiceProxy.invoke(), Item.class);
    }

    private Item addCatalogItem(String description, double price) throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(serverURL, "catalog/items"));

        webServiceProxy.setBody(mapOf(
            entry("description", description),
            entry("price", price)
        ));

        return BeanAdapter.adapt(webServiceProxy.invoke(), Item.class);
    }

    private void updateCatalogItem(Item item) throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("PUT", new URL(serverURL, String.format("catalog/items/%s", item.getID())));

        webServiceProxy.setBody(item);

        webServiceProxy.invoke();
    }

    private void deleteCatalogItem(int itemID) throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("DELETE", new URL(serverURL, String.format("catalog/items/%s", itemID)));

        webServiceProxy.invoke();
    }

    private List<Size> getCatalogSizes() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "catalog/sizes"));

        return BeanAdapter.adaptList(webServiceProxy.invoke(), Size.class);
    }

    @Test
    public void testCustomException() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test/error"));

        webServiceProxy.setErrorHandler((errorStream, contentType, statusCode) -> {
            throw new CustomException();
        });

        assertThrows(CustomException.class, webServiceProxy::invoke);
    }
}