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

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.httprpc.beans.BeanAdapter;

public class WebServiceProxyTest extends AbstractTest {
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

    private static Date date = new Date();

    private static LocalDate localDate = LocalDate.now();
    private static LocalTime localTime = LocalTime.now();
    private static LocalDateTime localDateTime = LocalDateTime.now();

    public static void main(String[] args) throws Exception {
        testGet();
        testURLEncodedPost();
        testMultipartPost();
        testCustomPost();
        testPut();
        testPatch();
        testDelete();
        testTimeout();
    }

    public static void testGet() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-server/test"));

        webServiceProxy.getArguments().putAll(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime))
        );

        Map<String, ?> result = webServiceProxy.invoke();

        validate("GET", result.get("string").equals("héllo+gøodbye")
            && result.get("strings").equals(listOf("a", "b", "c"))
            && result.get("number").equals(123)
            && result.get("flag").equals(true)
            && result.get("date").equals(date.getTime())
            && result.get("localDate").equals(localDate.toString())
            && result.get("localTime").equals(localTime.toString())
            && result.get("localDateTime").equals(localDateTime.toString()));
    }

    public static void testURLEncodedPost() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL("http://localhost:8080/httprpc-server/test"));

        webServiceProxy.getArguments().putAll(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime))
        );

        Map<String, ?> result = webServiceProxy.invoke();

        validate("POST (URL-encoded)", result.get("string").equals("héllo+gøodbye")
            && result.get("strings").equals(listOf("a", "b", "c"))
            && result.get("number").equals(123)
            && result.get("flag").equals(true)
            && result.get("date").equals(date.getTime())
            && result.get("localDate").equals(localDate.toString())
            && result.get("localTime").equals(localTime.toString())
            && result.get("localDateTime").equals(localDateTime.toString())
            && result.get("attachmentInfo").equals(listOf()));
    }

    public static void testMultipartPost() throws Exception {
        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL("http://localhost:8080/httprpc-server/test"));

        webServiceProxy.setEncoding(WebServiceProxy.Encoding.MULTIPART_FORM_DATA);

        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        webServiceProxy.getArguments().putAll(mapOf(
            entry("string", "héllo+gøodbye"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("attachments", listOf(textTestURL, imageTestURL)))
        );

        Response response = BeanAdapter.adapt(webServiceProxy.invoke(), Response.class);

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
        // TODO
    }

    public static void testPut() throws Exception {
        // TODO
    }

    public static void testPatch() throws Exception {
        // TODO
    }

    public static void testDelete() throws Exception {
        // TODO
    }

    public static void testTimeout() throws Exception {
        // TODO
    }

    private static void validate(String test, boolean condition) {
        System.out.println(test + ": " + (condition ? "OK" : "FAIL"));
    }
}
