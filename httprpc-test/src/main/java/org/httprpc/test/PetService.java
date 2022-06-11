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

import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.io.CSVEncoder;
import org.httprpc.io.TemplateEncoder;
import org.httprpc.sql.QueryBuilder;
import org.httprpc.sql.ResultSetAdapter;
import org.httprpc.util.ResourceBundleAdapter;

import jakarta.servlet.annotation.WebServlet;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/pets/*"}, loadOnStartup = 1)
public class PetService extends AbstractDatabaseService {
    public interface Pet {
        String getName();
        String getOwner();
        String getSpecies();
        String getSex();
        Date getBirth();
        Date getDeath();
    }

    @RequestMethod("GET")
    public List<Pet> getPets(String owner) throws SQLException {
        QueryBuilder queryBuilder = QueryBuilder.select("*").from("pet").where("owner = :owner");

        List<Map<String, Object>> results = queryBuilder.execute(getConnection(), mapOf(
            entry("owner", owner)
        )).getResults();

        return BeanAdapter.coerceList(results, Pet.class);
    }

    @RequestMethod("GET")
    public void getPets(String owner, String format) throws SQLException, IOException {
        QueryBuilder queryBuilder = QueryBuilder.select("*").from("pet").where("owner = :owner");

        try (PreparedStatement statement = queryBuilder.prepare(getConnection());
            ResultSetAdapter results = new ResultSetAdapter(queryBuilder.executeQuery(statement, mapOf(
                entry("owner", owner)
            )))) {
            if (format.equals("csv")) {
                getResponse().setContentType("text/csv");

                CSVEncoder csvEncoder = new CSVEncoder(listOf("name", "species", "sex", "birth", "death"));

                csvEncoder.setLabels(mapOf(
                    entry("name", "Name"),
                    entry("species", "Species"),
                    entry("sex", "Sex"),
                    entry("birth", "Birth"),
                    entry("death", "Death")
                ));

                csvEncoder.setFormats(mapOf(
                    entry("birth", DateFormat.getDateInstance(DateFormat.LONG))
                ));

                csvEncoder.write(results, getResponse().getOutputStream());
            } else if (format.equals("html")) {
                getResponse().setContentType("text/html");

                TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource("pets.html"));

                ResourceBundle resourceBundle = ResourceBundle.getBundle(getClass().getPackage().getName() + ".pets", getRequest().getLocale());

                templateEncoder.write(mapOf(
                    entry("headings", new ResourceBundleAdapter(resourceBundle)),
                    entry("data", results)
                ), getResponse().getOutputStream());
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    @RequestMethod("GET")
    @ResourcePath("average-age")
    public double getAverageAge() throws SQLException {
        String sql = QueryBuilder.select("birth").from("pet").toString();

        double averageAge;
        try (Statement statement = getConnection().createStatement();
            ResultSetAdapter results = new ResultSetAdapter(statement.executeQuery(sql))) {
            Date now = new Date();

            averageAge = results.stream()
                .map(result -> (Pet)BeanAdapter.coerce(result, Pet.class))
                .mapToLong(pet -> now.getTime() - (pet.getBirth()).getTime()).average().getAsDouble();
        }

        return averageAge / (365.0 * 24.0 * 60.0 * 60.0 * 1000.0);
    }
}
