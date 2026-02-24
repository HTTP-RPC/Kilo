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
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.io.CSVEncoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.sql.QueryBuilder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

@WebServlet(urlPatterns = {"/pets/*"}, loadOnStartup = 1)
public class PetService extends AbstractDatabaseService {
    @RequestMethod("GET")
    public List<Pet> getPets(@Required String owner) throws SQLException {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("select * from pet where owner = :owner order by name");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("owner", owner)
            ))) {
            return collect(mapAll(results, toType(Pet.class)), toList());
        }
    }

    @RequestMethod("GET")
    @ResourcePath("stream")
    public void getPetsStream(@Required String owner) throws SQLException, IOException {
        var response = getResponse();

        var accept = getRequest().getHeader("Accept");

        if (accept == null) {
            throw new UnsupportedOperationException();
        }

        var queryBuilder = QueryBuilder.select(Pet.class)
            .filterByForeignKey(Owner.class, "owner")
            .ordered(true);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("owner", owner)
            ))) {
            if (accept.equalsIgnoreCase(APPLICATION_JSON)) {
                response.setContentType(APPLICATION_JSON);

                var jsonEncoder = new JSONEncoder();

                jsonEncoder.write(results, response.getOutputStream());
            } else if (accept.equalsIgnoreCase(TEXT_XML)) {
                response.setContentType(TEXT_XML);

                var templateEncoder = new TemplateEncoder(getClass(), "pets.xml");

                templateEncoder.write(results, response.getOutputStream());
            } else if (accept.equalsIgnoreCase(TEXT_HTML)) {
                response.setContentType(TEXT_HTML);

                var templateEncoder = new TemplateEncoder(getClass(), "pets.html");

                templateEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getName(), getRequest().getLocale()));

                templateEncoder.write(results, response.getOutputStream());
            } else if (accept.equalsIgnoreCase(TEXT_CSV)) {
                response.setContentType(TEXT_CSV);

                var csvEncoder = new CSVEncoder(listOf("name", "species", "sex", "birth", "death"));

                csvEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getName(), getRequest().getLocale()));

                csvEncoder.write(results, response.getOutputStream());
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}
