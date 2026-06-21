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
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.kilo.util.concurrent.Pipe;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

@WebServlet(urlPatterns = "/employees/*", loadOnStartup = 0)
public class EmployeeService extends AbstractDatabaseService {
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    protected String getDataSourceName() {
        return EMPLOYEE_DB;
    }

    @RequestMethod("GET")
    public Iterable<Employee> getEmployees(boolean stream) throws SQLException {
        var queryBuilder = QueryBuilder.select(Employee.class);

        var connection = getConnection();

        if (stream) {
            var pipe = new Pipe<Employee>(4096, 15000);

            executorService.submit(() -> {
                try (var statement = queryBuilder.prepare(connection);
                    var results = queryBuilder.executeQuery(statement)) {
                    pipe.submit(mapAll(results, BeanAdapter.toType(Employee.class)));
                }

                return null;
            });

            return pipe;
        } else {
            try (var statement = queryBuilder.prepare(connection);
                var results = queryBuilder.executeQuery(statement)) {
                return listOf(mapAll(results, BeanAdapter.toType(Employee.class)));
            }
        }
    }

    @RequestMethod("GET")
    @ResourcePath("?")
    public EmployeeDetails getEmployee(Integer employeeNumber) throws SQLException {
        var queryBuilder = QueryBuilder.select(EmployeeDetails.class).filterByPrimaryKey("employeeNumber");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("employeeNumber", employeeNumber)
            ))) {
            return map(firstOf(results), BeanAdapter.toType(EmployeeDetails.class));
        }
    }
}
