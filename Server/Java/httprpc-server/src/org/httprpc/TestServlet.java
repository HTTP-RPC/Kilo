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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;

import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;
import org.jtemplate.ResponseMapping;

/**
 * Test servlet.
 */
@WebServlet(urlPatterns={
    "/test/get.json",
    "/test/post.json",
    "/test/put.json",
    "/test/delete.json",
    "/test/longList.json",
    "/test/delayedResult.json",
    "/test/sum.json"
}, loadOnStartup=1)
@MultipartConfig
public class TestServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    @RequestMethod("GET")
    @ResponseMapping(name="test/get.json")
    public Map<String, ?> testGet(String string, List<String> strings, int number, boolean flag) {
        return mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("flag", flag)
        );
    }

    @RequestMethod("POST")
    @ResponseMapping(name="test/post.json")
    public Map<String, ?> testPost(String string, List<String> strings, int number, boolean flag, List<URL> attachments) throws IOException {
        LinkedList<Map<String, ?>> attachmentInfo = new LinkedList<>();

        for (URL url : attachments) {
            long bytes = 0;
            long checksum = 0;

            try (InputStream inputStream = url.openStream()) {
                int b;
                while ((b = inputStream.read()) != -1) {
                    bytes++;
                    checksum += b;
                }
            }

            attachmentInfo.add(mapOf(
                entry("bytes", bytes),
                entry("checksum", checksum))
            );
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
    @ResponseMapping(name="test/put.json")
    public String testPut(String text) {
        return text.equals("héllo") ? "göodbye" : null;
    }

    @RequestMethod("DELETE")
    @ResponseMapping(name="test/delete.json")
    public boolean testDelete(int id) {
        return (id == 101);
    }

    @RequestMethod("GET")
    @ResponseMapping(name="test/longList.json")
    public List<?> getLongList() {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                return index;
            }

            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }
        };
    }

    @RequestMethod("GET")
    @ResponseMapping(name="test/delayedResult.json")
    public String getDelayedResult(String result, int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }

    @RequestMethod("GET")
    @ResponseMapping(name="test/sum.json")
    public int getSum(int a, int b) {
        return a + b;
    }
}
