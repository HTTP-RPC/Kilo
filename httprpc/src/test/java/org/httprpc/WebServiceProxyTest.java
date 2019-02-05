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

import javax.imageio.ImageIO;

import org.httprpc.beans.BeanAdapter;

public class WebServiceProxyTest extends AbstractTest {
    public interface TestService {
        public interface Response {
            public interface AttachmentInfo {
                public int getBytes();
                public int getChecksum();
            }

            public String getString();
            public List<String> getStrings();
            public int getNumber();
            public boolean getFlag();
            public Date getDate();
            public LocalDate getLocalDate();
            public LocalTime getLocalTime();
            public LocalDateTime getLocalDateTime();
            public List<AttachmentInfo> getAttachmentInfo();
        }

        @RequestMethod("GET")
        public Map<String, Object> testGet(@RequestParameter("string") String text, List<String> strings, int number, boolean flag,
            Date date, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime) throws IOException;

        @RequestMethod("GET")
        @ResourcePath("fibonacci")
        public List<Integer> testGetFibonacci() throws IOException;

        @RequestMethod("POST")
        public Response testPost(String string, List<String> strings, int number, boolean flag,
            Date date, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
            List<URL> attachments) throws IOException;
    }

    public interface MathService {
        @RequestMethod("GET")
        @ResourcePath("sum")
        public double getSum(double a, double b) throws IOException;

        @RequestMethod("GET")
        @ResourcePath("sum")
        public double getSum(List<Double> values) throws IOException;
    }

    public interface TreeNode {
        public String getName();
        public List<TreeNode> getChildren();
    }

    private static Date date = new Date();

    private static LocalDate localDate = LocalDate.now();
    private static LocalTime localTime = LocalTime.now();
    private static LocalDateTime localDateTime = LocalDateTime.now();

    private static final int EOF = -1;

    public static void main(String[] args) throws Exception {
        testGet();
        testGetFibonnaci();
        testURLEncodedPost();
        testMultipartPost();
        testCustomPost();
        testPut();
        testDelete();
        testUnauthorized();
        testError();
        testTimeout();
        testMath();
        testMathService();
        testTree();
    }

    public static void testGet() throws Exception {
        TestService testService = WebServiceProxy.adapt(new URL("http://localhost:8080/httprpc-test/test"), TestService.class);

        Map<String, ?> result = testService.testGet("héllo+gøodbye", listOf("a", "b", "c"), 123, true,
            date, localDate, localTime, localDateTime);

        validate("GET", result.get("string").equals("héllo+gøodbye")
            && result.get("strings").equals(listOf("a", "b", "c"))
            && result.get("number").equals(123L)
            && result.get("flag").equals(true)
            && result.get("date").equals(date.getTime())
            && result.get("localDate").equals(localDate.toString())
            && result.get("localTime").equals(localTime.toString())
            && result.get("localDateTime").equals(localDateTime.toString()));
    }

    public static void testGetFibonnaci() throws Exception {
        TestService testService = WebServiceProxy.adapt(new URL("http://localhost:8080/httprpc-test/test/"), TestService.class);

        List<Integer> fibonacci = testService.testGetFibonacci();

        validate("GET (Fibonacci)", fibonacci.equals(listOf(1, 2, 3, 5, 8, 13)));
    }

    public static void testURLEncodedPost() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL("http://localhost:8080/httprpc-test/test"));

        webServiceProxy.setArguments(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123L),
            entry("flag", true),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime)
        ));

        Map<String, ?> result = webServiceProxy.invoke();

        validate("POST (URL-encoded)", result.get("string").equals("héllo+gøodbye")
            && result.get("strings").equals(listOf("a", "b", "c"))
            && result.get("number").equals(123L)
            && result.get("flag").equals(true)
            && result.get("date").equals(date.getTime())
            && result.get("localDate").equals(localDate.toString())
            && result.get("localTime").equals(localTime.toString())
            && result.get("localDateTime").equals(localDateTime.toString())
            && result.get("attachmentInfo").equals(listOf()));
    }

    public static void testMultipartPost() throws Exception {
        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        TestService testService = WebServiceProxy.adapt(new URL("http://localhost:8080/httprpc-test/test"), TestService.class);

        TestService.Response response = testService.testPost("héllo+gøodbye", listOf("a", "b", "c"), 123, true,
            date, localDate, localTime, localDateTime,
            listOf(textTestURL, imageTestURL));

        validate("POST (multipart)", response.getString().equals("héllo+gøodbye")
            && response.getStrings().equals(listOf("a", "b", "c"))
            && response.getNumber() == 123
            && response.getFlag() == true
            && response.getDate().equals(date)
            && response.getLocalDate().equals(localDate)
            && response.getLocalTime().equals(localTime)
            && response.getLocalDateTime().equals(localDateTime)
            && response.getAttachmentInfo().get(0).getBytes() == 26
            && response.getAttachmentInfo().get(0).getChecksum() == 2412
            && response.getAttachmentInfo().get(1).getBytes() == 10392
            && response.getAttachmentInfo().get(1).getChecksum() == 1038036);
    }

    public static void testCustomPost() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL("http://localhost:8080/httprpc-test/test"));

        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        webServiceProxy.setRequestHandler((outputStream) -> {
            try (InputStream inputStream = imageTestURL.openStream()) {
                int b;
                while ((b = inputStream.read()) != -1) {
                    outputStream.write(b);
                }
            }
        });

        webServiceProxy.setArguments(mapOf(
            entry("name", imageTestURL.getFile())
        ));

        BufferedImage image = webServiceProxy.invoke((inputStream, contentType) -> {
            return ImageIO.read(inputStream);
        });

        validate("POST (custom)", image != null);
    }

    public static void testPut() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("PUT", new URL("http://localhost:8080/httprpc-test/test"));

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

        String text = webServiceProxy.invoke((inputStream, contentType) -> {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

            StringBuilder textBuilder = new StringBuilder();

            int c;
            while ((c = inputStreamReader.read()) != EOF) {
                textBuilder.append((char)c);
            }

            return textBuilder.toString();
        });

        validate("PUT", text != null);
    }

    public static void testDelete() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("DELETE", new URL("http://localhost:8080/httprpc-test/test"));

        webServiceProxy.setArguments(mapOf(
            entry("id", 101)
        ));

        webServiceProxy.invoke();

        validate("DELETE", true);
    }

    public static void testUnauthorized() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/test/unauthorized"));

        int status;
        try {
            webServiceProxy.invoke();

            status = HttpURLConnection.HTTP_OK;
        } catch (WebServiceException exception) {
            status = exception.getStatus();
        }

        validate("Unauthorized", status == HttpURLConnection.HTTP_FORBIDDEN);
    }

    public static void testError() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/test/error"));

        boolean error;
        try {
            webServiceProxy.invoke();

            error = false;
        } catch (WebServiceException exception) {
            error = true;
        }

        validate("Error", error);
    }

    public static void testTimeout() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/test"));

        webServiceProxy.setConnectTimeout(3000);
        webServiceProxy.setReadTimeout(3000);

        webServiceProxy.setArguments(mapOf(
            entry("value", 123),
            entry("delay", 6000)
        ));

        boolean timeout;
        try {
            webServiceProxy.invoke();

            timeout = false;
        } catch (SocketTimeoutException exception) {
            timeout = true;
        }

        validate("Timeout", timeout);
    }

    public static void testMath() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/math/sum"));

        HashMap<String, Integer> arguments = new HashMap<>();

        arguments.put("a", 4);
        arguments.put("b", 2);

        webServiceProxy.setArguments(arguments);

        Number result = webServiceProxy.invoke();

        validate("Math", result.doubleValue() == 6.0);
    }

    public static void testMathService() throws Exception {
        MathService mathService = WebServiceProxy.adapt(new URL("http://localhost:8080/httprpc-test/math/"), MathService.class);

        validate("Math (service)", mathService.getSum(4, 2) == 6.0
            && mathService.getSum(listOf(1.0, 2.0, 3.0)) == 6.0);
    }

    public static void testTree() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/tree"));

        TreeNode root = BeanAdapter.adapt(webServiceProxy.invoke(), TreeNode.class);

        validate("Tree", root.getName().equals("Seasons")
            && root.getChildren().get(0).getName().equals("Winter")
            && root.getChildren().get(0).getChildren().get(0).getName().equals("January"));
    }

    private static void validate(String test, boolean condition) {
        System.out.println(test + ": " + (condition ? "OK" : "FAIL"));
    }
}
