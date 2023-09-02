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
import org.httprpc.kilo.Description;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.kilo.sql.ResultSetAdapter;
import org.httprpc.kilo.util.concurrent.Pipe;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.httprpc.kilo.util.Collections.mapOf;

@WebServlet(urlPatterns = {"/employees/*"}, loadOnStartup = 1)
@Description("Employee example service.")
public class EmployeeService extends WebService {
    private DataSource dataSource = null;
    private ExecutorService executorService = null;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Context initialCtx = new InitialContext();

            var environmentContext = (Context)initialCtx.lookup("java:comp/env");

            dataSource = (DataSource)environmentContext.lookup("jdbc/EmployeeDB");
        } catch (NamingException exception) {
            throw new IllegalStateException(exception);
        }

        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void destroy() {
        super.destroy();

        executorService.shutdown();
    }

    @RequestMethod("GET")
    @Description("Returns a list of all employees.")
    @SuppressWarnings("unchecked")
    public List<Employee> getEmployees(
        @Description("Indicates that results should be streamed.") boolean stream
    ) {
        var queryBuilder = QueryBuilder.select(
            "emp_no as employeeNumber",
            "first_name as firstName",
            "last_name as lastName",
            "gender",
            "birth_date as birthDate",
            "hire_date as hireDate"
        ).from("employees");

        if (stream) {
            var pipe = new Pipe<Employee>();

            executorService.submit(() -> {
                try (var connection = dataSource.getConnection();
                    var statement = queryBuilder.prepare(connection);
                    var resultSet = queryBuilder.executeQuery(statement, mapOf());
                    var resultSetAdapter = new ResultSetAdapter(resultSet)) {
                    pipe.accept(resultSetAdapter.stream().map(result -> BeanAdapter.coerce(result, Employee.class)));
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }
            });

            return pipe;
        } else {
            try (var connection = dataSource.getConnection()) {
                return BeanAdapter.coerce(queryBuilder.execute(connection).getResults(), List.class, Employee.class);
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
