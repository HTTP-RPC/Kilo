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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import org.httprpc.kilo.PageServlet;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.CSVEncoder;
import org.httprpc.kilo.sql.QueryBuilder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

@WebServlet("/pets/example")
public class PetServlet extends PageServlet {
    private interface Parameters {
        @Required
        String getOwner();
    }

    private DataSource dataSource = null;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            var initialContext = new InitialContext();
            var environmentContext = (Context)initialContext.lookup("java:comp/env");

            dataSource = (DataSource)environmentContext.lookup(AbstractDatabaseService.DEMO_DB);
        } catch (NamingException exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    protected Object execute() throws IOException, SQLException {
        var parameters = getParameters(Parameters.class);

        var owner = parameters.getOwner();

        var queryBuilder = QueryBuilder.select(Pet.class)
            .filterByForeignKey(Owner.class, "owner")
            .ordered(true);

        List<Pet> pets;
        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("owner", owner)
            ))) {
            pets = listOf(mapAll(results, BeanAdapter.toType(Pet.class)));
        }

        var request = getRequest();

        var accept = map(request.getHeader("Accept"), String::toLowerCase);

        if (accept != null && accept.equals(WebService.TEXT_CSV)) {
            var response = getResponse();

            response.setContentType(WebService.TEXT_CSV);

            var csvEncoder = new CSVEncoder(listOf("name", "species", "sex", "birth", "death"));

            csvEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getName(), request.getLocale()));

            csvEncoder.write(pets, response.getWriter());

            return null;
        }

        return pets;
    }
}
