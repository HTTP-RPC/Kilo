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

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

/**
 * Generates API documentation.
 */
@WebServlet({"", "*.html"})
@WebListener
public class IndexServlet extends HttpServlet implements ServletContextListener {
    private static final String HTML_EXTENSION = ".html";

    private static final TreeMap<String, WebService.ServiceDescriptor> services = new TreeMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void contextInitialized(ServletContextEvent event) {
        var servletContext = event.getServletContext();

        var instanceFields = new HashMap<Class<?>, List<Field>>();

        for (var entry : servletContext.getServletRegistrations().entrySet()) {
            var name = entry.getKey();

            Class<?> type;
            try {
                type = Class.forName(entry.getValue().getClassName());
            } catch (ClassNotFoundException exception) {
                throw new RuntimeException(exception);
            }

            if (!WebService.class.isAssignableFrom(type)) {
                continue;
            }

            var webServlet = type.getAnnotation(WebServlet.class);

            if (webServlet == null) {
                throw new IllegalStateException("Missing web servlet annotation.");
            }

            var urlPattern = coalesce(firstOf(iterableOf(webServlet.value())), () -> firstOf(iterableOf(webServlet.urlPatterns())));

            if (urlPattern == null) {
                throw new IllegalStateException("Missing URL pattern.");
            }

            if (!(urlPattern.startsWith("/") && urlPattern.endsWith("/*"))) {
                throw new IllegalStateException("Invalid URL pattern.");
            }

            var servicePath = urlPattern.substring(0, urlPattern.length() - 2);

            Servlet servlet;
            try {
                servlet = servletContext.createServlet((Class<? extends Servlet>)type);
            } catch (ServletException exception) {
                throw new RuntimeException(exception);
            }

            servletContext.addServlet(name, servlet);

            services.put(servicePath, ((WebService)servlet).getServiceDescriptor());

            var fields = type.getDeclaredFields();

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];

                if (WebService.class.isAssignableFrom(field.getType())
                    && field.getAnnotation(WebService.Instance.class) != null) {
                    instanceFields.computeIfAbsent(type, key -> new LinkedList<>()).add(field);
                }
            }
        }

        for (var entry : instanceFields.entrySet()) {
            var webService = WebService.getInstance((Class<? extends WebService>)entry.getKey());

            for (var field : entry.getValue()) {
                field.setAccessible(true);

                var instance = WebService.getInstance((Class<? extends WebService>)field.getType());

                try {
                    field.set(webService, instance);
                } catch (IllegalAccessException exception) {
                    throw new UnsupportedOperationException(exception);
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");

        if (request.getPathInfo() != null) {
            var templateEncoder = new TemplateEncoder(IndexServlet.class, "index.html");

            var locale = request.getLocale();

            templateEncoder.setResourceBundle(getResourceBundle("index", locale));
            templateEncoder.setLocale(locale);

            templateEncoder.write(mapOf(
                entry("language", locale.getLanguage()),
                entry("contextPath", request.getContextPath()),
                entry("services", mapAll(services.entrySet(), entry -> entry(entry.getKey(), entry.getValue())))
            ), response.getOutputStream());
        } else {
            var servletPath = request.getServletPath();

            if (!servletPath.endsWith(HTML_EXTENSION)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            var servicePath = servletPath.substring(0, servletPath.length() - HTML_EXTENSION.length());

            var service = services.get(servicePath);

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
                entry("servicePath", servicePath),
                entry("service", service)
            ), response.getOutputStream());
        }
    }

    private static ResourceBundle getResourceBundle(String name, Locale locale) {
        return ResourceBundle.getBundle(String.format("%s.%s", IndexServlet.class.getPackage().getName(), name), locale);
    }
}
