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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.jtemplate.TemplateEncoder;

/**
 * Test servlet.
 */
@WebServlet(urlPatterns={"/test/*"}, loadOnStartup=1)
@MultipartConfig
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String value = request.getParameter("value");

        if (value == null) {
            HashMap<String, Object> result = new HashMap<>();

            result.put("string", request.getParameter("string"));
            result.put("strings", Arrays.asList(request.getParameterValues("strings")));
            result.put("number", Integer.parseInt(request.getParameter("number")));
            result.put("flag", Boolean.parseBoolean(request.getParameter("flag")));

            TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("test.json.txt"));
            encoder.writeValue(result, response.getOutputStream());
        } else {
            String delay = request.getParameter("delay");

            try {
                Thread.sleep((delay == null) ? 0 : Integer.parseInt(delay));
            } catch (InterruptedException exception) {
                throw new ServletException(exception);
            }

            response.getWriter().write(value);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Pass this from client?
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        response.setContentType("application/json;charset=UTF-8");

        String contentType = request.getContentType().toLowerCase();

        if (contentType.startsWith("application/json")) {
            InputStream inputStream = request.getInputStream();
            OutputStream outputStream = response.getOutputStream();

            int b;
            while ((b = inputStream.read()) != -1) {
                outputStream.write((byte)b);
            }
        } else {
            HashMap<String, Object> result = new HashMap<>();

            result.put("string", request.getParameter("string"));
            result.put("strings", Arrays.asList(request.getParameterValues("strings")));
            result.put("number", Integer.parseInt(request.getParameter("number")));
            result.put("flag", Boolean.parseBoolean(request.getParameter("flag")));

            LinkedList<Map<String, ?>> attachmentInfo = new LinkedList<>();

            if (contentType.startsWith("multipart/form-data")) {
                for (Part part : request.getParts()) {
                    String submittedFileName = part.getSubmittedFileName();

                    if (submittedFileName == null || submittedFileName.length() == 0) {
                        continue;
                    }

                    if (part.getName().equals("attachments")) {
                        long bytes = 0;
                        long checksum = 0;

                        try (InputStream inputStream = part.getInputStream()) {
                            int b;
                            while ((b = inputStream.read()) != -1) {
                                bytes++;
                                checksum += b;
                            }
                        }

                        HashMap<String, Object> values = new HashMap<>();

                        values.put("bytes", bytes);
                        values.put("checksum", checksum);

                        attachmentInfo.add(values);
                    }
                }
            }

            result.put("attachmentInfo", attachmentInfo);

            TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("test.json.txt"));
            encoder.writeValue(result, response.getOutputStream());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().startsWith("application/json")) {
            // TODO Pass this from client?
            if (request.getCharacterEncoding() == null) {
                request.setCharacterEncoding("UTF-8");
            }

            InputStream inputStream = request.getInputStream();
            OutputStream outputStream = response.getOutputStream();

            int b;
            while ((b = inputStream.read()) != -1) {
                outputStream.write((byte)b);
            }
        } else {
            response.getWriter().write("\"" + request.getParameter("text") + "\"");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write(Boolean.toString(Integer.parseInt(request.getParameter("id")) == 101));
    }
}
