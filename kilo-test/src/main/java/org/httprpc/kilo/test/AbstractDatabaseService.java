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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.httprpc.kilo.WebService;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractDatabaseService extends WebService {
    private static ThreadLocal<Connection> connection = new ThreadLocal<>();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        DataSource dataSource;
        try {
            var initialContext = new InitialContext();

            var environmentContext = (Context)initialContext.lookup("java:comp/env");

            dataSource = (DataSource)environmentContext.lookup("jdbc/DemoDB");
        } catch (NamingException exception) {
            throw new ServletException(exception);
        }

        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            AbstractDatabaseService.connection.set(connection);

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

                AbstractDatabaseService.connection.remove();
            }
        } catch (SQLException exception) {
            throw new ServletException(exception);
        }
    }

    protected static Connection getConnection() {
        return connection.get();
    }
}
