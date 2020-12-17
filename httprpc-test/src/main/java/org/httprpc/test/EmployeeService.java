package org.httprpc.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.httprpc.RequestMethod;
import org.httprpc.WebService;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.io.JSONEncoder;
import org.httprpc.sql.QueryBuilder;
import org.httprpc.sql.ResultSetAdapter;
import org.httprpc.util.StreamAdapter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@WebServlet(urlPatterns={"/employees/*"}, loadOnStartup=1)
public class EmployeeService extends WebService {
    public interface Employee {
        int getEmployeeNumber();
        String getFirstName();
        String getLastName();
        String getGender();
        Date getBirthDate();
        Date getHireDate();
    }

    @Entity
    @Table(name="employees")
    public static class EmployeeEntity implements Employee {
        @Id
        @Column(name = "emp_no")
        private int employeeNumber;

        @Column(name = "first_name")
        private String firstName;

        @Column(name = "last_name")
        private String lastName;

        @Column(name = "gender")
        private String gender;

        @Column(name = "birth_date")
        private Date birthDate;

        @Column(name = "hire_date")
        private Date hireDate;

        @Override
        public int getEmployeeNumber() {
            return employeeNumber;
        }

        @Override
        public String getFirstName() {
            return firstName;
        }

        @Override
        public String getLastName() {
            return lastName;
        }

        @Override
        public String getGender() {
            return gender;
        }

        @Override
        public Date getBirthDate() {
            return birthDate;
        }

        @Override
        public Date getHireDate() {
            return hireDate;
        }
    }

    private DataSource dataSource = null;
    private SessionFactory sessionFactory = null;

    private ThreadLocal<Boolean> useJackson = new ThreadLocal<>();

    private static final String SQL_QUERY = QueryBuilder.select("emp_no as employeeNumber",
        "first_name as firstName",
        "last_name as lastName",
        "gender",
        "birth_date as birthDate",
        "hire_date as hireDate").from("employees").toString();

    private static final String HQL_QUERY = QueryBuilder.select("e").from("EmployeeService$EmployeeEntity e").toString();

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Context initialCtx = new InitialContext();
            Context environmentContext = (Context)initialCtx.lookup("java:comp/env");

            dataSource = (DataSource)environmentContext.lookup("jdbc/EmployeesDB");
        } catch (NamingException exception) {
            throw new ServletException(exception);
        }

        Properties properties = new Properties();

        properties.setProperty("hibernate.connection.datasource", "java:comp/env/jdbc/EmployeesDB");

        Configuration configuration = new Configuration();

        configuration.setProperties(properties);
        configuration.addAnnotatedClass(EmployeeEntity.class);

        sessionFactory = configuration.buildSessionFactory();
    }

    @Override
    public void destroy() {
        super.destroy();

        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            super.service(request, response);
        } finally {
            useJackson.remove();
        }
    }

    @Override
    protected void encodeResult(HttpServletRequest request, HttpServletResponse response, Object result) throws IOException {
        if (useJackson.get()) {
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(response.getOutputStream(), result);
        } else {
            super.encodeResult(request, response, result);
        }
    }

    @RequestMethod("GET")
    public List<? extends Employee> getEmployees(boolean hql, boolean jackson, boolean stream) throws IOException, SQLException {
        if (stream) {
            streamEmployees(hql, jackson);
            return null;
        } else {
            return getEmployees(hql, jackson);
        }
    }

    private List<? extends Employee> getEmployees(boolean hql, boolean jackson) throws SQLException {
        useJackson.set(jackson);

        if (hql) {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery(HQL_QUERY, EmployeeEntity.class).list();
            }
        } else {
            try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY))) {
                return resultSetAdapter.stream()
                    .map(result -> (Employee)BeanAdapter.adapt(result, Employee.class))
                    .collect(Collectors.toList());
            }
        }
    }

    private void streamEmployees(boolean hql, boolean jackson) throws IOException, SQLException {
        getResponse().setContentType("application/json");

        if (jackson) {
            streamEmployeesJackson(hql);
        } else {
            JSONEncoder jsonEncoder = new JSONEncoder();

            if (hql) {
                try (Session session = sessionFactory.openSession();
                    StreamAdapter<Map<String, ?>> streamAdapter = new StreamAdapter<>(session.createQuery(HQL_QUERY, EmployeeEntity.class).stream().map(BeanAdapter::new))) {
                    jsonEncoder.write(streamAdapter, getResponse().getOutputStream());
                }
            } else {
                try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement();
                    ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY))) {
                    jsonEncoder.write(resultSetAdapter, getResponse().getOutputStream());
                }
            }
        }
    }

    private void streamEmployeesJackson(boolean hql) throws IOException, SQLException {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        ObjectWriter objectWriter = objectMapper.writer();

        if (hql) {
            try (Session session = sessionFactory.openSession();
                StreamAdapter<? extends Employee> streamAdapter = new StreamAdapter<>(session.createQuery(HQL_QUERY, EmployeeEntity.class).stream());
                SequenceWriter sequenceWriter = objectWriter.writeValues(getResponse().getOutputStream())) {
                sequenceWriter.init(true);

                for (Employee employee : streamAdapter) {
                    sequenceWriter.write(employee);
                }
            }
        } else {
            try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY));
                SequenceWriter sequenceWriter = objectWriter.writeValues(getResponse().getOutputStream())) {
                sequenceWriter.init(true);

                for (Map<String, ?> result : resultSetAdapter) {
                    sequenceWriter.write(result);
                }
            }
        }
    }
}
