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

package org.httprpc.example;

import java.io.IOException;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.httprpc.DispatcherServlet;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;

import static org.httprpc.WebServiceProxy.listOf;

/**
 * Key list example servlet.
 */
@WebServlet(urlPatterns={"/keys/*"}, loadOnStartup=1)
public class KeyListServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    @RequestMethod("GET")
    public List<?> getValues() throws IOException {
        return listOf("a", "b", "c");
    }

    @RequestMethod("GET")
    @ResourcePath("/?")
    public String getValue() throws IOException {
        return getKeys().get(0);
    }
}
