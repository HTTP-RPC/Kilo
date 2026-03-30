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
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

/**
 * Generates API documentation.
 */
@WebServlet({"", "*.html"})
public class IndexServlet extends HttpServlet {
    private static final String HTML_EXTENSION = ".html";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");

        var services = WebService.getServiceDescriptors();

        if (request.getPathInfo() != null) {
            var templateEncoder = new TemplateEncoder(IndexServlet.class, "index.html");

            var locale = request.getLocale();

            templateEncoder.setResourceBundle(getResourceBundle("index", locale));
            templateEncoder.setLocale(locale);

            templateEncoder.write(mapOf(
                entry("language", locale.getLanguage()),
                entry("contextPath", request.getContextPath()),
                entry("services", services)
            ), response.getOutputStream());
        } else {
            var servletPath = request.getServletPath();

            if (!servletPath.endsWith(HTML_EXTENSION)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            var path = servletPath.substring(0, servletPath.length() - HTML_EXTENSION.length());

            var service = firstOf(filter(services, whereEqualTo(WebService.ServiceDescriptor::getPath, path)));

            if (service == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            var templateEncoder = new TemplateEncoder(WebService.class, "api.html");

            var locale = request.getLocale();

            templateEncoder.setResourceBundle(getResourceBundle("api", locale));
            templateEncoder.setLocale(locale);

            templateEncoder.write(mapOf(
                entry("language", locale.getLanguage()),
                entry("contextPath", request.getContextPath()),
                entry("service", service)
            ), response.getOutputStream());
        }
    }

    private static ResourceBundle getResourceBundle(String name, Locale locale) {
        return ResourceBundle.getBundle(String.format("%s.%s", IndexServlet.class.getPackage().getName(), name), locale);
    }
}
