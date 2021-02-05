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
import org.httprpc.ResourcePath;
import org.httprpc.WebService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns={"/catalog/*"}, loadOnStartup=1)
@Description("Simulates a product catalog.")
public class CatalogService extends WebService {
    public static class Item {
        private String description;
        private double price;

        private Item(String description, double price) {
            this.description = description;
            this.price = price;
        }

        @Description("The item's description.")
        public String getDescription() {
            return description;
        }

        private void setDescription(String description) {
            this.description = description;
        }

        @Description("The item's price.")
        public double getPrice() {
            return price;
        }

        private void setPrice(double price) {
            this.price = price;
        }
    }

    private ArrayList<Item> items = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        super.init();

        addItem("Hat", 15.00);
        addItem("Mittens", 12.00);
        addItem("Scarf", 9.00);
    }

    @RequestMethod("GET")
    @ResourcePath("items")
    @Description("Returns a list of all items in the catalog.")
    public List<Item> getItems() {
        return items;
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    @Description("Adds a new item to the catalog.")
    public int addItem(
        @Description("The item's description.") String description,
        @Description("The item's price.") double price
    ) {
        items.add(new Item(description, price));

        HttpServletResponse response = getResponse();

        if (response != null) {
            response.setStatus(HttpServletResponse.SC_CREATED);
        }

        return items.size();
    }

    @RequestMethod("GET")
    @ResourcePath("items/?:itemID")
    @Description("Returns a single item.")
    public Item getItem() {
        int itemID = Integer.parseInt(getKey("itemID"));

        Item item;
        if (itemID > 0 && itemID <= items.size()) {
            item = items.get(itemID - 1);
        } else {
            item = null;
        }

        return item;
    }

    @RequestMethod("POST")
    @ResourcePath("items/?:itemID")
    @Description("Updates an item.")
    public void updateItem(
        @Description("The item's description.") String description,
        @Description("The item's price.") double price
    ) {
        int itemID = Integer.parseInt(getKey("itemID"));

        if (itemID > 0 && itemID <= items.size()) {
            Item item = items.get(itemID - 1);

            item.setDescription(description);
            item.setPrice(price);
        } else {
            throw new IllegalArgumentException("Item not found.");
        }
    }
}
