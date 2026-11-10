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

package org.httprpc.kilo.web.service;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Generates API documentation.
 */
@WebServlet(urlPatterns = {"", "*.html", "*.json"}, loadOnStartup = Integer.MAX_VALUE)
public class IndexServlet extends HttpServlet {
    private Map<String, WebService.ServiceDescriptor> serviceDescriptors = new TreeMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        for (var instance : WebService.getAll()) {
            var fields = instance.getClass().getDeclaredFields();

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];

                var fieldType = field.getType();

                if (WebService.class.isAssignableFrom(fieldType) && field.getAnnotation(WebService.Instance.class) != null) {
                    field.setAccessible(true);

                    try {
                        field.set(instance, WebService.getInstance((Class<? extends WebService>)fieldType));
                    } catch (IllegalAccessException exception) {
                        throw new UnsupportedOperationException(exception);
                    }
                }
            }

            var serviceDescriptor = instance.getServiceDescriptor();

            serviceDescriptors.put(serviceDescriptor.getPath(), serviceDescriptor);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var servletPath = request.getServletPath();

        if (servletPath.isEmpty()) {
            response.setContentType(WebService.TEXT_HTML);

            var locale = request.getLocale();

            var templateEncoder = new TemplateEncoder(IndexServlet.class, "index.html");

            templateEncoder.setLocale(locale);
            templateEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getName(), locale));

            templateEncoder.write(mapOf(
                entry("language", locale.getLanguage()),
                entry("contextPath", getServletContext().getContextPath()),
                entry("services", serviceDescriptors.values())
            ), response.getOutputStream());
        } else {
            var serviceDescriptor = serviceDescriptors.get(servletPath.substring(0, servletPath.lastIndexOf(".")));

            if (serviceDescriptor == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (servletPath.endsWith(".html")) {
                response.setContentType(WebService.TEXT_HTML);

                var locale = request.getLocale();

                var templateEncoder = new TemplateEncoder(IndexServlet.class, "service.html");

                templateEncoder.setLocale(locale);
                templateEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getName(), locale));

                templateEncoder.write(mapOf(
                    entry("language", locale.getLanguage()),
                    entry("contextPath", getServletContext().getContextPath()),
                    entry("service", serviceDescriptor)
                ), response.getOutputStream());
            } else if (servletPath.endsWith(".json")) {
                response.setContentType(WebService.APPLICATION_JSON);

                var jsonEncoder = new JSONEncoder();

                jsonEncoder.write(serviceDescriptor, response.getOutputStream());
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}
