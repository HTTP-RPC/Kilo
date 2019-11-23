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

import org.httprpc.RequestMethod;
import org.httprpc.RequestParameter;
import org.httprpc.ResourcePath;
import org.httprpc.WebService;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.mapOf;

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
        return mapOf(
            entry("string", text),
            entry("strings", strings),
            entry("number", number),
            entry("flag", flag),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime)
        );
    }

    @RequestMethod("GET")
    @ResourcePath("a/?:a/b/?/c/?:c/d/?")
    public Map<String, ?> testGet() {
        return mapOf(
            entry("list", Arrays.asList(getKey(0), getKey(1), getKey(2), getKey(3))),
            entry("map", mapOf(
                entry("a", getKey("a")),
                entry("b", getKey("b")),
                entry("c", getKey("c")),
                entry("d", getKey("d"))
            ))
        );
    }

    @RequestMethod("GET")
    @ResourcePath("fibonacci")
    public Iterable<BigInteger> testGetFibonacci(int count) {
        return () -> new Iterator<BigInteger>() {
            private int i = 0;

            private BigInteger a = BigInteger.valueOf(0);
            private BigInteger b = BigInteger.valueOf(1);

            @Override
            public boolean hasNext() {
                return i < count;
            }

            @Override
            public BigInteger next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                BigInteger next;
                if (i == 0) {
                    next = a;
                } else {
                    if (i > 1) {
                        BigInteger c = a.add(b);

                        a = b;
                        b = c;
                    }

                    next = b;
                }

                i++;

                return next;
            }
        };
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

            attachmentInfo.add(mapOf(
                entry("bytes", bytes),
                entry("checksum", checksum)
            ));
        }

        return mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("flag", flag),
            entry("date", date),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("attachmentInfo", attachmentInfo)
        );
    }

    @RequestMethod("POST")
    public void testPost(String name) throws IOException {
        echo();
    }

    @RequestMethod("PUT")
    public void testPut(int id) throws IOException {
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
    @ResourcePath("unauthorized")
    public void testUnauthorized() {
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

    @Override
    protected boolean isAuthorized(HttpServletRequest request, Method method) {
        String pathInfo = request.getPathInfo();

        return (pathInfo == null || !pathInfo.endsWith("unauthorized"));
    }
}
