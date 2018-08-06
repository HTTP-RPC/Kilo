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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.httprpc.WebService;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;

/**
 * Service that simulates a product catalog using path variables.
 */
@WebServlet(urlPatterns={"/catalog/*"}, loadOnStartup=1)
public class CatalogService extends WebService {
    private static final long serialVersionUID = 0;

    private List<?> items = null;

    @Override
    public void init() throws ServletException {
        super.init();

        ArrayList<HashMap<String, Object>> items = new ArrayList<>();

        HashMap<String, Object> item1 = new HashMap<>();

        item1.put("description", "Hat");
        item1.put("price", 15.00);

        items.add(item1);

        HashMap<String, Object> item2 = new HashMap<>();

        item2.put("description", "Mittens");
        item2.put("price", 12.00);

        items.add(item2);

        HashMap<String, Object> item3 = new HashMap<>();

        item3.put("description", "Scarf");
        item3.put("price", 9.00);

        items.add(item3);

        this.items = items;
    }

    @RequestMethod("GET")
    @ResourcePath("items")
    public List<?> getItems() {
        return items;
    }

    @RequestMethod("GET")
    @ResourcePath("items/?")
    public Object getItem() {
        int index = Integer.parseInt(getKey(0));

        return (index < items.size()) ? items.get(index - 1) : null;
    }
}
