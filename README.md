# Introduction
HTTP-RPC is an open-source framework for simplifying development of REST-based applications. It allows developers to create and access HTTP-based web services using a convenient, RPC-like metaphor while preserving fundamental REST principles such as statelessness and uniform resource access.

The project currently includes support for implementing REST services in Java and consuming services in Java, Objective-C/Swift, or JavaScript. The server component provides a lightweight alternative to other, larger Java-based REST frameworks, and the consistent cross-platform client API makes it easy to interact with services regardless of target device or operating system. 

# Service Operations
HTTP-RPC services are accessed by applying an HTTP verb such as `GET` or `POST` to a target resource. The target is specified by a path representing the name of the resource, and is generally expressed as a noun such as _/calendar_ or _/contacts_.

Arguments are provided either via the query string or in the request body, like an HTML form. Results are generally returned as JSON, although operations that do not return a value are also supported.

## GET
The `GET` method is used to retrive information from the server. `GET` arguments are passed in the query string. For example, the following request might be used to obtain data about a calendar event:

    GET /calendar?eventID=101

This request might retrieve the sum of two numbers, whose values are specified by the `a` and `b` query arguments:

    GET /math/sum?a=2&b=4

Alternatively, the argument values could be specified as a list rather than as two fixed variables:

    GET /math/sum?values=1&values=2&values=3
    
In either case, the service would return the value 6 in response.

## POST
The `POST` method is typically used to add new information to the server. For example, the following request might be used to create a new calendar event:

    POST /calendar

As with HTML forms, `POST` arguments are passed in the request body. If the arguments contain only text values, they can be encoded using the "application/x-www-form-urlencoded" MIME type:

    title=Planning+Meeting&start=2016-06-28T14:00&end=2016-06-28T15:00

If the arguments contain binary data such as a JPEG or PNG image, the "multipart/form-data" encoding can be used.

While it is not required, `POST` requests that create resources often return a value that can be used to identify the resource for later retrieval, update, or removal.

## PUT
The `PUT` method updates existing information on the server. `PUT` arguments are passed in the query string. For example, the following request might be used to modify the end date of a calendar event:

    PUT /calendar?eventID=102&end=2016-06-28T15:30

`PUT` requests generally do not return a value.

## DELETE
The `DELETE` method removes information from the server. `DELETE` arguments are passed in the query string. For example, this request might be used to delete a calendar event:

    DELETE /calendar?eventID=102

`DELETE` requests generally do not return a value.

## Response Codes
Although the HTTP specification defines a large number of possible response codes, only a few are applicable to HTTP-RPC services:

* _200 OK_ - The request succeeded, and the response contains a JSON value representing the result
* _204 No Content_ - The request succeeded, but did not produce a result
* _404 Not Found_ - The requested resource does not exist
* _405 Method Not Allowed_ - The requested resource exists, but does not support the requested HTTP method
* _500 Internal Server Error_ - An error occurred while executing the method

# Implementations
Support currently exists for implementing HTTP-RPC services in Java, and consuming services in Java, Objective-C/Swift, or JavaScript. For examples and additional information, please see the [wiki](https://github.com/gk-brown/HTTP-RPC/wiki).

## Java Server
The Java server library allows developers to create and publish HTTP-RPC web services in Java. It is distributed as a JAR file that contains the following core classes:

* _`org.httprpc`_
    * `WebService` - abstract base class for HTTP-RPC services
    * `RPC` - annotation that specifies a "remote procedure call", or service method
    * `RequestDispatcherServlet` - servlet that dispatches requests to service instances
* _`org.httprpc.beans`_
    * `BeanAdapter` - adapter class that presents the contents of a Java Bean instance as a map, suitable for serialization to JSON
* _`org.httprpc.sql`_
    * `ResultSetAdapter` - adapter class that presents the contents of a JDBC result set as an iterable list, suitable for streaming to JSON
    * `Parameters` - class for simplifying execution of prepared statements
* _`org.httprpc.util`_
    * `IteratorAdapter` - adapter class that presents the contents of an iterator as an iterable list, suitable for streaming to JSON

Additionally, the JAR file contains the following classes for working with templates, which allow service data to be declaratively transformed into alternate representations:

* _`org.httprpc`_
    * `Template` - annotation that associates a template with a service method
* _`org.httprpc.template`_
    * `TemplateEngine` - template processing engine
    * `Modifier` - interface representing a template modifier
    * `MarkupEscapeModifier` - modifier that escapes markup data
    * `JSONEscapeModifier` - modifier that escapes JSON data
    * `CSVEscapeModifier` - modifier that escapes CSV data
    * `URLEscapeModifer` - modifier that escapes URL values
    * `ResourceBundleAdapter` - adapter class that presents the contents of a root object and a resource bundle as a single map, suitable for use as a data dictionary

Each of these classes is discussed in more detail below. 

The JAR file for the Java server implementation of HTTP-RPC can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 8 and a servlet container supporting servlet specification 3.1 (e.g. Tomcat 8) or later are required.

### WebService Class
`WebService` is an abstract base class for HTTP-RPC web services. All services must extend this class and must provide a public, zero-argument constructor.

Service operations are defined by adding public methods to a concrete service implementation. The `@RPC` annotation is used to flag a method as remotely accessible. This annotation associates an HTTP verb and a resource path with the method. All public annotated methods automatically become available for remote execution when the service is published.

For example, the following class might be used to implement the simple addition operations discussed in the previous section:

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
    
Note that both methods are mapped to the path _/math/sum_. The `RequestDispatcherServlet` class discussed in the next section selects the best method to execute based on the names of the provided argument values. For example, the following request would cause the first method to be invoked:

    GET /math/sum?a=2&b=4
    
This request would invoke the second method:

    GET /math/sum?values=1&values=2&values=3

#### Method Arguments
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

`URL` arguments represent binary content provided by the caller and can only be used with `POST` requests submitted using the "multipart/form-data" encoding. List arguments may be used with any request type, but list elements must be a supported simple type; e.g. `List<Double>`.

Omitting the value of a primitive parameter results in an argument value of 0 for that parameter. Omitting the value of a simple reference type produces a null argument value for that parameter. Omitting all values for a list parameter produces an empty list argument for the parameter.

#### Return Values
Methods may return any of the following types:

* `byte`/`java.lang.Byte`
* `short`/`java.lang.Short`
* `int`/`java.lang.Integer`
* `long`/`java.lang.Long`
* `float`/`java.lang.Float`
* `double`/`java.lang.Double`
* `boolean`/`java.lang.Boolean`
* `java.lang.CharSequence`
* `java.util.List`
* `java.util.Map` 

Methods may also return `void` or `java.lang.Void` to indicate that they do not return a value.

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
* `java.lang.CharSequence`: string
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

If the method completes successfully and returns a value, an HTTP 200 status code is returned. If the method returns `void` or `Void`, HTTP 204 is returned.

If the requested resource does not exist, the servlet returns an HTTP 404 status code. If the resource exists but does not support the requested method, HTTP 405 is returned. 

If any exception is thrown while executing the method, HTTP 500 is returned.

Servlet security is provided by the underlying servlet container. See the Java EE documentation for more information.

### BeanAdapter Class
The `BeanAdapter` class allows the contents of a Java Bean object to be returned from a service method. This class implements the `Map` interface and exposes any properties defined by the Bean as entries in the map, allowing custom data types to be serialized to JSON.

For example, the following Bean class might be used to represent basic statistical data about a collection of values:

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

Using this class, an implementation of a `getStatistics()` method might look like this:

    @RPC(method="GET", path="statistics")
    public Map<String, ?> getStatistics(List<Double> values) {    
        Statistics statistics = new Statistics();

        int n = values.size();

        statistics.setCount(n);

        for (int i = 0; i < n; i++) {
            statistics.setSum(statistics.getSum() + values.get(i));
        }

        statistics.setAverage(statistics.getSum() / n);

        return new BeanAdapter(statistics);
    }

Although the values are actually stored in the strongly typed `Statistics` object, the adapter makes the data appear as a map, allowing it to be returned to the caller as a JSON object; for example:

    {
      "average": 3.0, 
      "count": 3, 
      "sum": 9.0
    }

Note that, if a property returns a nested Bean type, the property's value will be automatically wrapped in a `BeanAdapter` instance. Additionally, if a property returns a `List` or `Map` type, the value will be wrapped in an adapter of the appropriate type that automatically adapts its sub-elements. This allows service methods to return recursive structures such as trees.

### ResultSetAdapter Class
The `ResultSetAdapter` class allows the result of a SQL query to be efficiently returned from a service method. This class implements the `List` interface and makes each row in a JDBC result set appear as an instance of `Map`, rendering the data suitable for serialization to JSON. It also implements the `AutoCloseable` interface, to ensure that the underlying result set is closed and database resources are not leaked.

`ResultSetAdapter` is forward-scrolling only; its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a result set to be returned directly to the caller without any intermediate buffering. The caller can simply execute a JDBC query, pass the resulting result set to the `ResultSetAdapter` constructor, and return the adapter instance:

    @RPC(method="GET", path="data")
    public ResultSetAdapter getData() throws SQLException {
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

### Templates
Although data produced by an HTTP-RPC web service is typically returned to the caller as JSON, it can also be transformed into other representations via "templates". Templates are documents that describe an output format, such as HTML, XML, or CSV. They are merged with result data at execution time to create the final response that is sent back to the caller.

HTTP-RPC templates are based on the [CTemplate](https://github.com/OlafvdSpek/ctemplate) system, which defines a set of "markers" that are replaced with values supplied by a "data dictionary" when the template is processed. The following CTemplate marker types are supported by HTTP-RPC:

* {{_variable_}} - injects a variable from the data dictionary into the output
* {{#_section_}}...{{/_section_}} - defines a repeating section of content
* {{>_include_}} - imports content specified by another template
* {{!_comment_}} - defines a comment

The value returned by a service method represents the data dictionary. Usually, this will be an instance of `java.util.Map` whose keys represent the values supplied by the dictionary. However, it can be an instance of any supported return type. Non-map values are assigned a default key of ".", allowing them to be referred to in the template.

For example, a simple template for transforming the output of the `getStatistics()` method discussed earlier into HTML is shown below:

    <html>
    <head>
        <title>Statistics</title>
    </head>
    <body>
        <p>Count: {{count}}</p>
        <p>Sum: {{sum}}</p>
        <p>Average: {{average}}</p> 
    </body>
    </html>

This method returned a map containing the result of some simple statistical calculations:

    {
      "average": 3.0, 
      "count": 3, 
      "sum": 9.0
    }

At execution time, the "count", "sum", and "average" variable markers will be replaced by their corresponding values from the data dictionary:

    <html>
    <head>
        <title>Statistics</title>
    </head>
    <body>
        <p>Count: 3.0</p>
        <p>Sum: 9.0</p>
        <p>Average: 3.0</p> 
    </body>
    </html>


#### @Template Annotation
The `Template` annotation is used to associate a template document with a method. The annotation's value represents the name and type of the template that will be applied to the results. For example:

    @Template(name="statistics.html", mimeType="text/html")
    public Map<String, ?> getStatistics(List<Double> values) { ... }

The `name` value refers to the file containing the template definition. It is specified as a resource path relative to the service type.

The `mimeType` value indicates type of the content produced by the named template. It is used by `RequestDispatcherServlet` to identify the requested template. A specific representation is requested by appending a file extension associated with the desired MIME type to the service name in the URL. 

Note that it is possible to associate multiple templates with a single service method. For example, the following code adds an additional XML template document to the `getStatistics()` method:

    @Template(name="statistics.html", mimeType="text/html")
    @Template(name="statistics.xml", mimeType="application/xml")
    public Map<String, ?> getStatistics(List<Double> values) { ... }

#### TemplateEngine Class
TODO

#### Variable Markers
Variable markers can be used to refer to any simple dictionary value (i.e. number, boolean, or character sequence). Missing (i.e. `null`) values are replaced with the empty string in the generated output. Nested variables can be referred to using dot-separated path notation; e.g. "name.first".

Variable names beginning with an `@` character are considered resource references. Resources allow static template content to be localized. At execution time, the template processor looks for a resource bundle with the same base name as the service type, using the locale specified by the current HTTP request. If the bundle exists, it is used to provide a localized string value for the variable.

For example, the descriptive text from _statistics.html_ could be extracted into _MathService.properties_ as follows:

    title=Statistics
    count=Count
    sum=Sum
    average=Average

The template could be updated to refer to these values as shown below:

    <html>
    <head>
        <title>{{@title}}</title>
    </head>
    <body>
        <p>{{@count}}: {{count}}</p>
        <p>{{@sum}}: {{sum}}</p>
        <p>{{@average}}: {{average}}</p> 
    </body>
    </html>

When the template is processed, the resource references will be replaced with their corresponding values from the resource bundle.

##### Modifiers
TODO

#### Section Markers
TODO

#### Includes
Include markers import content defined by another template. They can be used to create reusable content modules; for example, document headers and footers.

TODO

#### Comments
Comment markers simply define a block of text that is excluded from the final output. They are generally used to provide informational text to the reader of the source template. For example:

TODO

When the template is processed, only the TODO content will be included in the output.

## Java Client
The Java client library enables Java applications (including Android) to consume HTTP-RPC web services. It is distributed as a JAR file that includes the following types, discussed in more detail below:

* _`org.httprpc`_
    * `WebServiceProxy` - invocation proxy for HTTP-RPC services
    * `ResultHandler` - callback interface for handling results
    * `Result` - abstract base class for typed results
    * `Authentication` - interface for authenticating requests
    * `BasicAuthentication` - authentication implementation supporting basic HTTP authentication

The JAR file for the Java client implementation of HTTP-RPC can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 7 or later is required.

### WebServiceProxy Class
The `WebServiceProxy` class acts as a client-side invocation proxy for HTTP-RPC web services. Internally, it uses an instance of `HttpURLConnection` to send and receive data. `POST` requests are encoded as "multipart/form-data".

`WebServiceProxy` provides a single constructor that takes the following arguments:

* `serverURL` - an instance of `java.net.URL` representing the URL of the server
* `executorService` - an instance of `java.util.concurrent.ExecutorService` that will be used to execute service requests

The executor service is used to schedule service requests. Internally, requests are implemented as a `Callable` that is submitted to the service. See the `ExecutorService` Javadoc for more information.

Service operations are initiated by calling the `invoke()` method:
    
    public <V> Future<V> invoke(String method, String path, 
        Map<String, ?> arguments,  
        ResultHandler<V> resultHandler) { ... }

This method takes the following arguments:

* `method` - the HTTP method to execute
* `path` - the resource path
* `arguments` - a map containing the request arguments as key/value pairs
* `resultHandler` - an instance of `org.httprpc.ResultHandler` that will be invoked upon completion of the service operation

A convenience method is also provided for executing operations that don't take any arguments:

    public <V> Future<V> invoke(String method, String path, 
        ResultHandler<V> resultHandler) { ... }

Request arguments may be any of the following types:

* `java.lang.Number`
* `java.lang.Boolean`
* `java.lang.String`
* `java.net.URL`
* `java.util.List`

URL arguments represent binary content and can only be used with `POST` requests. List arguments may be used with any request type, but list elements must be a supported simple type; e.g. `List<Double>`.

The result handler is called upon completion of the operation. `ResultHandler` is a functional interface whose single method, `execute()`, is defined as follows:

    public void execute(V result, Exception exception);

On successful completion, the first argument will contain the result of the operation. It will be an instance of one of the following types or `null`, depending on the response returned by the server:

* string: `java.lang.String`
* number: `java.lang.Number`
* true/false: `java.lang.Boolean`
* array: `java.util.List`
* object: `java.util.Map`

The second argument will always be `null` in this case. If an error occurs, the first argument will be `null` and the second will contain an exception representing the error that occurred.

Both variants of the `invoke()` method return an instance of `java.util.concurrent.Future` representing the invocation request. This object allows a caller to cancel an outstanding request as well as obtain information about a request that has completed.

#### Argument Map Creation
Since explicit creation and population of the argument map can be cumbersome, `WebServiceProxy` provides the following static convenience methods to help simplify map creation:

    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) { ... }
    public static <K> Map.Entry<K, ?> entry(K key, Object value) { ... }
    
Using these convenience methods, argument map creation can be reduced from this:

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("a", 2);
    arguments.put("b", 4);
    
to this:

    Map<String, Object> arguments = mapOf(entry("a", 2), entry("b", 4));
    
A complete example using `WebServiceProxy#invoke()` is provided later.

#### Multi-Threading Considerations
By default, a result handler is called on the thread that executed the remote request, which in most cases will be a background thread. However, user interface toolkits generally require updates to be performed on the main thread. As a result, handlers typically need to "post" a message back to the UI thread in order to update the application's state. For example, a Swing application might call `SwingUtilities#invokeAndWait()`, whereas an Android application might call `Activity#runOnUiThread()` or `Handler#post()`.

While this can be done in the result handler itself, `WebServiceProxy` provides a more convenient alternative. The `setResultDispatcher()` method allows an application to specify an instance of `java.util.concurrent.Executor` that will be used to perform all result handler notifications. This is a static method that only needs to be called once at application startup.

For example, the following Android-specific code ensures that all result handlers will be executed on the main UI thread:

    WebServiceProxy.setResultDispatcher(new Executor() {
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    });

Similar dispatchers can be configured for other Java UI toolkits such as Swing, JavaFX, and SWT. Command line applications can generally use the default dispatcher, which simply performs result handler notifications on the current thread.

### Result Class
`Result` is an abstract base class for typed results. Using this class, applications can easily map untyped object data returned by a service operation to typed values. It provides the following constructor that is used to populate Java Bean property values from map entries:

    public Result(Map<String, Object> properties) { ... }
    
For example, the following Java class might be used to provide a typed version of the statistical data returned by the `getStatistics()` method discussed earlier:

    public class Statistics extends Result {
        private int count;
        private double sum;
        private double average;

        public Statistics(Map<String, Object> properties) {
            super(properties);
        }

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

The map data returned by `getStatistics()` can be converted to a `Statistics` instance as follows:

    serviceProxy.invoke("getStatistics", (Map<String, Object> result, Exception exception) -> {
        Statistics statistics = new Statistics(result);

        // Prints 3, 9.0, and 3.0
        System.out.println(statistics.getCount());
        System.out.println(statistics.getSum());
        System.out.println(statistics.getAverage());
    });

### Authentication
Although it is possible to use the `java.net.Authenticator` class to authenticate service requests, this class can be difficult to work with, especially when dealing with multiple concurrent requests or authenticating to multiple services with different credentials. It also requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `org.httprpc.Authentication` interface defines a single method that is used to authenticate each request submitted by a proxy instance:

    public interface Authentication {
        public void authenticate(HttpURLConnection connection);
    }

Authentication providers are associated with a proxy instance via the `setAuthentication()` method of the `WebServiceProxy` class. For example, the following code associates an instance of `org.httprpc.BasicAuthentication` with a service proxy:

    serviceProxy.setAuthentication(new BasicAuthentication("username", "password"));

The `BasicAuthentication` class is provided by the HTTP-RPC Java client library. Applications may provide custom implementations of the `Authentication` interface to support other authentication schemes.

### Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to access the resources of the hypothetical math service discussed earlier. It first creates an instance of the `WebServiceProxy` class and configures it with a pool of ten threads for executing requests. It then invokes the `getSum(double, double)` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `getSum(List<Double>)` method, passing the values 1, 2, and 3 as arguments:

    // Create service
    URL serverURL = new URL("https://localhost:8443");
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    WebServiceProxy serviceProxy = new WebServiceProxy(serverURL, executorService);

    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("a", 2), entry("b", 4)), new ResultHandler<Number>() {
        @Override
        public void execute(Number result, Exception exception) {
            // result is 6
        }
    });
    
    // Get sum of all values
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("values", listOf(1, 2, 3))), new ResultHandler<Number>() {
        @Override
        public void execute(Number result, Exception exception) {
            // result is 6
        }
    });

Note that, in Java 8 or later, lambda expressions can be used instead of anonymous classes to implement result handlers, reducing the invocation code to the following:

    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("a", 2), entry("b", 4)), (result, exception) -> {
        // result is 6
    });

    // Get sum of all values
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("values", listOf(1, 2, 3))), (result, exception) -> {
        // result is 6
    });

## Objective-C/Swift Client
The Objective-C/Swift client library enables iOS applications to consume HTTP-RPC services. It is delivered as a modular framework that includes the following types, discussed in more detail below:

* `WSWebServiceProxy` - invocation proxy for HTTP-RPC services
* `WSAuthentication` - interface for authenticating requests
* `WSBasicAuthentication` - authentication implementation supporting basic HTTP authentication

The framework for the Objective-C/Swift client can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). It is also available via [CocoaPods](https://cocoapods.org/pods/HTTP-RPC). iOS 8 or later is required.

### WSWebServiceProxy Class
The `WSWebServiceProxy` class serves as an invocation proxy for HTTP-RPC services. Internally, it uses an instance of `NSURLSession` to issue HTTP requests. `POST` requests are encoded as "multipart/form-data". `NSJSONSerialization` is used to deserialize response content.

Service proxies are initialized via the `initWithSession:serverURL:` method, which takes an `NSURLSession` instance and the URL of the server as arguments. Service operations are executed by calling the `invoke:path:arguments:resultHandler:` method:

    - (NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
        arguments:(NSDictionary<NSString *, id> *)arguments
        resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

This method takes the following arguments:

* `method` - the HTTP method to execute
* `path` - the resource path
* `arguments` - a dictionary containing the request arguments as key/value pairs
* `resultHandler` - a callback that will be invoked upon completion of the method

A convenience method is also provided for executing operations that don't take any arguments:

    - (NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
        resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

Arguments may be any of the following types:

* `NSNumber`
* `NSString`
* `NSURL`
* `NSArray`

`CFBooleanRef` is also supported for boolean arguments. URL arguments represent binary content and can only be used with `POST` requests. Array arguments may be used with any request type, but array elements must be a supported simple type.

The result handler callback is called upon completion of the operation. The callback takes two arguments: a result object and an error object. If the operation completes successfully, the first argument first argument will contain the result of the operation. If the operation fails, the second argument will be populated with an instance of `NSError` describing the error that occurred.

Both variants of the `invoke` method return an instance of `NSURLSessionDataTask` representing the invocation request. This allows an application to cancel a task, if necessary.

Although requests are typically processed on a background thread, result handlers are called on the same operation queue that initially invoked the service method. This is typically the application's main queue, which allows result handlers to update the application's user interface directly, rather than posting a separate update operation to the main queue.

### Authentication
Although it is possible to use the `URLSession:task:didReceiveChallenge:completionHandler:` method of the `NSURLSessionDataDelegate` protocol to authenticate service requests, this method requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `WSAuthentication` protocol defines a single method that is used to authenticate each request submitted by a proxy instance:

    - (void)authenticate:(NSMutableURLRequest *)request;

Authentication providers are associated with a proxy instance via the `authentication` property of the `WSWebServiceProxy` class. For example, the following code associates an instance of `WSBasicAuthentication` with a service proxy:

    serviceProxy.authentication = WSBasicAuthentication(username: "username", password: "password")

The `WSBasicAuthentication` class is provided by the HTTP-RPC framework. Applications may provide custom implementations of the `WSAuthentication` protocol to support other authentication schemes.

### Examples
The following code snippet demonstrates how `WSWebServiceProxy` can be used to invoke the methods of the hypothetical math service. It first creates an instance of the `WSWebServiceProxy` class backed by a default URL session and a delegate queue supporting ten concurrent operations. It then invokes the `getSum(double, double)` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `getSum(List<Double>)` method, passing the values 1, 2, and 3 as arguments:

    // Configure session
    let configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
    configuration.requestCachePolicy = NSURLRequestCachePolicy.ReloadIgnoringLocalAndRemoteCacheData

    let delegateQueue = NSOperationQueue()
    delegateQueue.maxConcurrentOperationCount = 10

    let session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

    // Initialize service proxy and invoke methods
    let serverURL = NSURL(string: "https://localhost:8443")

    let serviceProxy = WSWebServiceProxy(session: session, serverURL: serverURL!)
    
    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", path: "/math/sum", arguments: ["a": 2, "b": 4]) {(result, error) in
        // result is 6
    }

    // Get sum of all values
    serviceProxy.invoke("GET", path: "/math/sum", arguments: ["values": [1, 2, 3]]) {(result, error) in
        // result is 6
    }

## JavaScript Client
The JavaScript HTTP-RPC client enables browser-based applications to consume HTTP-RPC services. It is delivered as JavaScript file that defines a single `WebServiceProxy` class, which is discussed in more detail below. 

The source code for the JavaScript client can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases).

### WebServiceProxy Class
The `WebServiceProxy` class serves as an invocation proxy for HTTP-RPC services. Internally, it uses an instance of `XMLHttpRequest` to communicate with the server, and uses `JSON.parse()` to convert the response to an object. `POST` requests are encoded using the "application/x-www-form-urlencoded" MIME type.

Service proxies are initialized via the `WebServiceProxy` constructor. Service operations are executed by calling the `invoke()` method on the service proxy. Request arguments can be numbers, booleans, strings, or arrays of any simple type.

The `invoke()` method takes a result handler function as the final argument. This callback is invoked upon completion of the operation. The callback takes two arguments: a result object and an error object. If the remote method completes successfully, the first argument contains the value returned by the method. If the method call fails, the second argument will contain the HTTP status code corresponding to the error that occurred.

Both methods return the `XMLHttpRequest` instance used to execute the remote call. This allows an application to cancel a request, if necessary.

### Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to invoke the methods of the hypothetical math service. It first creates an instance of the `WebServiceProxy` class, and then invokes the `getSum(double, double)` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `getSum(List<Double>)` method, passing the values 1, 2, and 3 as arguments:

    // Create service proxy
    var serviceProxy = new WebServiceProxy();

    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", "/math/sum", {a:4, b:2}, function(result, error) {
        // result is 6
    });

    // Get sum of all values
    serviceProxy.invoke("GET", "/math/sum", {values:[1, 2, 3, 4]}, function(result, error) {
        // result is 6
    });

# More Information
For additional information and examples, see the [the wiki](https://github.com/gk-brown/HTTP-RPC/wiki).
