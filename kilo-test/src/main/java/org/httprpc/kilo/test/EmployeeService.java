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
import org.hibernate.cfg.Configuration;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;
import org.httprpc.kilo.util.concurrent.Pipe;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebServlet(urlPatterns = {"/employees/*"}, loadOnStartup = 1)
public class EmployeeService extends WebService {
    private static ExecutorService executorService = null;

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

    private Connection getConnection() throws SQLException {
        DataSource dataSource;
        try {
            var initialContext = new InitialContext();

            dataSource = (DataSource)initialContext.lookup("java:comp/env/jdbc/EmployeeDB");
        } catch (NamingException exception) {
            throw new IllegalStateException(exception);
        }

        return dataSource.getConnection();
    }

    @RequestMethod("GET")
    public List<Employee> getEmployees() throws SQLException {
        var queryBuilder = QueryBuilder.select(Employee.class);

        try (var connection = getConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement)) {
            return results.stream().map(result -> BeanAdapter.coerce(result, Employee.class)).toList();
        }
    }

    @RequestMethod("GET")
    @ResourcePath("stream")
    public Iterable<Employee> getEmployeesStream() {
        var pipe = new Pipe<Employee>(4096, 15000);

        executorService.submit(() -> {
            var queryBuilder = QueryBuilder.select(Employee.class);

            try (var connection = getConnection();
                var statement = queryBuilder.prepare(connection);
                var results = queryBuilder.executeQuery(statement)) {
                pipe.accept(results.stream().map(result -> BeanAdapter.coerce(result, Employee.class)));
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });

        return pipe;
    }

    @RequestMethod("GET")
    @ResourcePath("hibernate")
    public List<Employee> getEmployeesHibernate() throws SQLException {
        var configuration = new Configuration();

        configuration.addAnnotatedClass(HibernateEmployee.class);

        try (var connection = getConnection();
            var sessionFactory = configuration.configure().buildSessionFactory();
            var session = sessionFactory.withOptions().connection(connection).openSession()) {
            var criteriaQuery = session.getCriteriaBuilder().createQuery(Employee.class);
            var query = session.createQuery(criteriaQuery.select(criteriaQuery.from(HibernateEmployee.class)));

            return query.list();
        }
    }

    @RequestMethod("GET")
    @ResourcePath("hibernate-stream")
    public Iterable<Employee> getEmployeesHibernateStream() {
        var pipe = new Pipe<Employee>(4096, 15000);

        executorService.submit(() -> {
            var configuration = new Configuration();

            configuration.addAnnotatedClass(HibernateEmployee.class);

            try (var connection = getConnection();
                var sessionFactory = configuration.configure().buildSessionFactory();
                var session = sessionFactory.withOptions().connection(connection).openSession()) {
                var criteriaQuery = session.getCriteriaBuilder().createQuery(Employee.class);
                var query = session.createQuery(criteriaQuery.select(criteriaQuery.from(HibernateEmployee.class)));

                try (var stream = query.stream()) {
                    pipe.accept(stream);
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });

        return pipe;
    }
}
