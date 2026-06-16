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
public abstract class PageServlet<P> extends HttpServlet {
    @Override
    public void init() throws ServletException {
        // TODO Validate path
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try (var connection = openConnection()) {
            if (connection != null) {
                connection.setReadOnly(true);
            }

            WebService.setConnection(connection);

            super.service(request, response);
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO
    }

    /**
     * Executes a page request.
     *
     * @param parameters
     * The page parameters.
     *
     * @return
     * The page result.
     */
    protected abstract Object execute(P parameters);
}
