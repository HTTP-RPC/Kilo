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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Test servlet.
 */
@WebServlet(urlPatterns={"/test/*"}, loadOnStartup=1)
@MultipartConfig
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String value = request.getParameter("value");

        Object result;
        if (value == null) {
            result = null; // TODO
            /*
            result = mapOf(
                entry("string", request.getParameter("string")),
                entry("strings", Arrays.asList(request.getParameterValues("strings"))),
                entry("number", Integer.parseInt(request.getParameter("number"))),
                entry("flag", Boolean.parseBoolean(request.getParameter("flag")))
            );
            */
        } else {
            String delay = request.getParameter("delay");

            try {
                Thread.sleep((delay == null) ? 0 : Integer.parseInt(delay));
            } catch (InterruptedException exception) {
                throw new ServletException(exception);
            }

            result = Integer.parseInt(value);
        }

        writeResult(result, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contentType = request.getContentType();

        if (contentType == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        contentType = contentType.toLowerCase();

        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        Map<String, ?> result;
        if (contentType.startsWith("application/json")) { // TODO Constant
            result = null; // TODO
            /*
            JSONDecoder decoder = new JSONDecoder();

            result = (Map<String, ?>)decoder.readValue(request.getInputStream());
            */
        } else {
            LinkedList<Map<String, ?>> attachmentInfo = new LinkedList<>();

            if (contentType.startsWith("multipart/form-data")) { // TODO Constant
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

                        // TODO
                        /*
                        attachmentInfo.add(mapOf(
                            entry("bytes", bytes),
                            entry("checksum", checksum))
                        );
                        */
                    }
                }
            }

            result = null; // TODO
            /*
            result = mapOf(
                entry("string", request.getParameter("string")),
                entry("strings", Arrays.asList(request.getParameterValues("strings"))),
                entry("number", Integer.parseInt(request.getParameter("number"))),
                entry("flag", Boolean.parseBoolean(request.getParameter("flag"))),
                entry("attachmentInfo", attachmentInfo)
            );
            */
        }

        writeResult(result, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contentType = request.getContentType();

        String result;
        if (contentType != null && contentType.startsWith("application/json")) { // TODO Constant
            // TODO
            /*
            JSONDecoder decoder = new JSONDecoder();

            Map<String, ?> arguments = (Map<String, ?>)decoder.readValue(request.getInputStream());

            result = arguments.get("text").equals("héllo") ? "göodbye" : null;
            */
            result = null; // TODO
        } else {
            result = request.getParameter("text").equals("héllo") ? "göodbye" : null;
        }

        writeResult(result, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeResult(Integer.parseInt(request.getParameter("id")) == 101, response);
    }

    private void writeResult(Object result, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        // TODO
        /*
        JSONEncoder encoder = new JSONEncoder();

        encoder.writeValue(result, response.getOutputStream());
        */
    }
}
