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
import java.util.HashMap;

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
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getMethod().equalsIgnoreCase("PATCH")) {
            doPut(request, response);
        } else {
            super.service(request, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        String value = request.getParameter("value");

        if (value == null) {
            PrintWriter writer = response.getWriter();

            writer.write("{");

            writer.write(String.format("\"string\": \"%s\", ", request.getParameter("string")));

            String[] strings = request.getParameterValues("strings");

            writer.write("\"strings\": [");

            for (int i = 0; i < strings.length; i++) {
                if (i > 0) {
                    writer.write(", ");
                }

                writer.write(String.format("\"%s\"", strings[i]));
            }

            writer.write("], ");

            writer.write(String.format("\"number\": %d, \"flag\": %b",
                Integer.parseInt(request.getParameter("number")),
                Boolean.parseBoolean(request.getParameter("flag"))));

            writer.write("}");
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
        response.setContentType("application/json;charset=UTF-8");

        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        String contentType = request.getContentType().toLowerCase();

        if (contentType.startsWith("application/json")) {
            InputStream inputStream = request.getInputStream();
            OutputStream outputStream = response.getOutputStream();

            int b;
            while ((b = inputStream.read()) != -1) {
                outputStream.write((byte)b);
            }
        } else {
            PrintWriter writer = response.getWriter();

            writer.write("{");

            writer.write(String.format("\"string\": \"%s\", ", request.getParameter("string")));

            String[] strings = request.getParameterValues("strings");

            writer.write("\"strings\": [");

            for (int i = 0; i < strings.length; i++) {
                if (i > 0) {
                    writer.write(", ");
                }

                writer.write(String.format("\"%s\"", strings[i]));
            }

            writer.write("], ");

            writer.write(String.format("\"number\": %d, \"flag\": %b",
                Integer.parseInt(request.getParameter("number")),
                Boolean.parseBoolean(request.getParameter("flag"))));

            writer.write(", \"attachmentInfo\": [");

            if (contentType.startsWith("multipart/form-data")) {
                int i = 0;

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

                        if (i > 0) {
                            writer.write(", ");
                        }

                        HashMap<String, Object> values = new HashMap<>();

                        values.put("bytes", bytes);
                        values.put("checksum", checksum);

                        writer.write(String.format("{\"bytes\": %d, \"checksum\": %d}", bytes, checksum));

                        i++;
                    }
                }
            }

            writer.write("]");

            writer.write("}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        String contentType = request.getContentType();

        if (contentType != null && contentType.toLowerCase().startsWith("application/json")) {
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
