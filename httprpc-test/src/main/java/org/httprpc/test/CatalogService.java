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

import org.httprpc.Content;
import org.httprpc.Description;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;
import org.httprpc.WebService;
import org.httprpc.beans.Key;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet(urlPatterns={"/catalog/*"}, loadOnStartup=1)
@Description("Simulates a product catalog.")
public class CatalogService extends WebService {
    @Description("Represents an item in the catalog.")
    public static class Item {
        private Integer id;
        private String description;
        private Double price;

        @Key("id")
        @Description("The item's ID.")
        public Integer getID() {
            return id;
        }

        @Key("id")
        public void setID(int id) {
            this.id = id;
        }

        @Description("The item's description.")
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Description("The item's price.")
        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }

    @Description("Represents a size option.")
    public enum Size {
        @Description("A small size.")
        SMALL,
        @Description("A medium size.")
        MEDIUM,
        @Description("A large size.")
        LARGE
    }

    private Map<Integer, Item> items = new HashMap<>();

    private int nextID = 1;

    @RequestMethod("GET")
    @ResourcePath("items")
    @Description("Returns a list of all items in the catalog.")
    public Iterable<Item> getItems() {
        return items.values();
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    @Description("Adds an item to the catalog.")
    @Content(Item.class)
    public Item addItem() {
        Item item = getBody();

        item.id = nextID++;

        items.put(item.id, item);

        getResponse().setStatus(HttpServletResponse.SC_CREATED);

        return item;
    }

    @RequestMethod("PUT")
    @ResourcePath("items/?:itemID")
    @Description("Updates an item.")
    @Content(Item.class)
    public void updateItem() {
        int itemID = getKey("itemID", Integer.class);

        if (!items.containsKey(itemID)) {
            throw new NoSuchElementException();
        }

        Item item = getBody();

        if (itemID != item.id) {
            throw new IllegalArgumentException();
        }

        items.put(itemID, item);
    }

    @RequestMethod("DELETE")
    @ResourcePath("items/?:itemID")
    @Description("Deletes an item.")
    public void deleteItem() {
        items.remove(Integer.parseInt(getKey("itemID")));
    }

    @RequestMethod("GET")
    @ResourcePath("sizes")
    @Description("Returns a list of size options.")
    public List<Size> getSizes() {
        return Arrays.asList(Size.values());
    }
}
