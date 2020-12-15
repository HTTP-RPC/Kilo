package org.httprpc.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;
import org.httprpc.WebService;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.beans.Key;
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
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@WebServlet(urlPatterns={"/employess/*"}, loadOnStartup=1)
public class EmployeeService extends WebService {
    public interface Employee {
        @Key("emp_no")
        int getEmployeeNumber();

        @Key("first_name")
        String getFirstName();

        @Key("last_name")
        String getLastName();

        @Key("gender")
        String getGender();

        @Key("birth_date")
        Date getBirthDate();

        @Key("hire_date")
        Date getHireDate();
    }

    @Entity
    @Table(name="employees")
    public static class EmployeeEntity {
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

        public int getEmployeeNumber() {
            return employeeNumber;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getGender() {
            return gender;
        }

        public Date getBirthDate() {
            return birthDate;
        }

        public Date getHireDate() {
            return hireDate;
        }
    }

    private DataSource dataSource = null;

    private SessionFactory sessionFactory = null;

    private static final String SQL_QUERY = QueryBuilder.select("*").from("employees").toString();
    private static final String HQL_QUERY = QueryBuilder.select("e").from("EmployeeEntity e").toString();

    private ThreadLocal<Boolean> useJackson = new ThreadLocal<>();

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

        useJackson.set(false);
    }

    @RequestMethod("GET")
    public List<Employee> getEmployees() throws SQLException {
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY))) {
            resultSetAdapter.setFetchSize(2048);

            return resultSetAdapter.stream()
                .map(result -> (Employee)BeanAdapter.adapt(result, Employee.class))
                .collect(Collectors.toList());
        }
    }

    @RequestMethod("GET")
    @ResourcePath("jackson")
    public List<Employee> getEmployeesJackson() throws SQLException {
        useJackson.set(true);

        try {
            return getEmployees();
        } finally {
            useJackson.set(false);
        }
    }

    @RequestMethod("GET")
    @ResourcePath("hibernate")
    public List<EmployeeEntity> getEmployeesHibernate() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(HQL_QUERY, EmployeeEntity.class).list();
        }
    }

    @RequestMethod("GET")
    @ResourcePath("hibernate-jackson")
    public List<EmployeeEntity> getEmployeesHibernateJackson() {
        useJackson.set(true);

        try {
            return getEmployeesHibernate();
        } finally {
            useJackson.set(false);
        }
    }

    @Override
    protected void encodeResult(HttpServletRequest request, HttpServletResponse response, Object result) throws IOException {
        if (useJackson.get()) {
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            objectMapper.writeValue(getResponse().getOutputStream(), result);
        } else {
            super.encodeResult(request, response, result);
        }
    }

    @RequestMethod("GET")
    @ResourcePath("stream")
    public void streamEmployees() throws SQLException, IOException {
        getResponse().setContentType("application/json");

        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY))) {
            resultSetAdapter.setFetchSize(2048);

            JSONEncoder jsonEncoder = new JSONEncoder();

            jsonEncoder.write(resultSetAdapter, getResponse().getOutputStream());
        }
    }

    @RequestMethod("GET")
    @ResourcePath("stream-jackson")
    public void streamEmployeesJackson() throws SQLException, IOException {
        getResponse().setContentType("application/json");

        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY))) {
            resultSetAdapter.setFetchSize(2048);

            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            ObjectWriter objectWriter = objectMapper.writer();

            try (SequenceWriter sequenceWriter = objectWriter.writeValues(getResponse().getOutputStream())) {
                sequenceWriter.init(true);

                resultSetAdapter.stream()
                    .map(result -> (Employee)BeanAdapter.adapt(result, Employee.class))
                    .forEach(employee -> {
                    try {
                        sequenceWriter.write(employee);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    @RequestMethod("GET")
    @ResourcePath("stream-hibernate")
    public void streamEmployeesHibernate() throws IOException {
        getResponse().setContentType("application/json");

        try (Session session = sessionFactory.openSession();
            StreamAdapter<EmployeeEntity> streamAdapter = new StreamAdapter<>(session.createQuery(HQL_QUERY, EmployeeEntity.class).stream())) {
            JSONEncoder jsonEncoder = new JSONEncoder();

            jsonEncoder.write(streamAdapter, getResponse().getOutputStream());
        }
    }

    @RequestMethod("GET")
    @ResourcePath("stream-hibernate-jackson")
    public void streamEmployeesHibernateJackson() throws IOException {
        getResponse().setContentType("application/json");

        try (Session session = sessionFactory.openSession();
            Stream<EmployeeEntity> stream = session.createQuery(HQL_QUERY, EmployeeEntity.class).stream()) {
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            ObjectWriter objectWriter = objectMapper.writer();

            try (SequenceWriter sequenceWriter = objectWriter.writeValues(getResponse().getOutputStream())) {
                sequenceWriter.init(true);

                stream.forEach(employee -> {
                    try {
                        sequenceWriter.write(employee);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

}
