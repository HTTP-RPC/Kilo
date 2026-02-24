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
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.kilo.util.concurrent.Pipe;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Iterables.*;

@WebServlet(urlPatterns = {"/employees/*"}, loadOnStartup = 1)
public class EmployeeService extends WebService {
    private static ExecutorService executorService = null;

    @Override
    protected String getDataSourceName() {
        return "java:comp/env/jdbc/EmployeeDB";
    }

    @Override
    public void init() throws ServletException {
        super.init();

        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void destroy() {
        executorService.shutdown();

        super.destroy();
    }

    @RequestMethod("GET")
    public List<Employee> getEmployees() throws SQLException {
        var queryBuilder = QueryBuilder.select(Employee.class);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement)) {
            return collect(mapAll(results, toType(Employee.class)), toList());
        }
    }

    @RequestMethod("GET")
    @ResourcePath("stream")
    public Iterable<Employee> getEmployeesStream() {
        var pipe = new Pipe<Employee>(4096, 15000);

        var connection = getConnection();

        executorService.submit(() -> {
            var queryBuilder = QueryBuilder.select(Employee.class);

            try (var statement = queryBuilder.prepare(connection);
                var results = queryBuilder.executeQuery(statement)) {
                pipe.submit(mapAll(results, toType(Employee.class)));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });

        return pipe;
    }
}
