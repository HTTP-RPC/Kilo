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
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;
import org.httprpc.Response;
import org.httprpc.WebService;
import org.httprpc.io.JSONEncoder;
import org.httprpc.sql.Parameters;
import org.httprpc.sql.ResultSetAdapter;

/**
 * Employee service.
 */
@WebServlet(urlPatterns={"/employees/*"}, loadOnStartup=1)
public class EmployeeService extends WebService {
    private static final long serialVersionUID = 0;

    private static final String DB_URL = "jdbc:mysql://db.local:3306/employees?user=root&password=password&useSSL=false";

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
    @Response("[{\n"
        + "  id: integer,\n"
        + "  firstName: string,\n"
        + "  lastName: string\n"
        + "}]")
    public void listEmployees(String name) throws SQLException, IOException {
        Parameters parameters = Parameters.parse("SELECT emp_no AS id, "
            + "first_name AS firstName, "
            + "last_name AS lastName "
            + "FROM employees "
            + "WHERE first_name LIKE :name "
            + "OR last_name LIKE :name");

        parameters.put("name", (name == null) ? "%" : name.replace('*', '%'));

        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement);

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetAdapter resultSetAdapter = new ResultSetAdapter(resultSet);

                getResponse().setContentType("application/json");

                JSONEncoder jsonEncoder = new JSONEncoder();

                jsonEncoder.writeValue(resultSetAdapter, getResponse().getOutputStream());
            }
        }
    }

    @RequestMethod("POST")
    public int addEmployee(String firstName,
        String lastName,
        String gender,
        Date birthDate,
        Date hireDate) throws SQLException {
        Parameters parameters = Parameters.parse("INSERT INTO employees "
            + "(first_name, last_name, gender, birth_date, hire_date) VALUES "
            + "(:firstName, :lastName, :gender, :birthDate, :hireDate)");

        parameters.put("firstName", firstName);
        parameters.put("lastName", lastName);
        parameters.put("gender", gender);
        parameters.put("birthDate", birthDate);
        parameters.put("hireDate", hireDate);

        int id;
        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement);
            statement.execute();

            id = statement.getGeneratedKeys().getInt(1);
        }

        return id;
    }

    @RequestMethod("GET")
    @ResourcePath("?:id")
    @Response("{\n"
        + "  id: integer,\n"
        + "  firstName: string,\n"
        + "  lastName: string,\n"
        + "  gender: string,\n"
        + "  birthDate: string,\n"
        + "  hireDate: string,\n"
        + "  titles: [{\n"
        + "    title: string,\n"
        + "    fromDate: date,\n"
        + "    toDate: date\n"
        + "  }],\n"
        + "  salaries: [{\n"
        + "    salary: integer,\n"
        + "    fromDate: date,\n"
        + "    toDate: date\n"
        + "  }]\n"
        + "}")
    public void getEmployee(List<String> details) throws SQLException, IOException {
        String id = getKey("id");

        Parameters parameters = Parameters.parse("SELECT emp_no AS id, "
            + "first_name AS firstName, "
            + "last_name AS lastName, "
            + "gender, "
            + "birth_date AS birthDate, "
            + "hire_date AS hireDate "
            + "FROM employees WHERE emp_no = :id");

        parameters.put("id", id);

        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement);

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetAdapter resultSetAdapter = new ResultSetAdapter(resultSet);

                for (String detail : details) {
                    switch (detail) {
                        case "titles": {
                            resultSetAdapter.attach("titles", "SELECT title, "
                                + "from_date AS fromDate, "
                                + "to_date AS toDate "
                                + "FROM titles WHERE emp_no = :id");

                            break;
                        }

                        case "salaries": {
                            resultSetAdapter.attach("salaries", "SELECT salary, "
                                + "from_date AS fromDate, "
                                + "to_date AS toDate "
                                + "FROM salaries WHERE emp_no = :id");

                            break;
                        }
                    }
                }

                getResponse().setContentType("application/json");

                JSONEncoder jsonEncoder = new JSONEncoder();

                jsonEncoder.writeValue(resultSetAdapter.next(), getResponse().getOutputStream());
            }
        }
    }

    @RequestMethod("POST")
    @ResourcePath("?:id")
    public void updateEmployee(String firstName,
        String lastName,
        String gender,
        Date birthDate,
        Date hireDate) throws SQLException {
        String id = getKey("id");

        Parameters parameters = Parameters.parse("UPDATE employees "
            + "SET first_name = COALESCE(:firstName, first_name), "
            + "SET last_name = COALESCE(:lastName, last_name), "
            + "SET gender = COALESCE(:gender, gender), "
            + "SET birth_date = COALESCE(:birthDate, birth_date), "
            + "SET hire_date = COALESCE(:hireDate, hire_date), "
            + "WHERE emp_no = :id");

        parameters.put("id", id);
        parameters.put("firstName", firstName);
        parameters.put("lastName", lastName);
        parameters.put("gender", gender);
        parameters.put("birthDate", birthDate);
        parameters.put("hireDate", hireDate);

        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement);
            statement.execute();
        }
    }

    @RequestMethod("DELETE")
    @ResourcePath("?:id")
    public void deleteEmployee() throws SQLException {
        String id = getKey("id");

        Parameters parameters = Parameters.parse("DELETE FROM employees WHERE emp_no = :id");

        parameters.put("id", id);

        try (Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement);
            statement.execute();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
