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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Generates API documentation.
 */
@WebServlet(urlPatterns = {"", "*.html"}, loadOnStartup = Integer.MAX_VALUE)
public class IndexServlet extends HttpServlet {
    private Map<String, WebService.ServiceDescriptor> serviceDescriptors = new TreeMap<>();

    @Override
    public void init() {
        for (var entry : WebService.instances.entrySet()) {
            var type = entry.getKey();
            var instance = entry.getValue();

            var fields = type.getDeclaredFields();

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];

                var fieldType = field.getType();

                if (WebService.class.isAssignableFrom(fieldType) && field.getAnnotation(WebService.Instance.class) != null) {
                    field.setAccessible(true);

                    try {
                        field.set(instance, WebService.instances.get(fieldType));
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
        response.setContentType(WebService.TEXT_HTML);

        var locale = request.getLocale();
        var resourceBundle = ResourceBundle.getBundle(IndexServlet.class.getName(), locale);

        var servletContext = getServletContext();

        if (request.getPathInfo() != null) {
            var templateEncoder = new TemplateEncoder(IndexServlet.class, "index.html");

            templateEncoder.setLocale(locale);
            templateEncoder.setResourceBundle(resourceBundle);

            templateEncoder.write(mapOf(
                entry("language", locale.getLanguage()),
                entry("title", servletContext.getServletContextName()),
                entry("contextPath", servletContext.getContextPath()),
                entry("services", serviceDescriptors.values())
            ), response.getOutputStream());
        } else {
            var servletPath = request.getServletPath();

            var path = servletPath.substring(0, servletPath.lastIndexOf("."));

            var serviceDescriptor = serviceDescriptors.get(path);

            if (serviceDescriptor == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            var templateEncoder = new TemplateEncoder(IndexServlet.class, "api.html");

            templateEncoder.setLocale(locale);
            templateEncoder.setResourceBundle(resourceBundle);

            templateEncoder.write(mapOf(
                entry("language", locale.getLanguage()),
                entry("contextPath", servletContext.getContextPath()),
                entry("service", serviceDescriptor)
            ), response.getOutputStream());
        }
    }
}
