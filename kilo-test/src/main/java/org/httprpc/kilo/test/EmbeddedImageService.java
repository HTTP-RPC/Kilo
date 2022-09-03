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

package org.httprpc.kilo.test;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.io.TemplateEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/embedded-image/*"}, loadOnStartup = 1)
@MultipartConfig
public class EmbeddedImageService extends WebService {
    @RequestMethod("GET")
    public void embedImage() throws IOException {
        byte[] data;
        try (var inputStream = getClass().getResourceAsStream("test.jpg");
            var byteArrayOutputStream = new ByteArrayOutputStream(4096)) {

            int b;
            while ((b = inputStream.read()) != -1) {
                byteArrayOutputStream.write(b);
            }

            data = byteArrayOutputStream.toByteArray();
        }

        var response = getResponse();

        response.setContentType("text/html");

        var templateEncoder = new TemplateEncoder(getClass().getResource("image.html"));

        templateEncoder.write(mapOf(
            entry("data", data)
        ), response.getOutputStream());
    }
}
