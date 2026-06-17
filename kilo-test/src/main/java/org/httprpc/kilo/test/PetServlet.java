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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.PageServlet;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.io.CSVEncoder;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.sql.QueryBuilder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

@WebServlet("/pets/stream")
public class PetServlet extends PageServlet {
    private DataSource dataSource = null;

    @Override
    public void init() throws ServletException {
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
    protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var accept = map(request.getHeader("Accept"), String::toLowerCase);

        if (accept == null) {
            return;
        }

        var owner = request.getParameter("owner");

        if (owner == null) {
            return;
        }

        var queryBuilder = QueryBuilder.select(Pet.class)
            .filterByForeignKey(Owner.class, "owner")
            .ordered(true);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("owner", owner)
            ))) {
            if (accept.startsWith(WebService.TEXT_XML)) {
                response.setContentType(WebService.TEXT_XML);

                var templateEncoder = new TemplateEncoder(getClass(), "pets.xml");

                templateEncoder.write(results, response.getWriter());
            } else if (accept.startsWith(WebService.TEXT_HTML)) {
                response.setContentType(WebService.TEXT_HTML);

                var templateEncoder = new TemplateEncoder(getClass(), "pets.html");

                templateEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getName(), request.getLocale()));

                templateEncoder.write(results, response.getWriter());
            } else if (accept.startsWith(WebService.TEXT_CSV)) {
                response.setContentType(WebService.TEXT_CSV);

                var csvEncoder = new CSVEncoder(listOf("name", "species", "sex", "birth", "death"));

                csvEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getName(), request.getLocale()));

                csvEncoder.write(results, response.getWriter());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            }
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}
