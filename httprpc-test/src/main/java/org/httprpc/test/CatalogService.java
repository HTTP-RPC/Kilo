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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.httprpc.Content;
import org.httprpc.Description;
import org.httprpc.Endpoint;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;
import org.httprpc.WebService;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.beans.Key;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.sql.QueryBuilder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/catalog/*"}, loadOnStartup = 1)
@Description("Simulates a product catalog.")
@Endpoint(path = "items", description = "Item collection.")
@Endpoint(path = "items/?", description = "Item detail.", keys = {"The item ID."})
@Endpoint(path = "sizes", description = "Size collection.")
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

    private DataSource dataSource = null;

    private ThreadLocal<Connection> connection = new ThreadLocal<>();

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Context initialCtx = new InitialContext();
            Context environmentContext = (Context)initialCtx.lookup("java:comp/env");

            dataSource = (DataSource)environmentContext.lookup("jdbc/DemoDB");
        } catch (NamingException exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            this.connection.set(connection);

            try {
                super.service(request, response);

                if (response.getStatus() / 100 == 2) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
            } catch (IOException | SQLException | RuntimeException exception) {
                connection.rollback();

                log(exception.getMessage(), exception);

                throw exception;
            } finally {
                connection.setAutoCommit(true);

                this.connection.remove();
            }
        } catch (SQLException exception) {
            throw new ServletException(exception);
        }
    }

    private Connection getConnection() {
        return connection.get();
    }

    @RequestMethod("GET")
    @ResourcePath("items")
    @Description("Returns a list of all items in the catalog.")
    public List<Item> getItems() throws SQLException {
        List<Map<String, Object>> results = QueryBuilder.select("*").from("item").execute(getConnection()).getResults();

        return BeanAdapter.coerceList(results, Item.class);
    }

    @RequestMethod("POST")
    @ResourcePath("items")
    @Description("Adds an item to the catalog.")
    @Content(Item.class)
    public Item addItem() throws SQLException {
        Item item = getBody();

        Integer itemID = BeanAdapter.coerce(QueryBuilder.insertInto("item", mapOf(
            entry("description", ":description"),
            entry("price", ":price")
        )).execute(getConnection(), mapOf(
            entry("description", item.getDescription()),
            entry("price", item.getPrice())
        )).getGeneratedKeys().get(0), Integer.class);

        Map<String, Object> result = QueryBuilder.select("*").from("item").where("id = :itemID").execute(getConnection(), mapOf(
            entry("itemID", itemID)
        )).getResult();

        getResponse().setStatus(HttpServletResponse.SC_CREATED);

        return BeanAdapter.coerce(result, Item.class);
    }

    @RequestMethod("PUT")
    @ResourcePath("items/?:itemID")
    @Description("Updates an item.")
    @Content(Item.class)
    public void updateItem() throws SQLException {
        Integer itemID = getKey("itemID", Integer.class);

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
    public void deleteItem() throws SQLException {
        Integer itemID = getKey("itemID", Integer.class);

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
