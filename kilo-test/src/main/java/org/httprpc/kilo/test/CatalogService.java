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

import org.httprpc.kilo.Content;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.Keys;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.beans.Key;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.sql.QueryBuilder;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/catalog/*"}, loadOnStartup = 1)
@Description("Catalog example service.")
public class CatalogService extends AbstractDatabaseService {
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

    @RequestMethod("GET")
    @ResourcePath("items")
    @Description("Returns a list of all items in the catalog.")
    @SuppressWarnings("unchecked")
    public List<Item> getItems() throws SQLException {
        var results = QueryBuilder.select("*").from("item").execute(getConnection()).getResults();

        return BeanAdapter.coerce(results, List.class, Item.class);
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    @Description("Adds an item to the catalog.")
    @Content(Item.class)
    public Item addItem() throws SQLException {
        Item item = getBody();

        var itemID = BeanAdapter.coerce(QueryBuilder.insertInto("item").values(mapOf(
            entry("description", ":description"),
            entry("price", ":price")
        )).execute(getConnection(), mapOf(
            entry("description", item.getDescription()),
            entry("price", item.getPrice())
        )).getGeneratedKeys().get(0), Integer.class);

        var result = QueryBuilder.select("*").from("item").where("id = :itemID").execute(getConnection(), mapOf(
            entry("itemID", itemID)
        )).getResult();

        getResponse().setStatus(HttpServletResponse.SC_CREATED);

        return BeanAdapter.coerce(result, Item.class);
    }

    @RequestMethod("PUT")
    @ResourcePath("items/?:itemID")
    @Description("Updates an item.")
    @Keys({"The item ID."})
    @Content(Item.class)
    public void updateItem() throws SQLException {
        var itemID = getKey("itemID", Integer.class);

        Item item = getBody();

        QueryBuilder.update("item").set(mapOf(
            entry("description", ":description"),
            entry("price", ":price")
        )).where("id = :itemID").execute(getConnection(), mapOf(
            entry("itemID", itemID),
            entry("description", item.getDescription()),
            entry("price", item.getPrice())
        ));
    }

    @RequestMethod("DELETE")
    @ResourcePath("items/?:itemID")
    @Description("Deletes an item.")
    @Keys({"The item ID."})
    public void deleteItem() throws SQLException {
        var itemID = getKey("itemID", Integer.class);

        QueryBuilder.deleteFrom("item").where("id = :itemID").execute(getConnection(), mapOf(
            entry("itemID", itemID)
        ));
    }

    @RequestMethod("GET")
    @ResourcePath("sizes")
    @Description("Returns a list of size options.")
    public List<Size> getSizes() {
        return Arrays.asList(Size.values());
    }
}
