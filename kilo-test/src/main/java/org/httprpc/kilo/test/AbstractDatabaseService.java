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
import org.httprpc.kilo.WebService;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractDatabaseService extends WebService {
    private DataSource dataSource = null;

    public static final String DEMO_DB = "jdbc/DemoDB";
    public static final String EMPLOYEE_DB = "jdbc/EmployeeDB";
    public static final String SAKILA_DB = "jdbc/SakilaDB";

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            var initialContext = new InitialContext();
            var environmentContext = (Context)initialContext.lookup("java:comp/env");

            dataSource = (DataSource)environmentContext.lookup(getDataSourceName());
        } catch (NamingException exception) {
            throw new ServletException(exception);
        }
    }

    protected abstract String getDataSourceName();

    @Override
    protected Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
