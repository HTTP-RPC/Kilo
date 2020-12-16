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
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

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

    private static final String SQL_QUERY = QueryBuilder.select("emp_no AS employeeNumber",
        "first_name AS firstName",
        "last_name AS lastName",
        "gender",
        "birth_date AS birthDate",
        "hire_date AS hireDate").from("employees").toString();

    private static final String HQL_QUERY = QueryBuilder.select("e").from("EmployeeEntity e").toString();

    private static final int FETCH_SIZE = 2048;

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

    @RequestMethod("GET")
    @ResourcePath("sql")
    public Iterable<Employee> getEmployees() throws SQLException {
        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY))) {
            resultSetAdapter.setFetchSize(FETCH_SIZE);

            return new StreamAdapter<>(resultSetAdapter.stream().map(result -> BeanAdapter.adapt(result, Employee.class)));
        }
    }

    @RequestMethod("GET")
    @ResourcePath("hql")
    public List<EmployeeEntity> getEmployeesHibernate() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(HQL_QUERY, EmployeeEntity.class).list();
        }
    }

    @RequestMethod("GET")
    @ResourcePath("sql-stream")
    public void streamEmployees() throws SQLException, IOException {
        getResponse().setContentType("application/json");

        JSONEncoder jsonEncoder = new JSONEncoder();

        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(SQL_QUERY))) {
            resultSetAdapter.setFetchSize(FETCH_SIZE);

            jsonEncoder.write(resultSetAdapter, getResponse().getOutputStream());
        }
    }

    @RequestMethod("GET")
    @ResourcePath("hql-stream")
    public void streamEmployeesHibernate() throws IOException {
        getResponse().setContentType("application/json");

        JSONEncoder jsonEncoder = new JSONEncoder();

        try (Session session = sessionFactory.openSession();
            StreamAdapter<EmployeeEntity> streamAdapter = new StreamAdapter<>(session.createQuery(HQL_QUERY, EmployeeEntity.class).stream())) {
            jsonEncoder.write(streamAdapter, getResponse().getOutputStream());
        }
    }

    @RequestMethod("GET")
    @ResourcePath("hql-stream-jackson")
    public void streamEmployeesHibernateJackson() throws IOException {
        getResponse().setContentType("application/json");

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        ObjectWriter objectWriter = objectMapper.writer();

        try (Session session = sessionFactory.openSession();
            Stream<EmployeeEntity> stream = session.createQuery(HQL_QUERY, EmployeeEntity.class).stream();
            SequenceWriter sequenceWriter = objectWriter.writeValues(getResponse().getOutputStream())) {
            sequenceWriter.init(true);

            stream.forEach(employeeEntity -> {
                try {
                    sequenceWriter.write(employeeEntity);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }
}
