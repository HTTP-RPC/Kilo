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

import org.httprpc.Description;
import org.httprpc.RequestMethod;
import org.httprpc.WebService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns={"/echo/*"}, loadOnStartup=1)
@Description("Echoes a string value by writing a custom response.")
public class EchoService extends WebService {
    private static final long serialVersionUID = 0;

    @RequestMethod("GET")
    @Description("Echoes a string value to the servlet response stream.")
    public void echo(String value) throws IOException {
        HttpServletResponse response = getResponse();

        response.setContentType("text/plain");

        if (value != null) {
            PrintWriter writer = response.getWriter();

            writer.append(value);
            writer.flush();
        }
    }
}
