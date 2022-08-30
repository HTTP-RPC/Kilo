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

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.util.ResourceBundleAdapter;

import java.io.IOException;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

/**
 * Generates an index of all active services.
 */
public class IndexServlet extends HttpServlet {
    /**
     * Generates the service index.
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var serviceDescriptors = BeanAdapter.adapt(WebService.getServiceDescriptors());

        var accept = request.getHeader("Accept");

        if (accept != null && accept.equalsIgnoreCase("application/json")) {
            response.setContentType("application/json;charset=UTF-8");

            var jsonEncoder = new JSONEncoder();

            jsonEncoder.write(serviceDescriptors, response.getOutputStream());
        } else {
            response.setContentType("text/html;charset=UTF-8");

            var templateEncoder = new TemplateEncoder(WebService.class.getResource("index.html"));

            var resourceBundle = ResourceBundle.getBundle(WebService.class.getPackage().getName() + ".index", request.getLocale());

            templateEncoder.write(mapOf(
                entry("labels", new ResourceBundleAdapter(resourceBundle)),
                entry("contextPath", request.getContextPath()),
                entry("services", serviceDescriptors)
            ), response.getOutputStream());
        }
    }
}
