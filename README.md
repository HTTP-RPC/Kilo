# Overview
HTTP-RPC is a mechanism for executing remote procedure calls via HTTP. It combines the flexibility of SOAP with the simplicity of REST, allowing callers to invoke arbitrary operations on a remote endpoint using human-readable URLs and JSON rather than complex XML messages and descriptors. Any platform capable of submitting an HTTP request and consuming a JSON response can be an HTTP-RPC client, and any platform that can respond to an HTTP request and produce a JSON result can act as an HTTP-RPC server.

In HTTP-RPC, remote procedures, or "methods", are encapsulated by a "service", which is simply a collection of related operations. The service and method names are specified in the path component of the request URL. Method arguments are passed either via the query string or in the request body, like an HTML form. Method results are typically returned as JSON values, although methods that do not return a value are also supported.

For example, a GET request for the following URL might invoke the "add" method of a hypothetical "math" service:

    http://example.com/rpc/math/add?a=1&b=2
    
The values 1 and 2 are passed as the "a" and "b" arguments to the method, respectively, with the service returning the value 3 in response. Alternatively, the same result could be obtained by submitting a POST request with a content type of `application/x-www-form-urlencoded` and a request body of `a=1&b=2`. No other HTTP operations are supported.

Method parameters may be either scalar (single-value) or vector (multi-value) types. Scalar values may be any simple type including string, number, or boolean (true/false). String values are not quoted; however, as with any HTTP request, values that include reserved characters must be URL-encoded.

Multi-value arguments are specified by providing zero or more scalar values for a given parameter. For example, the add method above could be modified to accept a list of numbers to add rather than two fixed argument values:

    http://example.com/rpc/math/addValues?values=1&values=2&values=3

The order in which parameters are specified does not matter. Omitting a value for a scalar parameter produces a null argument value for that parameter. Omitting all values for a vector parameter produces an empty collection argument for the parameter.

Return values may be any JSON type, including string, number, boolean (true/false), object, array, and null. No content is returned by a method that does not produce a value.

An HTTP 200 is returned on successful completion, and HTTP 500 is returned in the case of an error (i.e. an exception). Note that exceptions are intended to represent unexpected failures, not application-specific errors. No other HTTP status codes are supported.

Services may return a optional JSON descriptor that documents the methods provided by the service. If supported, the descriptor should be of the following form, and should be returned by a GET request for the base service URL:

    [
      {
        "name": "<method name>",
        "description": "<method description>",
        "returns": ("string" | "number" | "boolean" | "array" | "object" | null),
        "parameters": [
          {
            "name": "<parameter name>",
            "description": "<parameter description>",
            "type": ("string" | "number" | "boolean" | "array")
          },
          ...
        ]
      },
      ...
    ]

For example, a descriptor for the hypothetical math service might look something like this:

    [
      {
        "name": "add",
        "description": "Returns the sum of two numbers.",
        "returns": "number",
        "parameters": [
          {
            "name": "a",
            "description": "The first number.",
            "type": "number"
          },
          {
            "name": "b",
            "description": "The second number.",
            "type": "number"
          }
        ]
      },
      {
        "name": "addValues",
        "description": "Returns the sum of a list of values.",
        "returns": "number",
        "parameters": [
          {
            "name": "values",
            "description": "The values to add.",
            "type": "array"
          }
        ]
      }
    ]
        
and could be obtained by a request for the following URL:

    http://example.com/rpc/math
    
## Implementations
Support currently exists for implementing HTTP-RPC services in Java, and consuming services in Java, Objective-C/Swift, or JavaScript. Support for other platforms may be added in the future. Contributions are welcome.

For examples and additional information, please see the [wiki](https://github.com/gk-brown/HTTP-RPC/wiki).

# Java Server
The Java server implementation of HTTP-RPC allows developers to create and publish HTTP-RPC web services in Java. It is distributed as a JAR file that contains the following classes:

* _`org.httprpc`_
    * `WebService` - abstract base class for HTTP-RPC services
    * `RequestDispatcherServlet` - servlet that dispatches requests to service instances
    * `Template` - annotation that associates a template with a web service method
* _`org.httprpc.beans`_
    * `BeanAdapter` - wrapper class that presents the contents of a Java Bean instance as a map, suitable to serialization to JSON
* _`org.httprpc.sql`_
    * `ResultSetAdapter` - wrapper class that presents the contents of a JDBC result set as an iterable list, suitable for streaming to JSON
    * `Parameters` - class for simplifying execution of prepared statements

Each of these classes is discussed in more detail below. 

The JAR file for the Java server implementation of HTTP-RPC can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 8 and a servlet container supporting servlet specification 3.1 or later are required.

## WebService Class
`WebService` is an abstract base class for HTTP-RPC web services. All services must extend this class and must provide a public, zero-argument constructor.

Service methods are defined by adding public methods to a concrete service implementation. All public methods defined by the class automatically become available for remote execution when the service is published, as described in the next section. Note that overloaded methods are not supported; every method name must be unique. 

Scalar method arguments can be any numeric primitive type, a boolean primitive, or `String`. Object wrappers for primitive types are also supported. Multi-value ("vector") arguments are specified as lists of any supported scalar type; e.g. `List<Double>`.

Methods must return a numeric or boolean primitive type, one of the following reference types, or `void`:

* `java.lang.String`
* `java.lang.Number`
* `java.lang.Boolean`
* `java.util.List`
* `java.util.Map`

`Map` implementations must use `String` values for keys. Nested structures are supported.

`List` and `Map` types are not required to support random access; iterability is sufficient. Additionally, `List` and `Map` types that implement `java.lang.AutoCloseable` will be automatically closed after their values have been written to the output stream. This allows service implementations to stream response data rather than buffering it in memory before it is written. 

For example, the `ResultSetAdapter` class wraps an instance of `java.sql.ResultSet` and exposes its contents as a forward-scrolling, auto-closeable list of map values. Closing the list also closes the underlying result set, ensuring that database resources are not leaked. `ResultSetAdapter` is discussed in more detail later.

### Request Metadata
`WebService` provides the following methods that allow an extending class to obtain additional information about the current request:

* `getLocale()` - returns the locale associated with the current request
* `getUserName()` - returns the user name associated with the current request, or `null` if the request was not authenticated
* `getUserRoles()` - returns a set representing the roles the user belongs to, or `null` if the request was not authenticated

The values returned by these methods are populated via protected setters, which are called once per request by `RequestDispatcherServlet`. These setters are not meant to be called by application code. However, they can be used to facilitate unit testing of service implementations by simulating a request from an actual client. 

### Examples
The following code demonstrates one possible implementation of the hypothetical "math" service discussed earlier:

    public class MathService extends WebService {
        // Add a + b
        public double add(double a, double b) {
            return a + b;
        }

        // Add values
        public double addValues(List<Double> values) {
            double total = 0;

            for (double value : values) {
                total += value;
            }

            return total;
        }
    }

Executing a GET request for the following URL would invoke the service's `add()` method, producing the number 5 in response:

    /math/add?a=2&b=3
    
Similarly, a GET for the following URL would invoke the `addValues()` method, producing the number 9:

    /math/addValues?values=1&values=3&values=5

## RequestDispatcherServlet Class
HTTP-RPC services are published via the `RequestDispatcherServlet` class. This class is resposible for translating HTTP request parameters to method arguments, invoking the specified service method, and serializing the return value to JSON. Note that service classes must be compiled with the `-parameters` flag so their parameter names are available at runtime.

Java objects are mapped to their JSON equivalents as follows:

* `java.lang.String`: string
* `java.lang.Number` or numeric primitive: number
* `java.lang.Boolean` or boolean primitive: true/false
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

The servlet returns an HTTP 200 status code on successful method completion. If any exception is thrown, HTTP 500 is returned.

Servlet security is provided by the underlying servlet container. See the Java EE documentation for more information.

### Service Descriptors
`RequestDispatcherServlet` will automatically generate a service descriptor document in response to a GET on the service root URL. Localized descriptions for service methods and parameter descriptions can be specified via resource bundles. The resource bundles must have the same name as the service and must be formatted as follows:

    method: <method description>
    method_parameter1: <parameter 1 description>
    method_parameter2: <parameter 2 description>
    ...

For example, localized descriptions for `MathService`'s `add()` and `addValues()` methods might be specified in a file named _MathService.properties_ as follows:

    add: Returns the sum of two numbers.
    add_a: The first number.
    add_b: The second number.
    
    addValues: Returns the sum of a list of values.
    addValues_values: The values to add.
    
Additional properties files (e.g. _MathService\_es\_US.properties_) could be provided to support other localizations.

## Templates
Although data produced by an HTTP-RPC web service is typically returned to the caller as JSON, it can also be transformed into other representations via "templates". Templates are documents that describe an output format, such as HTML, XML, or CSV. They are merged with result data at execution time to create the final response that is sent back to the caller.

HTTP-RPC templates are based on the [Ctemplate](https://google-ctemplate.googlecode.com/svn/trunk/doc/guide.html) system, which defines a set of "markers" that are replaced with values supplied by a "data dictionary" when the template is processed. The following marker types are supported by HTTP-RPC:

* {{_variable_}} - injects a variable from the data dictionary into the output
* {{#_section_}}...{{/_section_}} - defines a repeating section of content
* {{!_comment_}} - defines a comment (informational text that is excluded from the final output)

The value returned by the service method represents the data dictionary. Usually, this will be an instance of `java.util.Map` whose keys represent the values supplied by the dictionary. 

For example, a method that calculates a set of statistical values and returns them to the caller might be defined as follows:

    public Map<String, Object> getStatistics(List<Double> values) { ... }
    
A GET for this URL would invoke the `getStatistics()` method, producing a JSON document in response:

    /math/getStatistics?values=1&values=3&values=5

The response might look something like this:

    {
      "average":3.0, 
      "count":3, 
      "sum":9.0
    }

However, it may be more convenient in some circumstances to return the results to the caller in a different format; for example, as HTML to support a browser-based client application. A simple template for presenting the result data as a web page is shown below:

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

Note the use of the variable markers for the "count", "sum", and "average" values. At execution time, these markers will be replaced by the corresponding values in the data dictionary (i.e. the map value returned by the method). Variable markers can be used to refer to any `String`, `Number`, or `Boolean` value. If a property returns `null`, the marker is replaced with the empty string in the generated output. Nested variables can be referred to using dot-separated path notation; e.g. "foo.bar".

The `Template` annotation is used to associate a template document with a method. The annotation's value represents the name of the template document that will be applied to the results. For example, if the HTML template above was named _statistics.html_, the `getStatistics()` method would be annotated as follows:

    @Template("statistics.html")
    public Map<String, Object> getStatistics(List<Double> values) { ... }

With the annotation applied, a GET for the following URL would invoke the `getStatistics()` method and apply the template to the results:

    /math/statistics.html?values=1&values=3&values=5

This request would generate the following markup:

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

Note that it is possible to associate multiple templates with a single service method. For example, the following code adds an additional XML template document to the `getStatistics()` method:

    @Template("statistics.html")
    @Template("statistics.xml")
    public Map<String, Object> getStatistics(List<Double> values) { ... }

### Sections
If a value in a data dictionary is an instance of `java.util.List`, the value's key can be used as a section marker. Content between the section start and end markers is repeated once for each element in the list. If the list is empty or the value of the key is `null`, the section's content is excluded from the output.

For example, a method that returns information about a purchase order might be defined as follows:

    public Map<String, Object> getPurchaseOrder(int orderID) { ... }
    
The method might return a data structure similar to the following:

    {   
      "orderID": 101,
      "customerID": "xyz-1234",
      "date": "2016-2-25",
      "items": [
        {
          "itemID": 1,
          "description": "Item 1",          
          "quantity": 3
        },
        {
          "itemID": 2,
          "description": "Item 2",
          "quantity": 1
        },
        ...
      ]
    }   

In this example, the "items" key refers to a list value, so this key could be used in a template to define a section. For example:

    <html>
    <head>
        <title>Order #{{orderID}}</title>
    </head>
    <body>
    <p>Customer ID: {{customerID}}</p>
    <p>Date: {{date}}</p>
    <table>
        <tr>
            <td>Item #</td>
            <td>Description</td>
            <td>Quantity</td>
        </tr>
        {{#items}}
        <tr>
            <td>{{itemID}}</td>
            <td>{{description}}</td>
            <td>{{quantity}}</td>
        </tr>
        {{/items}}
    </table>
    </body>
    </html>

Note that the "orderID" and "customerID" variables are referenced outside of the section declaration, since they are defined by the data dictionary for the order itself, not by the dictionaries for the line items.

If the `getPurchaseOrder()` method is associated with this template (for example, named _order.html_), a GET request for the following URL would execute the method and generate an HTML document reflecting the contents of the order:

    /orders/order.html?orderID=101

The "items" section would be repeated for each item in the list, producing the following output:

    <html>
    <head>
        <title>Order #101</title>
    </head>
    <body>
    <p>Customer ID: xyz-1234</p>
    <p>Date: 2016-2-25</p>
    <table>
        <tr>
            <td>Item #</td>
            <td>Description</td>
            <td>Quantity</td>
        </tr>
        <tr>
            <td>1</td>
            <td>Item 1</td>
            <td>3</td>
        </tr>
        <tr>
            <td>2</td>
            <td>Item 2</td>
            <td>1</td>
        </tr>
        ...
    </table>
    </body>
    </html>

#### Dot Notation
Each section in a template must be backed by a map representing the section's data dictionary. This implies that list elements must always be instances of `java.util.Map`. Unfortunately, this is not always convenient. In the previous example, it was natural to associate the list of order items with the object representing the order itself. However, in many cases it may be more appropriate for a service method to return a list directly, rather than as a property of some parent object.

In order to support this case, HTTP-RPC automatically wraps any list element that is not already a map in a map instance, and assigns a default name of "." to the element. For example, if the method in the previous example had been defined as follows:

    @Template("orderitems.html")
    public List<Object> getPurchaseOrderItems(int orderID) { ... }

it might simply return a list similar to the following:

    [
      {
        "itemID": 1,
        "description": "Item 1",          
        "quantity": 3
      },
      {
        "itemID": 2,
        "description": "Item 2",
        "quantity": 1
      },
      ...
    ]

Since the list is no longer accessible via a key in a data dictionary, there would otherwise be no way to refer to it and define a section in the template. However, using dot notation, the template can be defined as follows (note the use of the "." character in the section declaration):

    <html>
    <body>
    <table>
        <tr>
            <td>Item #</td>
            <td>Description</td>
            <td>Quantity</td>
        </tr>
        {{#.}}
        <tr>
            <td>{{itemID}}</td>
            <td>{{description}}</td>
            <td>{{quantity}}</td>
        </tr>
        {{/.}}
    </table>
    </body>
    </html>

Executing a GET for _orderitems.html_ with an order ID of 101 would produce the following response:

    <html>
    <body>
    <table>
        <tr>
            <td>Item #</td>
            <td>Description</td>
            <td>Quantity</td>
        </tr>
        <tr>
            <td>1</td>
            <td>Item 1</td>
            <td>3</td>
        </tr>
        <tr>
            <td>2</td>
            <td>Item 2</td>
            <td>1</td>
        </tr>
        ...
    </table>
    </body>
    </html>

Dot notation isn't limited to lists and sections - it can also be used in variable markers. For example, the following template could be used to generate a simple HTML response to the `add()` method discussed earlier, which returns a single `double` value:

    <html>
    <body>
    <p>{{.}}</p>
    </body>
    </html>
    
If the result of the add operation was the number `8`, the resulting output would look like this:

    <html>
    <body>
    <p>8</p>
    </body>
    </html>

Further, if a method returns a list of non-map elements, they can be referred to within a section as follows:

    <html>
    <body>
    <table>
    {{#.}}
    <tr><td>{{.}}</td></tr>
    {{/.}}
    </table>
    </body>
    </html>
    
For example, if the JSON response to the method contained the following:

    [1, 2, 3, 5, 8, 13, 21]
    
the generated HTML would contain a table containing seven rows, each with a single cell containing the corresponding value from the list. 

## BeanAdapter Class
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
    
        double sum = 0;
        
        for (double value : values) {
            sum += value;
        }
    
        statistics.setSum(sum);
        statistics.setAverage(sum / n);
    
        return new BeanAdapter(statistics);
    }

Although the values are actually stored in the strongly typed `Statistics` object, the adapter makes the data appear as a map, allowing it to be returned to the caller as a JSON object.

Note that, if a property returns a nested Bean type, the property's value will be automatically wrapped in a `BeanAdapter` instance. Additionally, if a property returns a `List` or `Map` type, the value will be wrapped in an adapter of the appropriate type that automatically adapts its sub-elements. 

The `BeanAdapter#adapt()` method is used to adapt property values. This method is called internally by `BeanAdapter#get()`, but it can also be used to explicitly adapt list or map values as needed.

See the Javadoc for the `BeanAdapter` class for more information.

## ResultSetAdapter Class
The `ResultSetAdapter` class allows the result of a SQL query to be efficiently returned from a service method. This class implements the `List` interface and makes each row in a JDBC result set appear as an instance of `Map`, rendering the data suitable for serialization to JSON by `RequestDispatcherServlet`. It also implements the `AutoCloseable` interface, to ensure that the underlying result set is closed and database resources are not leaked.

`ResultSetAdapter` is forward-scrolling only; its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a result set to be returned directly to the caller without any intermediate buffering. The caller can simply execute a JDBC query, pass the resulting result set to the `ResultSetAdapter` constructor, and return the adapter instance:

    public List<Map<String, Object>> getData() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from some_table");
        
        return new ResultSetAdapter(resultSet);
    }

## Parameters Class
The `Parameters` class provides a means for executing prepared statements using named parameter values rather than indexed arguments. Parameter names are specified by a leading `:` character. For example:

    SELECT * FROM some_table 
    WHERE column_a = :a OR column_b = :b OR column_c = COALESCE(:c, 4.0)
    
The `parse()` method is used to create a `Parameters` instance from a SQL statement. It takes a `java.io.Reader` containing the SQL text as an argument:

    Parameters parameters = Parameters.parse(new StringReader(sql));

The `getSQL()` method of the `Parameters` class returns the parsed SQL in standard JDBC syntax; for example:

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

# Java Client
The Java client implementation of HTTP-RPC enables Java-based applications to consume HTTP-RPC web services. It is distributed as a JAR file that includes the following types, discussed in more detail below:

* _`org.httprpc`_
    * `WebServiceProxy` - invocation proxy for HTTP-RPC services
    * `ResultHandler` - callback interface for handling results
    * `Result` - abstract base class for typed results

The JAR file for the Java client implementation of HTTP-RPC can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 7 or later is required.

## WebServiceProxy Class
The `WebServiceProxy` class acts as a client-side invocation proxy for HTTP-RPC web services. Internally, it uses an instance of `HttpURLConnection` to send and receive data. Requests are submitted via HTTP POST.

`WebServiceProxy` provides a single constructor that takes the following arguments:

* `baseURL` - an instance of `java.net.URL` representing the base URL of the service
* `executorService` - an instance of `java.util.concurrent.ExecutorService` that will be used to execute service requests

The base URL represents the fully-qualified name of the service. Method names are appended to this URL during execution. 

The executor service is used to schedule remote method requests. Internally, requests are implemented as a `Callable` that is submitted to the service. See the `ExecutorService` Javadoc for more information.

Remote methods are executed by calling the `invoke()` method:
    
    public <V> Future<V> invoke(String methodName, Map<String, Object> arguments, ResultHandler<V> resultHandler) { ... }

This method takes the following arguments:

* `methodName` - the name of the remote method to invoke
* `arguments` - an instance of `java.util.Map` containing the arguments to the remote method as key/value pairs
* `resultHandler` - an instance of `org.httprpc.ResultHandler` that will be invoked upon completion of the remote method

A convenience method for invoking remote methods that don't take any arguments is also provided.

Scalar arguments can be any numeric type, a boolean, or a string. Multi-value arguments are specified as a list of any supported scalar type; e.g. `List<Double>`.

The result handler is called upon completion of the remote method. `ResultHandler` is a functional interface whose single method, `execute()`, is defined as follows:

    public void execute(V result, Exception exception);

On successful completion, the first argument will contain the result of the remote method call. It will be an instance of one of the following types or `null`, depending on the content of the JSON response returned by the server:

* `java.lang.String`: string
* `java.lang.Number`: number
* `java.lang.Boolean` true/false
* `java.util.List`: array
* `java.util.Map`: object

The second argument will always be `null` in this case. If an error occurs, the first argument will be `null` and the second will contain an exception representing the error that occurred.

Both variants of the `invoke()` method return an instance of `java.util.concurrent.Future` representing the invocation request. This object allows a caller to cancel an outstanding request as well as obtain information about a request that has completed.

Request security is provided by the underlying URL connection. See the `HttpURLConnection` documentation for more information.

### Argument Map Creation
Since explicit creation and population of the argument map can be cumbersome, `WebServiceProxy` provides the following static convenience methods to help simplify map creation:

    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) { ... }
    public static <K> Map.Entry<K, ?> entry(K key, Object value) { ... }
    
Using these convenience methods, argument map creation can be reduced from this:

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("a", 2);
    arguments.put("b", 4);
    
to this:

    Map<String, Object> arguments = mapOf(entry("a", 2), entry("b", 4));
    
A complete example using `WebServiceProxy#invoke()` is shown later.

### Multi-Threading Considerations
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

Similar dispatchers can be configured for other Java UI toolkits such as Swing, JavaFX, and SWT. Command-line applications can generally use the default dispatcher, which simply performs result handler notifications on the current thread.

## Result Class
`Result` is an abstract base class for typed results. Using this class, applications can easily map untyped object data returned by a service method to typed values. It provides the following public methods that can be used to populate Java Bean property values from dictionary entries:

    public void set(String name, Object value) { ... }
    public void setAll(Map<String, Object> values) { ... }

Additionally, it defines the following protected method that can be overridden to handle missing properties:

    protected void setUndefined(String name, Object value) { ... }
    
For example, the following Java class might be used to provide a typed version of the statistical data returned by the `getStatistics()` method discussed earlier:

    public class Statistics extends Result {
        private int count;
        private double sum;
        private double average;

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
        Statistics statistics = new Statistics();
        statistics.setAll(result);

        // statistics.getCount() = 3
        // statistics.getSum() = 9.0
        // statistics.getAverage() = 3.0
    });

## Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to invoke the methods of the hypothetical math service discussed earlier. It first creates an instance of the `WebServiceProxy` class and configures it with a pool of ten threads for executing requests. It then invokes the `add()` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `addValues()` method, passing the values 1, 2, 3, and 4 as arguments:

    // Create service
    URL baseURL = new URL("https://localhost:8443/httprpc-test-server/test/");
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    WebServiceProxy serviceProxy = new WebServiceProxy(baseURL, executorService);

    // Add a + b
    serviceProxy.invoke("add", mapOf(entry("a", 2), entry("b", 4)), new ResultHandler<Number>() {
        @Override
        public void execute(Number result, Exception exception) {
            // result is 6
        }
    });
    
    // Add values
    serviceProxy.invoke("add", mapOf(entry("values", Arrays.asList(1, 2, 3, 4))), new ResultHandler<Number>() {
        @Override
        public void execute(Number result, Exception exception) {
            // result is 10
        }
    });

Note that, in Java 8 or later, lambda expressions can be used instead of anonymous classes to implement result handlers, reducing the code for invoking the remote methods to the following:

    // Add a + b
    serviceProxy.invoke("add", mapOf(entry("a", 2), entry("b", 4)), (result, exception) -> {
        // result is 6
    });

    // Add values
    serviceProxy.invoke("addValues", mapOf(entry("values", Arrays.asList(1, 2, 3, 4))), (result, exception) -> {
        // result is 10
    });

# Objective-C/Swift Client
The Objective-C/Swift client implementation of HTTP-RPC enables iOS applications to consume HTTP-RPC services. It is delivered as a modular framework that defines a single `WSWebServiceProxy` class, which is discussed in more detail below. 

The framework for the Objective-C/Swift client can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). It is also available via [CocoaPods](https://cocoapods.org/pods/HTTP-RPC). iOS 8 or later is required.

## WSWebServiceProxy Class
The `WSWebServiceProxy` class serves as an invocation proxy for HTTP-RPC services. Internally, it uses an instance of `NSURLSession` to issue HTTP requests, which are submitted via HTTP POST. It uses the `NSJSONSerialization` class to deserialize response content.

Service proxies are initialized via the `initWithSession:baseURL:` method, which takes an `NSURLSession` instance and the service's base URL as arguments. Method names are appended to this URL during method execution.

Remote methods are invoked by calling either `invoke:resultHandler:` or `invoke:withArguments:resultHandler:` on the service proxy. The first version is a convenience method for calling remote methods that don't take any arguments. The second takes a dictionary of argument values to be passed to the remote method. The first method delegates to the second, passing an empty argument dictionary.

Scalar arguments can be any numeric type, a boolean, or a string. Multi-value arguments are specified as an array of any supported scalar type; e.g. `[Double]`.

Both invocation methods take a result handler as the final argument. The result handler is a callback that is invoked upon successful completion of the remote method, as well as if the method call fails. The callback takes two arguments: a result object and an error object. If the remote method completes successfully, the first argument contains the value returned by the method, or `nil` if the method does not return a value. If the method call fails, the second argument will be populated with an instance of `NSError` describing the error that occurred.

Both methods return an instance of `NSURLSessionDataTask` representing the invocation request. This allows an application to cancel a task, if necessary.

Although requests are typically processed on a background thread, result handlers are called on the same operation queue that initially invoked the service method. This is typically the application's main queue, which allows result handlers to update the application's user interface directly, rather than posting a separate update operation to the main queue.

Request security is provided by the the underlying URL session. See the `NSURLSession` documentation for more information.

## Examples
The following code snippet demonstrates how `WSWebServiceProxy` can be used to invoke the methods of the hypothetical math service. It first creates an instance of the `WSWebServiceProxy` class backed by a default URL session and a delegate queue supporting ten concurrent operations. It then invokes the `add()` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `addValues()` method, passing the values 1, 2, 3, and 4 as arguments:

    // Configure session
    let configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
    configuration.requestCachePolicy = NSURLRequestCachePolicy.ReloadIgnoringLocalAndRemoteCacheData

    let delegateQueue = NSOperationQueue()
    delegateQueue.maxConcurrentOperationCount = 10

    let session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

    // Initialize service proxy and invoke methods
    let baseURL = NSURL(string: "https://localhost:8443/httprpc-test-server/test/")

    let serviceProxy = WSWebServiceProxy(session: session, baseURL: baseURL!)
    
    // Add a + b
    serviceProxy.invoke("add", withArguments: ["a": 2, "b": 4]) {(result, error) in
        // result is 6
    }

    // Add values
    serviceProxy.invoke("addValues", withArguments: ["values": [1, 2, 3, 4]]) {(result, error) in
        // result is 10
    }

# JavaScript Client
The JavaScript client implementation of HTTP-RPC enables browser-based applications to consume HTTP-RPC services. It is delivered as source code file and defines a single `WebServiceProxy` class, which is discussed in more detail below. 

The source code for the JavaScript client can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases).

## WebServiceProxy Class
The `WebServiceProxy` class serves as an invocation proxy for HTTP-RPC services. Internally, it uses an instance of `XMLHttpRequest` to communicate with the server, and uses `JSON.parse()` to convert the response to an object. Requests are submitted via HTTP POST. 

Service proxies are initialized via the `WebServiceProxy` constructor, which takes a single `baseURL` argument representing the path to the service. Method names are appended to this URL during method execution.

Remote methods are invoked by calling either `invoke()` or `invokeWithArguments()` on the service proxy. The first version is a convenience method for calling remote methods that don't take any arguments. The second takes an object containing the set of argument values to be passed to the remote method. The first method delegates to the second, passing an empty argument object.

Scalar arguments can be number, boolean, or string values. Multi-value arguments are specified as an array of scalar values; e.g. `[1, 2, 3]`.

Both invocation methods take a result handler as the final argument. The result handler is a callback function that is invoked upon successful completion of the remote method, as well as if the method call fails. The callback takes two arguments: a result object and an error object. If the remote method completes successfully, the first argument contains the value returned by the method, or `null` if the method does not return a value. If the method call fails, the second argument will contain the HTTP status code corresponding to the error that occurred.

Both methods return the `XMLHttpRequest` instance used to execute the remote call. This allows an application to cancel a request, if necessary.

## Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to invoke the methods of the hypothetical math service. It first creates an instance of the `WebServiceProxy` class that points to the base service URL. It then invokes the `add()` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `addValues()` method, passing the values 1, 2, 3, and 4 as arguments:

    // Initialize service proxy and invoke methods
    var serviceProxy = new WebServiceProxy("/httprpc-test-server/test");

    // Add
    serviceProxy.invokeWithArguments("add", {a:4, b:2}, function(result, error) {
        // result is 6
    });

    // Add values
    serviceProxy.invokeWithArguments("addValues", {values:[1, 2, 3, 4]}, function(result, error) {
        // result is 10
    });

# More Information
For more information, see [the wiki](https://github.com/gk-brown/HTTP-RPC/wiki) or [the issue list](https://github.com/gk-brown/HTTP-RPC/issues).
