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

package org.httprpc.kilo;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract base class for page servlets.
 */
public abstract class PageServlet extends HttpServlet {
    @Override
    public void init() throws ServletException {
        var fields = getClass().getDeclaredFields();

        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];

            var fieldType = field.getType();

            if (WebService.class.isAssignableFrom(fieldType) && field.getAnnotation(WebService.Instance.class) != null) {
                field.setAccessible(true);

                try {
                    field.set(this, WebService.instances.get(fieldType));
                } catch (IllegalAccessException exception) {
                    throw new UnsupportedOperationException(exception);
                }
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getMethod().equalsIgnoreCase("GET")) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        try (var connection = openConnection()) {
            if (connection != null) {
                connection.setReadOnly(true);
            }

            WebService.setConnection(connection);

            try {
                process(request, response);
            } catch (Exception exception) {
                log(exception.getMessage(), exception);

                throw exception;
            } finally {
                if (connection != null) {
                    connection.setReadOnly(false);
                }
            }
        } catch (SQLException exception) {
            throw new ServletException(exception);
        } finally {
            WebService.setConnection(null);
        }
    }

    /**
     * Opens a database connection.
     *
     * @return
     * A database connection, or {@code null} if the page does not require a
     * database connection.
     */
    protected Connection openConnection() throws SQLException {
        return null;
    }

    /**
     * Processes a page request.
     *
     * @param request
     * The servlet request.
     *
     * @param response
     * The servlet response.
     *
     * @throws IOException
     * If an I/O error occurs while processing the request.
     */
    protected abstract void process(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Returns the database connection.
     *
     * @return
     * The database connection, or {@code null} if a database connection has
     * not been established.
     */
    protected static Connection getConnection() {
        return WebService.getConnection();
    }
}
