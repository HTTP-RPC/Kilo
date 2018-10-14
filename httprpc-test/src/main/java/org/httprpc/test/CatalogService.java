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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.httprpc.WebService;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;

/**
 * Service that simulates a product catalog.
 */
@WebServlet(urlPatterns={"/catalog/*"}, loadOnStartup=1)
public class CatalogService extends WebService {
    private static final long serialVersionUID = 0;

    /**
     * Simulates an item in the product catalog.
     */
    public static class Item {
        private String description;
        private double price;

        private Item(String description, double price) {
            this.description = description;
            this.price = price;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    private ArrayList<Item> items = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        super.init();

        items.add(new Item("Hat", 15.00));
        items.add(new Item("Mittens", 12.00));
        items.add(new Item("Scarf", 9.00));
    }

    @RequestMethod("GET")
    @ResourcePath("items")
    public List<Item> getItems() {
        return items;
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    public int addItem(String description, double price) {
        items.add(new Item(description, price));

        return items.size() - 1;
    }

    @RequestMethod("GET")
    @ResourcePath("items/?:itemID")
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
    public void updateItem(String description, double price) {
        int itemID = Integer.parseInt(getKey("itemID"));

        if (itemID > 0 && itemID <= items.size()) {
            Item item = items.get(itemID);

            item.setDescription(description);
            item.setPrice(price);
        } else {
            throw new IllegalArgumentException("Item not found.");
        }
    }

    @RequestMethod("DELETE")
    @ResourcePath("items/?:itemID")
    public void deleteItem() {
        int itemID = Integer.parseInt(getKey("itemID"));

        if (itemID > 0 && itemID <= items.size()) {
            items.remove(itemID);
        }
    }
}
