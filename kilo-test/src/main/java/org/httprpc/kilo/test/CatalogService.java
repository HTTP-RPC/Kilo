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

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.Content;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.Keys;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.kilo.sql.ResultSetAdapter;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/catalog/*"}, loadOnStartup = 1)
@Description("Catalog example service.")
public class CatalogService extends AbstractDatabaseService {
    @RequestMethod("GET")
    @ResourcePath("items")
    @Description("Returns a list of all items in the catalog.")
    public List<Item> getItems() throws SQLException {
        var queryBuilder = QueryBuilder.select("*").from("item");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = new ResultSetAdapter(queryBuilder.executeQuery(statement))) {
            return results.stream().map(result -> BeanAdapter.coerce(result, Item.class)).toList();
        }
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    @Description("Adds an item to the catalog.")
    @Content(type = Item.class)
    public Item addItem() throws SQLException {
        var item = (Item)getBody();

        var queryBuilder = QueryBuilder.insertInto("item").values(mapOf(
            entry("description", ":description"),
            entry("price", ":price")
        ));

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(item));
        }

        getResponse().setStatus(HttpServletResponse.SC_CREATED);

        return getItem(BeanAdapter.coerce(queryBuilder.getGeneratedKeys().get(0), Integer.class));
    }

    private Item getItem(int itemID) throws SQLException {
        var queryBuilder = QueryBuilder.select("*").from("item").where("id = :id");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = new ResultSetAdapter(queryBuilder.executeQuery(statement, mapOf(
                entry("id", itemID)
            )))) {
            return results.stream().findFirst().map(result -> BeanAdapter.coerce(result, Item.class)).orElse(null);
        }
    }

    @RequestMethod("PUT")
    @ResourcePath("items/?:itemID")
    @Description("Updates an item.")
    @Keys({"The item ID."})
    @Content(type = Item.class)
    public void updateItem() throws SQLException {
        var id = getKey("itemID", Integer.class);

        var item = (Item)getBody();

        item.setID(id);

        var queryBuilder = QueryBuilder.update("item").set(mapOf(
            entry("description", ":description"),
            entry("price", ":price")
        )).where("id = :id");

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(item));
        }
    }

    @RequestMethod("DELETE")
    @ResourcePath("items/?:itemID")
    @Description("Deletes an item.")
    @Keys({"The item ID."})
    public void deleteItem() throws SQLException {
        var id = getKey("itemID", Integer.class);

        var queryBuilder = QueryBuilder.deleteFrom("item").where("id = :id");

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("id", id)
            ));
        }
    }

    @RequestMethod("GET")
    @ResourcePath("sizes")
    @Description("Returns a list of size options.")
    public List<Size> getSizes() {
        return Arrays.asList(Size.values());
    }
}
