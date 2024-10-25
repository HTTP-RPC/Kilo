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

package org.httprpc.kilo;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Generates an index of all active services.
 */
@WebServlet(urlPatterns = {""}, loadOnStartup = Integer.MAX_VALUE)
public class IndexServlet extends HttpServlet {
    /**
     * Generates the service index.
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var serviceDescriptors = WebService.getServiceDescriptors();

        var accept = request.getHeader("Accept");

        if (accept != null && accept.equalsIgnoreCase(WebService.APPLICATION_JSON)) {
            response.setContentType(String.format(WebService.CONTENT_TYPE_FORMAT, WebService.APPLICATION_JSON, StandardCharsets.UTF_8));

            var jsonEncoder = new JSONEncoder();

            jsonEncoder.write(serviceDescriptors, response.getOutputStream());
        } else {
            response.setContentType(String.format(WebService.CONTENT_TYPE_FORMAT, WebService.TEXT_HTML, StandardCharsets.UTF_8));

            var templateEncoder = new TemplateEncoder(IndexServlet.class, "index.html");

            templateEncoder.setResourceBundle(ResourceBundle.getBundle(IndexServlet.class.getName(), request.getLocale()));

            templateEncoder.write(mapOf(
                entry("contextPath", request.getContextPath()),
                entry("services", serviceDescriptors)
            ), response.getOutputStream());
        }
    }
}
