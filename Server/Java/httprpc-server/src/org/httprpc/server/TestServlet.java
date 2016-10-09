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

package org.httprpc.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Test servlet.
 */
@MultipartConfig
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String pathInfo = request.getPathInfo();

        Object result;
        if (pathInfo == null) {
            HashMap<String, Object> map = new HashMap<>();

            map.put("string", request.getParameter("string"));
            map.put("strings", Arrays.asList(request.getParameterValues("strings")));

            map.put("number", Integer.parseInt(request.getParameter("number")));

            map.put("boolean", Boolean.parseBoolean(request.getParameter("boolean")));

            result = map;
        } else {
            switch (pathInfo.substring(1)) {
                case "longList": {
                    result = new AbstractList<Integer>() {
                        @Override
                        public Integer get(int index) {
                            return index;
                        }

                        @Override
                        public int size() {
                            return Integer.MAX_VALUE;
                        }
                    };

                    break;
                }

                case "delayedResult": {
                    result = request.getParameter("result");

                    int delay = Integer.parseInt(request.getParameter("delay"));

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException exception) {
                        throw new RuntimeException(exception);
                    }

                    break;
                }

                case "sum": {
                    int a = Integer.parseInt(request.getParameter("a"));
                    int b = Integer.parseInt(request.getParameter("b"));

                    result = a + b;

                    break;
                }

                default: {
                    result = null;
                    break;
                }
            }
        }

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        response.setContentType("application/json;charset=UTF-8");

        HashMap<String, Object> result = new HashMap<>();

        result.put("string", request.getParameter("string"));
        result.put("strings", Arrays.asList(request.getParameterValues("strings")));

        result.put("number", Integer.parseInt(request.getParameter("number")));

        result.put("boolean", Boolean.parseBoolean(request.getParameter("boolean")));

        String contentType = request.getContentType();

        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            LinkedList<Map<String, ?>> attachmentInfo = new LinkedList<>();

            for (Part part : request.getParts()) {
                String name = part.getName();

                if (name.equals("attachments")) {
                    long bytes = 0;
                    long checksum = 0;

                    File file = File.createTempFile(part.getName(), "_" + part.getSubmittedFileName());

                    try {
                        part.write(file.getAbsolutePath());

                        try (InputStream inputStream = new FileInputStream(file)) {
                            int b;
                            while ((b = inputStream.read()) != -1) {
                                bytes++;
                                checksum += b;
                            }
                        }
                    } finally {
                        file.delete();
                    }

                    HashMap<String, Object> map = new HashMap<>();

                    map.put("bytes", bytes);
                    map.put("checksum", checksum);

                    attachmentInfo.add(map);
                }
            }

            result.put("attachmentInfo", attachmentInfo);
        }

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String result = request.getParameter("text").equals("héllo") ? "göodbye" : null;

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        boolean result = (Integer.parseInt(request.getParameter("id")) == 101);

        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(result, response.getOutputStream());
    }
}
