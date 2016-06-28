# Introduction
HTTP-RPC is a framework for simplifying development of REST-based applications. It allows developers to publish and interact with HTTP-based web services using a convenient, RPC-like interface while preserving fundamental REST concepts such as statelessness and uniform resource access.

The project currently includes support for implementing REST services in Java and consuming services in Java, Objective-C/Swift, or JavaScript. The server library provides a lightweight alternative to other, larger Java-based REST frameworks, and the consistent cross-platform client API makes it easy to interact with services regardless of target device or operating system. 

# Service Overview
HTTP-RPC services are accessed by applying an HTTP verb such as GET or POST to a target resource. The target is specified by a path representing the name of the resource, and is generally expressed as a noun such as _/calendar_ or _/contacts_.

Arguments are passed either via the query string or in the request body, like an HTML form. Results are typically returned as JSON, although operations that do not return a value are also supported.

## GET
The `GET` method is used to retrive information from the server. For example, the following request might be used to obtain data about a calendar event:

    GET /calendar?eventID=101

This request might retrieve the sum of two numbers, whose values are specified by the `a` and `b` arguments:

    GET /math/sum?a=2&b=4

Alternatively, the values could be specified as a list rather than as two fixed variables:

    GET /math/sum?values=1&values=2&values=3

## POST
The `POST` method is typically used to add new information to the server. For example, the following request might be used to create a new calendar event:

    POST /calendar

As with HTML forms, if the `POST` arguments contain only text values, they can be encoded using the "application/x-www-form-urlencoded" MIME type:

    title=Planning+Meeting&start=2016-06-28T14:00&end=2016-06-28T15:00

If the arguments contain binary data such as a JPEG or PNG image, the "multipart/form-data" encoding can be used.

## PUT
The `PUT` method updates existing information on the server. For example, the following request might be used to modify the end date of a calendar event:

    PUT /calendar?eventID=102&end=2016-06-28T15:30

## DELETE
The `DELETE` method removes information from the server. For example, this request might be used to delete a calendar event:

    DELETE /calendar?eventID=102

## Response Codes
Although the HTTP specification defines a large number of possible response codes, only a few are applicable to HTTP-RPC services:

* _200 OK_ - The request succeeded, and the response contains a JSON value representing the result
* _204 No Content_ - The request succeeded, but did not produce a result
* _404 Not Found_ - The requested resource does not exist
* _405 Method Not Allowed_ - The requested resource exists, but does not support the requested HTTP method
* _500 Internal Server Error_ - An error occurred while executing the method

# Implementations
Support currently exists for implementing HTTP-RPC services in Java, and consuming services in Java, Objective-C/Swift, or JavaScript. For examples and additional information, please see the [wiki](https://github.com/gk-brown/HTTP-RPC/wiki).

I am always looking for ways to improve HTTP-RPC. If you have any suggestions, please [let me know](mailto:gk_brown@verizon.net?subject=HTTP-RPC).

## Java Server
The Java server implementation of HTTP-RPC allows developers to create and publish HTTP-RPC web services in Java. It is distributed as a JAR file that contains the following classes:

* _`org.httprpc`_
    * `WebService` - abstract base class for HTTP-RPC services
    * `RPC` - annotation that specifies a remote procedure call
    * `RequestDispatcherServlet` - servlet that dispatches requests to service instances
* _`org.httprpc.beans`_
    * `BeanAdapter` - wrapper class that presents the contents of a Java Bean instance as a map, suitable for serialization to JSON
* _`org.httprpc.sql`_
    * `ResultSetAdapter` - wrapper class that presents the contents of a JDBC result set as an iterable list, suitable for streaming to JSON
    * `Parameters` - class for simplifying execution of prepared statements
* _`org.httprpc.util`_
    * `IteratorAdapter` - wrapper class that presents the contents of an iterator as an iterable list, suitable for streaming to JSON

Each of these classes is discussed in more detail below. 

The JAR file for the Java server implementation of HTTP-RPC can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 8 and a servlet container supporting servlet specification 3.1 (e.g. Tomcat 8) or later are required.

### WebService Class
`WebService` is an abstract base class for HTTP-RPC web services. All services must extend this class and must provide a public, zero-argument constructor.

Service operations are defined by adding public methods to a concrete service implementation. All public methods annotated with the `@RPC` annotation automatically become available for remote execution when the service is published.

For example, the following class might be used to implement the _sum_ operations discussed earlier:

    public class MathService extends WebService {
        @RPC(method="GET", path="sum")
        public double getSum(double a, double b) {
            return a + b;
        }
    
        @RPC(method="GET", path="sum")
        public double getSum(List<Double> values) {
            double total = 0;
    
            for (double value : values) {
                total += value;
            }
    
            return total;
        }
    }
    
Note that both methods are mapped to the _sum_ path. The `RequestDispatcherServlet` class discussed in the next section will select the best method to execute based on the provided argument values. For example, the following request would cause the first method to be invoked, returning a result of 6:

    GET /math/sum?a=2&b=4
    
This request would invoke the second method, also returning 6:

    GET /math/sum?values=1&values=2&values=3

Method arguments may be any of the following types:

* `byte`/`java.lang.Byte`
* `short`/`java.lang.Short`
* `int`/`java.lang.Integer`
* `long`/`java.lang.Long`
* `float`/`java.lang.Float`
* `double`/`java.lang.Double`
* `boolean`/`java.lang.Boolean`
* `java.lang.String`
* `java.net.URL`
* `java.util.List`

`URL` arguments represent binary content provided by the caller and can only be used with `POST` requests. List arguments may be used with any request type, but list elements must be a supported simple type; e.g. `List<Double>`.

If the value of a primitive parameter is not provided, a value of 0 will be passed to the method for that argument. Omitting the value of a wrapper or other simple (non-list) type produces a null argument value for that parameter. Omitting all values for a list parameter produces an empty list argument for the parameter.

Methods may return any of the following types, `void`, or `java.lang.Void`:

* `byte`/`java.lang.Byte`
* `short`/`java.lang.Short`
* `int`/`java.lang.Integer`
* `long`/`java.lang.Long`
* `float`/`java.lang.Float`
* `double`/`java.lang.Double`
* `boolean`/`java.lang.Boolean`
* `java.lang.String`
* `java.util.List`
* `java.util.Map` 

`Map` implementations must use `String` values for keys. Nested structures are supported, but reference cycles are not permitted.

`List` and `Map` types are not required to support random access; iterability is sufficient. Additionally, `List` and `Map` types that implement `java.lang.AutoCloseable` will be automatically closed after their values have been written to the output stream. This allows service implementations to stream response data rather than buffering it in memory before it is written. 

For example, the `org.httprpc.sql.ResultSetAdapter` class wraps an instance of `java.sql.ResultSet` and exposes its contents as a forward-scrolling, auto-closeable list of map values. Closing the list also closes the underlying result set, ensuring that database resources are not leaked. `ResultSetAdapter` is discussed in more detail later.

#### Request Metadata
`WebService` provides the following methods that allow an extending class to obtain additional information about the current request:

* `getLocale()` - returns the locale associated with the current request
* `getUserName()` - returns the user name associated with the current request, or `null` if the request was not authenticated
* `getUserRoles()` - returns a set representing the roles the user belongs to, or `null` if the request was not authenticated

The values returned by these methods are populated via protected setters, which are called once per request by `RequestDispatcherServlet`. These setters are not meant to be called by application code. However, they can be used to facilitate unit testing of service implementations by simulating a request from an actual client. 

### RequestDispatcherServlet Class
HTTP-RPC services are published via the `RequestDispatcherServlet` class. This class is resposible for translating HTTP request parameters to method arguments, invoking the specified method, and serializing the return value to JSON. Note that service classes must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

Java objects are mapped to their JSON equivalents as follows:

* `java.lang.Number` or numeric primitive: number
* `java.lang.Boolean` or boolean primitive: true/false
* `java.lang.String`: string
* `java.util.List`: array
* `java.util.Map`: object

Each servlet instance hosts a single HTTP-RPC service. The name of the service type is passed to the servlet via the "serviceClassName" initialization parameter. For example:

	<servlet>
	    <servlet-name>MathServlet</servlet-name>
	    <servlet-class>org.httprpc.RequestDispatcherServlet</servlet-class>
        <init-param>
            <param-name>serviceClassName</param-name>
            <param-value>com.example.MathService</param-value>
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>MathServlet</servlet-name>
        <url-pattern>/math/*</url-pattern>
    </servlet-mapping>

A new service instance is created and initialized for each request. `RequestDispatcherServlet` converts the request parameters to the argument types expected by the named method, invokes the method, and writes the return value to the response stream as JSON.

If the method completes successfully and returns a value, an HTTP 200 status is returned. If the method returns `void` or `Void`, HTTP 204 is returned.

If the requested resource does not exist, the servlet returns an HTTP 404 status. If the resource exists but does not support the requested method, HTTP 405 is returned. 

If any exception is thrown while executing the method, HTTP 500 is returned.

Servlet security is provided by the underlying servlet container. See the Java EE documentation for more information.

### BeanAdapter Class
The `BeanAdapter` class allows the contents of a Java Bean object to be returned from a service method. This class implements the `Map` interface and exposes any properties defined by the Bean as entries in the map, allowing custom data types to be serialized to JSON.

For example, the statistical data discussed in the previous section might be represented by the following Bean class:

    public class Statistics {
        private int count = 0;
        private double sum = 0;
        private double average = 0;
    
        public int getCount() {
            return count;
        }
    
        public void setCount(int count) {
            this.count = count;
        }
    
        public double getSum() {
            return sum;
        }
    
        public void setSum(double sum) {
            this.sum = sum;
        }
    
        public double getAverage() {
            return average;
        }
    
        public void setAverage(double average) {
            this.average = average;
        }
    }

Using this class, an implementation of the `getStatistics()` method might look like this:

    public Map<String, Object> getStatistics(List<Double> values) {    
        Statistics statistics = new Statistics();

        int n = values.size();

        statistics.setCount(n);

        for (int i = 0; i < n; i++) {
            statistics.setSum(statistics.getSum() + values.get(i));
        }

        statistics.setAverage(statistics.getSum() / n);

        return new BeanAdapter(statistics);
    }

Although the values are actually stored in the strongly typed `Statistics` object, the adapter makes the data appear as a map, allowing it to be returned to the caller as a JSON object.

Note that, if a property returns a nested Bean type, the property's value will be automatically wrapped in a `BeanAdapter` instance. Additionally, if a property returns a `List` or `Map` type, the value will be wrapped in an adapter of the appropriate type that automatically adapts its sub-elements. 

For example, the `getTree()` method discussed earlier could be implemented using `BeanAdapter` as follows:

    @Template("tree.html")
    public Map<String, Object> getTree() {
        TreeNode root = new TreeNode();
        ...

        return new BeanAdapter(root);
    }

The `TreeNode` instances returned by the `getChildren()` method will be recursively adapted:

    public class TreeNode {
        public String getName() { ... }    
        public List<TreeNode> getChildren() { ... }
    }

The `BeanAdapter#adapt()` method is used to adapt property values. This method is called internally by `BeanAdapter#get()`, but it can also be used to explicitly adapt list or map values as needed. See the Javadoc for the `BeanAdapter` class for more information.

### ResultSetAdapter Class
The `ResultSetAdapter` class allows the result of a SQL query to be efficiently returned from a service method. This class implements the `List` interface and makes each row in a JDBC result set appear as an instance of `Map`, rendering the data suitable for serialization to JSON. It also implements the `AutoCloseable` interface, to ensure that the underlying result set is closed and database resources are not leaked.

`ResultSetAdapter` is forward-scrolling only; its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a result set to be returned directly to the caller without any intermediate buffering. The caller can simply execute a JDBC query, pass the resulting result set to the `ResultSetAdapter` constructor, and return the adapter instance:

    public List<Map<String, Object>> getData() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from some_table");
        
        return new ResultSetAdapter(resultSet);
    }

### Parameters Class
The `Parameters` class provides a means for executing prepared statements using named parameter values rather than indexed arguments. Parameter names are specified by a leading `:` character. For example:

    SELECT * FROM some_table 
    WHERE column_a = :a OR column_b = :b OR column_c = COALESCE(:c, 4.0)
    
The `parse()` method is used to create a `Parameters` instance from a SQL statement. It takes a `java.io.Reader` containing the SQL text as an argument; for example:

    Parameters parameters = Parameters.parse(new StringReader(sql));

The `getSQL()` method of the `Parameters` class returns the parsed SQL in standard JDBC syntax:

    SELECT * FROM some_table 
    WHERE column_a = ? OR column_b = ? OR column_c = COALESCE(?, 4.0)

This value is used to create the actual prepared statement:

    PreparedStatement statement = DriverManager.getConnection(url).prepareStatement(parameters.getSQL());

Parameter values are applied to the statement using the `apply()` method. The first argument to this method is the prepared statement, and the second is a map containing the statement arguments:

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("a", "hello");
    arguments.put("b", 3);
    
    parameters.apply(statement, arguments);

Since explicit creation and population of the argument map can be cumbersome, the `WebService` class provides the following static convenience methods to help simplify map creation:

    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) { ... }
    public static <K> Map.Entry<K, ?> entry(K key, Object value) { ... }
    
Using the convenience methods, the code that applies the parameter values can be reduced to the following:

    parameters.apply(statement, mapOf(entry("a", "hello"), entry("b", 3)));

Once applied, the statement can be executed:

    return new ResultSetAdapter(statement.executeQuery());    

### IteratorAdapter Class
The `IteratorAdapter` class allows the content of an arbitrary cursor to be efficiently returned from a service method. This class implements the `List` interface and adapts each element produced by the iterator for serialization to JSON, including nested `List` and `Map` structures. Like `ResultSetAdapter`, `IteratorAdapter` implements the `AutoCloseable` interface. If the underlying iterator type also implements `AutoCloseable`, `IteratorAdapter` will ensure that the underlying cursor is closed so that resources are not leaked.

As with `ResultSetAdapter`, `IteratorAdapter` is forward-scrolling only, so its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a cursor to be returned directly to the caller without any intermediate buffering.

`IteratorAdapter` is typically used to serialize result data produced by NoSQL databases.

## Java Client
TODO

# Objective-C/Swift Client
TODO

# JavaScript Client
TODO

# More Information
For additional information and examples, see the [the wiki](https://github.com/gk-brown/HTTP-RPC/wiki).
