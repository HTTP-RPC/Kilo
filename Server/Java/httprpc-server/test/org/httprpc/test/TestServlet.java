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
import java.io.PrintWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

import org.httprpc.DispatcherServlet;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

/**
 * Test servlet.
 */
@WebServlet(urlPatterns={"/test/*"}, loadOnStartup=1)
@MultipartConfig
public class TestServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    @RequestMethod("GET")
    public Map<String, ?> testGet(String string, List<String> strings, int number, boolean flag) {
        return mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("flag", flag)
        );
    }

    @RequestMethod("GET")
    public int testGet(int value, int delay) throws InterruptedException {
        Thread.sleep(delay);

        return value;
    }

    @RequestMethod("GET")
    @ResourcePath("/a/?/b/?/c/?/d/?")
    public List<String> testGet() {
        return getKeys();
    }

    @RequestMethod("GET")
    @ResourcePath("/error")
    public void testError() throws IOException {
        HttpServletResponse response = getResponse();

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/plain");

        PrintWriter writer = response.getWriter();

        writer.append("Sample error message");
        writer.flush();
    }

    @RequestMethod("POST")
    public Map<String, ?> testPost(String string, List<String> strings, int number, boolean flag, List<URL> attachments) throws IOException {
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
            entry("attachmentInfo", attachmentInfo)
        );
    }

    @RequestMethod("PUT")
    public List<?> testPut(String text, Map<String, ?> map, List<?> list) {
        return listOf(text, map, list);
    }

    @RequestMethod("PATCH")
    public String testPatch(String text) {
        return text;
    }

    @RequestMethod("DELETE")
    public boolean testDelete(int id) {
        return (id == 101);
    }
}
