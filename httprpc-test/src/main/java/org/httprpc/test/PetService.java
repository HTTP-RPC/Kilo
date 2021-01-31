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
import org.httprpc.WebService;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.io.CSVEncoder;
import org.httprpc.io.JSONEncoder;
import org.httprpc.io.TemplateEncoder;
import org.httprpc.sql.Parameters;
import org.httprpc.sql.QueryBuilder;
import org.httprpc.sql.ResultSetAdapter;
import org.httprpc.util.ResourceBundleAdapter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;

/**
 * Pet service.
 */
@WebServlet(urlPatterns={"/pets/*"}, loadOnStartup=1)
public class PetService extends WebService {
    private DataSource dataSource = null;

    public interface Pet {
        Date getBirth();
    }

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Context initialCtx = new InitialContext();
            Context environmentContext = (Context)initialCtx.lookup("java:comp/env");

            dataSource = (DataSource)environmentContext.lookup("jdbc/MenagerieDB");
        } catch (NamingException exception) {
            throw new ServletException(exception);
        }
    }

    @RequestMethod("GET")
    public void getPets(String owner, String format) throws SQLException, IOException {
        Parameters parameters = Parameters.parse(QueryBuilder.select("name", "species", "sex", "birth")
            .from("pet")
            .where("owner = :owner").toString());

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement, mapOf(
                entry("owner", owner)
            ));

            try (ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery())) {
                if (format == null || format.equals("json")) {
                    getResponse().setContentType("application/json");

                    JSONEncoder jsonEncoder = new JSONEncoder();

                    jsonEncoder.write(resultSetAdapter, getResponse().getOutputStream());
                } else if (format.equals("csv")) {
                    getResponse().setContentType("text/csv");

                    CSVEncoder csvEncoder = new CSVEncoder(listOf("name", "species", "sex", "birth"));

                    csvEncoder.setLabels(mapOf(
                        entry("name", "Name"),
                        entry("species", "Species"),
                        entry("sex", "Sex"),
                        entry("birth", "Birth")
                    ));

                    csvEncoder.setFormats(mapOf(
                        entry("birth", DateFormat.getDateInstance(DateFormat.LONG))
                    ));

                    csvEncoder.write(resultSetAdapter, getResponse().getOutputStream());
                } else if (format.equals("html")) {
                    getResponse().setContentType("text/html");

                    TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource("pets.html"));

                    ResourceBundle resourceBundle = ResourceBundle.getBundle(getClass().getPackage().getName() + ".pets", getRequest().getLocale());

                    templateEncoder.write(mapOf(
                        entry("headings", new ResourceBundleAdapter(resourceBundle)),
                        entry("data", resultSetAdapter)
                    ), getResponse().getOutputStream());
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    @RequestMethod("GET")
    @ResourcePath("average-age")
    public double getAverageAge() throws SQLException {
        String sql = QueryBuilder.select("birth").from("pet").toString();

        double averageAge;
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(sql))) {
            Date now = new Date();

            averageAge = resultSetAdapter.stream()
                .map(result -> (Pet)BeanAdapter.adapt(result, Pet.class))
                .mapToLong(pet -> now.getTime() - (pet.getBirth()).getTime()).average().getAsDouble();
        }

        return averageAge / (365.0 * 24.0 * 60.0 * 60.0 * 1000.0);
    }
}
