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

import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.httprpc.DispatcherServlet;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

/**
 * Servlet that simulates a product catalog.
 */
@WebServlet(urlPatterns={"/catalog/*"}, loadOnStartup=1)
public class KeyListServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    private List<?> items = listOf(
        mapOf(
            entry("description", "Hat"),
            entry("price", 15.00)
        ),
        mapOf(
            entry("description", "Mittens"),
            entry("price", 12.00)
        ),
        mapOf(
            entry("description", "Scarf"),
            entry("price", 9.00)
        )
    );

    @RequestMethod("GET")
    @ResourcePath("/items")
    public List<?> getItems() {
        return items;
    }

    @RequestMethod("GET")
    @ResourcePath("/items/?")
    public Object getItem() {
        int index = Integer.parseInt(getKeys().get(0));

        return (index < items.size()) ? items.get(index) : null;
    }
}
