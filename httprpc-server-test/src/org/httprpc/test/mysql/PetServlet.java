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

package org.httprpc.test.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.httprpc.DispatcherServlet;
import org.httprpc.JSONEncoder;
import org.httprpc.RequestMethod;
import org.httprpc.sql.Parameters;
import org.httprpc.sql.ResultSetAdapter;
import org.jtemplate.TemplateEncoder;

/**
 * Pet servlet.
 */
@WebServlet(urlPatterns={"/pets"}, loadOnStartup=1)
public class PetServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    private static final String DB_URL = "jdbc:mysql://db.local:3306/menagerie?user=root&password=password";

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ServletException(exception);
        }
    }

    @RequestMethod("GET")
    public void getPets(String owner, String format) throws SQLException, IOException {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            Parameters parameters = Parameters.parse("select name, species, sex, birth from pet where owner = :owner");

            parameters.put("owner", owner);

            try (PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
                parameters.apply(statement);

                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetAdapter resultSetAdapter = new ResultSetAdapter(resultSet);

                    if (format == null) {
                        JSONEncoder jsonEncoder = new JSONEncoder();

                        jsonEncoder.writeValue(resultSetAdapter, getResponse().getOutputStream());
                    } else {
                        String name = String.format("%s~%s", getRequest().getServletPath().substring(1), format);

                        getResponse().setContentType(String.format("%s;charset=UTF-8", getServletContext().getMimeType(name)));

                        TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource(String.format("%s.txt", name)));

                        templateEncoder.setBaseName(getClass().getName());
                        templateEncoder.writeValue(resultSetAdapter, getResponse().getOutputStream(), getRequest().getLocale());
                    }
                }
            }
        } finally {
            getResponse().flushBuffer();
        }
    }
}
