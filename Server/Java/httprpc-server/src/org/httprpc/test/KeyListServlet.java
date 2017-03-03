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
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.httprpc.DispatcherServlet;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;

import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

/**
 * Servlet that echoes path variables.
 */
@WebServlet(urlPatterns={"/keys/*"}, loadOnStartup=1)
public class KeyListServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    /**
     * Echoes path variables to the output stream.
     *
     * @throws IOException
     * If an error occurs while writing the response.
     */
    @RequestMethod("GET")
    @ResourcePath("/a/?/b/?/c/?")
    public Map<String, ?> echo(String d) throws IOException {
        List<String> keys = getKeys();

        return mapOf(
            entry("a", keys.get(0)),
            entry("b", keys.get(1)),
            entry("c", keys.get(2)),
            entry("d", d)
        );
    }
}
