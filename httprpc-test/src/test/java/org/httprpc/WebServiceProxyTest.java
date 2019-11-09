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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.httprpc.util.Collections.*;

public class WebServiceProxyTest {
    public interface TestService {
        interface Response {
            interface AttachmentInfo {
                int getBytes();
                int getChecksum();
            }

            String getString();
            List<String> getStrings();
            int getNumber();
            boolean getFlag();
            Date getDate();
            LocalDate getLocalDate();
            LocalTime getLocalTime();
            LocalDateTime getLocalDateTime();
            List<AttachmentInfo> getAttachmentInfo();
        }

        @RequestMethod("GET")
        @ResourcePath("fibonacci")
        List<Integer> testGetFibonacci(int count) throws IOException;

        @RequestMethod("POST")
        Response testMultipartPost(String string, List<String> strings, int number, boolean flag,
            Date date, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
            List<URL> attachments) throws IOException;
    }

    public interface MathService {
        @RequestMethod("GET")
        @ResourcePath("sum")
        double getSum(double a, double b) throws IOException;

        @RequestMethod("GET")
        @ResourcePath("sum")
        double getSum(List<Double> values) throws IOException;
    }

    public interface TreeNode {
        String getName();
        List<TreeNode> getChildren();
    }

    private Date date = new Date();

    private LocalDate localDate = LocalDate.now();
    private LocalTime localTime = LocalTime.now();
    private LocalDateTime localDateTime = LocalDateTime.now();

    private URL serverURL;

    private static final int EOF = -1;

    public static class AttachmentInfo {
        private int bytes = 0;
        private int checksum = 0;

        public int getBytes() {
            return bytes;
        }

        public int getChecksum() {
            return checksum;
        }
    }

    public WebServiceProxyTest() throws IOException {
        serverURL = new URL("http://localhost:8080/httprpc-test-1.0/");
    }

    @Test
    public void testGet() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime)
        ));

        Map<String, ?> result = webServiceProxy.invoke();

        Assertions.assertEquals(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123L),
            entry("flag", true),
            entry("date", date.getTime()),
            entry("localDate", localDate.toString()),
            entry("localTime", localTime.toString()),
            entry("localDateTime", localDateTime.toString())
        ), result);
    }

    @Test
    public void testGetFibonacci() throws IOException {
        TestService testService = WebServiceProxy.adapt(new URL(serverURL, "test/"), TestService.class);

        List<Integer> result  = testService.testGetFibonacci(8);

        Assertions.assertEquals(listOf(0, 1, 1, 2, 3, 5, 8, 13), result);
    }

    @Test
    public void testURLEncodedPost() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(serverURL, "test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime)
        ));

        Map<String, ?> result = webServiceProxy.invoke();

        Assertions.assertEquals(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123L),
            entry("flag", true),
            entry("date", date.getTime()),
            entry("localDate", localDate.toString()),
            entry("localTime", localTime.toString()),
            entry("localDateTime", localDateTime.toString()),
            entry("attachmentInfo", listOf())
        ), result);
    }

    @Test
    public void testMultipartPost() throws IOException {
        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        TestService testService = WebServiceProxy.adapt(new URL(serverURL, "test/"), TestService.class);

        TestService.Response response = testService.testMultipartPost("héllo+gøodbye", listOf("a", "b", "c"), 123, true,
            date, localDate, localTime, localDateTime,
            listOf(textTestURL, imageTestURL));

        Assertions.assertNotNull(response);

        Assertions.assertTrue(response.getString().equals("héllo+gøodbye")
            && response.getStrings().equals(listOf("a", "b", "c"))
            && response.getNumber() == 123
            && response.getFlag()
            && response.getDate().equals(date)
            && response.getLocalDate().equals(localDate)
            && response.getLocalTime().equals(localTime)
            && response.getLocalDateTime().equals(localDateTime)
            && response.getAttachmentInfo().get(0).getBytes() == 26
            && response.getAttachmentInfo().get(0).getChecksum() == 2412
            && response.getAttachmentInfo().get(1).getBytes() == 10392
            && response.getAttachmentInfo().get(1).getChecksum() == 1038036);
    }

    @Test
    public void testCustomPost() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(serverURL, "test"));

        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        webServiceProxy.setRequestHandler((outputStream) -> {
            try (InputStream inputStream = imageTestURL.openStream()) {
                int b;
                while ((b = inputStream.read()) != EOF) {
                    outputStream.write(b);
                }
            }
        });

        webServiceProxy.setArguments(mapOf(
            entry("name", imageTestURL.getFile())
        ));

        BufferedImage image = webServiceProxy.invoke((inputStream, contentType, headers) -> ImageIO.read(inputStream));

        Assertions.assertNotNull(image);
    }

    @Test
    public void testPut() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("PUT", new URL(serverURL, "test"));

        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");

        webServiceProxy.setRequestHandler((outputStream) -> {
            try (InputStream inputStream = textTestURL.openStream()) {
                int b;
                while ((b = inputStream.read()) != EOF) {
                    outputStream.write(b);
                }
            }
        });

        webServiceProxy.setArguments(mapOf(
            entry("id", 101)
        ));

        String text = webServiceProxy.invoke((inputStream, contentType, headers) -> {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

            StringBuilder textBuilder = new StringBuilder();

            int c;
            while ((c = inputStreamReader.read()) != EOF) {
                textBuilder.append((char)c);
            }

            return textBuilder.toString();
        });

        Assertions.assertNotNull(text);
    }

    @Test
    public void testDelete() throws IOException {
        WebServiceProxy webServiceProxy = new WebServiceProxy("DELETE", new URL(serverURL, "test"));

        webServiceProxy.setArguments(mapOf(
            entry("id", 101)
        ));

        webServiceProxy.invoke();

        Assertions.assertTrue(true);
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

        Assertions.assertEquals(HttpURLConnection.HTTP_FORBIDDEN, statusCode);
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

        Assertions.assertTrue(error);
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

        Assertions.assertTrue(timeout);
    }

    @Test
    public void testMath() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        Number result = webServiceProxy.invoke();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(6.0, result.doubleValue());
    }

    @Test
    public void testMathService() throws Exception {
        MathService mathService = WebServiceProxy.adapt(new URL(serverURL, "math/"), MathService.class);

        Assertions.assertEquals(6.0, mathService.getSum(4, 2));
        Assertions.assertEquals(6.0, mathService.getSum(listOf(1.0, 2.0, 3.0)));
    }

    @Test
    public void testTree() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "tree/"));

        TreeNode seasons = BeanAdapter.adapt(webServiceProxy.invoke(), TreeNode.class);

        Assertions.assertNotNull(seasons);
        Assertions.assertEquals("Seasons", seasons.getName());

        TreeNode winter = seasons.getChildren().get(0);

        Assertions.assertNotNull(winter);
        Assertions.assertEquals("Winter", winter.getName());

        TreeNode january = winter.getChildren().get(0);

        Assertions.assertNotNull(january);
        Assertions.assertEquals("January", january.getName());
    }
}