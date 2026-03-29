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

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Generates API documentation.
 */
@WebServlet("*.html")
@WebListener
public class IndexServlet extends HttpServlet implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        // TODO
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(String.format(WebService.CONTENT_TYPE_FORMAT, WebService.TEXT_HTML, StandardCharsets.UTF_8));

        var templateEncoder = new TemplateEncoder(IndexServlet.class, "index.html");

        var locale = request.getLocale();

        templateEncoder.setResourceBundle(ResourceBundle.getBundle(IndexServlet.class.getName(), locale));
        templateEncoder.setLocale(locale);

        templateEncoder.write(mapOf(
            entry("language", locale.getLanguage()),
            entry("contextPath", request.getContextPath()),
            entry("services", null) // TODO
        ), response.getOutputStream());
    }
}
