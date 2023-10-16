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
import org.httprpc.kilo.Creates;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.kilo.sql.ResultSetAdapter;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.httprpc.kilo.test.Item.Schema.DESCRIPTION;
import static org.httprpc.kilo.test.Item.Schema.ID;
import static org.httprpc.kilo.test.Item.Schema.PRICE;
import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/catalog/*"}, loadOnStartup = 1)
@Description("Catalog example service.")
public class CatalogService extends AbstractDatabaseService {
    @RequestMethod("GET")
    @ResourcePath("items")
    @Description("Returns a list of all items in the catalog.")
    public List<Item> getItems() throws SQLException {
        var queryBuilder = QueryBuilder.selectAll().from(Item.Schema.class);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = new ResultSetAdapter(queryBuilder.executeQuery(statement))) {
            return results.stream().map(result -> BeanAdapter.coerce(result, Item.class)).toList();
        }
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    @Description("Adds an item to the catalog.")
    @Creates
    public Item addItem(
        @Description("The item to add.") Item item
    ) throws SQLException {
        var queryBuilder = QueryBuilder.insertInto(Item.Schema.class, DESCRIPTION, PRICE)
            .values("description", "price");

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(item));
        }

        return getItem(BeanAdapter.coerce(queryBuilder.getGeneratedKeys().get(0), Integer.class));
    }

    private Item getItem(int itemID) throws SQLException {
        var queryBuilder = QueryBuilder.selectAll().from(Item.Schema.class).where(ID.eq("id"));

        try (var statement = queryBuilder.prepare(getConnection());
            var results = new ResultSetAdapter(queryBuilder.executeQuery(statement, mapOf(
                entry("id", itemID)
            )))) {
            return results.stream().findFirst().map(result -> BeanAdapter.coerce(result, Item.class)).orElse(null);
        }
    }

    @RequestMethod("PUT")
    @ResourcePath("items/?")
    @Description("Updates an item.")
    public void updateItem(
        @Description("The item ID.") Integer itemID,
        @Description("The updated item.") Item item
    ) throws SQLException {
        item.setID(itemID);

        var queryBuilder = QueryBuilder.update(Item.Schema.class, DESCRIPTION, PRICE)
            .set("description", "price")
            .where(ID.eq("id"));

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(item));
        }
    }

    @RequestMethod("DELETE")
    @ResourcePath("items/?")
    @Description("Deletes an item.")
    public void deleteItem(
        @Description("The item ID.") Integer itemID
    ) throws SQLException {
        var queryBuilder = QueryBuilder.deleteFrom(Item.Schema.class).where(ID.eq("id"));

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("id", itemID)
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
