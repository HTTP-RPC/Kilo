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

import java.sql.SQLException;
import java.util.Date;
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
        var queryBuilder = QueryBuilder.select(Item.class).ordered(true);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement)) {
            return results.stream().map(result -> BeanAdapter.coerce(result, Item.class)).toList();
        }
    }

    @RequestMethod("GET")
    @ResourcePath("items/?")
    @Description("Returns detailed information about a specific item.")
    public ItemDetail getItem(
        @Description("The item ID.") Integer itemID
    ) throws SQLException {
        var queryBuilder = QueryBuilder.select(ItemDetail.class).filterByPrimaryKey("itemID");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("itemID", itemID)
            ))) {
            return results.stream().findFirst().map(result -> BeanAdapter.coerce(result, ItemDetail.class)).orElse(null);
        }
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    @Description("Adds an item to the catalog.")
    @Creates
    public ItemDetail addItem(
        @Description("The item to add.") ItemDetail item
    ) throws SQLException {
        var queryBuilder = QueryBuilder.insert(ItemDetail.class);

        item.setCreated(new Date());

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(item));
        }

        return getItem(queryBuilder.getGeneratedKey(0, Integer.class));
    }

    @RequestMethod("PUT")
    @ResourcePath("items/?")
    @Description("Updates an item.")
    public ItemDetail updateItem(
        @Description("The item ID.") Integer itemID,
        @Description("The updated item.") ItemDetail item
    ) throws SQLException {
        item.setID(itemID);

        var queryBuilder = QueryBuilder.update(ItemDetail.class).filterByPrimaryKey("id");

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(item));
        }

        return getItem(itemID);
    }

    @RequestMethod("DELETE")
    @ResourcePath("items/?")
    @Description("Deletes an item.")
    public void deleteItem(
        @Description("The item ID.") Integer itemID
    ) throws SQLException {
        var queryBuilder = QueryBuilder.delete(Item.class).filterByPrimaryKey("itemID");

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("itemID", itemID)
            ));
        }
    }
}
