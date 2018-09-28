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

package org.httprpc.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.httprpc.WebService;
import org.httprpc.io.JSONDecoder;
import org.httprpc.RequestMethod;
import org.httprpc.RequestParameter;
import org.httprpc.ResourcePath;

/**
 * Test service.
 */
@WebServlet(urlPatterns={"/test/*"}, loadOnStartup=1)
@MultipartConfig
public class TestService extends WebService {
    private static final long serialVersionUID = 0;

    @RequestMethod("GET")
    public Map<String, ?> testGet(@RequestParameter("string") String text, List<String> strings, int number, boolean flag,
        Date date, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime) {
        HashMap<String, Object> result = new HashMap<>();

        result.put("string", text);
        result.put("strings", strings);
        result.put("number", number);
        result.put("flag", flag);
        result.put("date", date);
        result.put("localDate", localDate);
        result.put("localTime", localTime);
        result.put("localDateTime", localDateTime);

        return result;
    }

    @RequestMethod("GET")
    @ResourcePath("a/?:a/b/?:b/c/?:c/d/?:d")
    public Map<String, ?> testGet() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("list", Arrays.asList(getKey(0), getKey(1), getKey(2), getKey(3)));

        HashMap<String, String> map = new HashMap<>();

        map.put("a", getKey("a"));
        map.put("b", getKey("b"));
        map.put("c", getKey("c"));
        map.put("d", getKey("d"));

        result.put("map", map);

        return result;
    }

    @RequestMethod("GET")
    @ResourcePath("fibonacci")
    public List<?> testGetFibonacci() throws IOException {
        JSONDecoder jsonDecoder = new JSONDecoder();

        return jsonDecoder.readValue(new StringReader("[1, 2, 3, 5, 8, 13]"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        writer.append("Unsupported operation.");
        writer.flush();
    }

    @RequestMethod("POST")
    public Map<String, ?> testPost(String string, List<String> strings, int number, boolean flag,
        Date date, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
        List<URL> attachments) throws IOException {
        List<Map<String, ?>> attachmentInfo = new LinkedList<>();

        for (URL attachment : attachments) {
            long bytes = 0;
            long checksum = 0;

            try (InputStream inputStream = attachment.openStream()) {
                int b;
                while ((b = inputStream.read()) != -1) {
                    bytes++;
                    checksum += b;
                }
            }

            HashMap<String, Long> map = new HashMap<>();

            map.put("bytes", bytes);
            map.put("checksum", checksum);

            attachmentInfo.add(map);
        }

        HashMap<String, Object> result = new HashMap<>();

        result.put("string", string);
        result.put("strings", strings);
        result.put("number", number);
        result.put("flag", flag);
        result.put("date", date);
        result.put("localDate", localDate);
        result.put("localTime", localTime);
        result.put("localDateTime", localDateTime);
        result.put("attachmentInfo", attachmentInfo);

        return result;
    }

    @RequestMethod("POST")
    public void testPost(String name) throws IOException {
        echo();
    }

    @RequestMethod("PUT")
    public void testPut(int id) throws IOException {
        echo();
    }

    @RequestMethod("PATCH")
    public void testPatch(int id) throws IOException {
        echo();
    }

    private void echo() throws IOException {
        InputStream inputStream = getRequest().getInputStream();
        OutputStream outputStream = getResponse().getOutputStream();

        int b;
        while ((b = inputStream.read()) != -1) {
            outputStream.write(b);
        }

        outputStream.flush();
    }

    @RequestMethod("DELETE")
    public void testDelete(int id) {
        // No-op
    }

    @RequestMethod("GET")
    @ResourcePath("deprecated")
    @Deprecated
    public void testDeprecated() {
        // No-op
    }

    @RequestMethod("GET")
    @ResourcePath("error")
    public void testError() throws Exception {
        throw new Exception("Sample error message.");
    }

    @RequestMethod("GET")
    public int testTimeout(int value, int delay) throws InterruptedException {
        Thread.sleep(delay);

        return value;
    }
}
