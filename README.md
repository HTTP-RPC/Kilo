# Overview
WebRPC is a mechanism for executing remote procedure calls via HTTP. It is an intentionally simple protocol that is designed primarily to address common use cases in web service development. Any platform capable of submitting an HTTP request and consuming a JSON response can be a WebRPC client, and any platform capable of responding to an HTTP request and producing a JSON result can act as a WebRPC server.

WebRPC procedures, or "methods", are encapsulated by a "service", which is a collection of related methods. The service and method names are specified in the path component of the request URL. Method arguments are passed in either the query string for GET requests or in the request body for POST requests. POST requests are encoded using the `application/x-www-form-urlencoded` MIME type, like an HTML form. Other HTTP operations are not supported.

Method results are typically returned as JSON values. However, methods that do not return a value are also supported.

For example, a GET request for the following URL might invoke the "add" method of a hypothetical "math" service:

    http://example.com/rpc/math/add?a=1&b=2
    
The values 1 and 2 are passed as the "a" and "b" arguments to the method, respectively, with the service returning the value 3 in response. Alternatively, the same result could be obtained by submitting a POST request for the path part of the URL along with a request body containing the query component.

Method parameters may be either scalar (single-value) or vector (multi-value) types. Values may be any simple type including string, number, or boolean (true/false). As with any HTTP request, values that include reserved characters must be URL-encoded.

Multi-value arguments are specified by providing zero or more values for a given parameter. For example, the add method above could be modified to accept a list of numbers to add rather than two fixed parameters:

    http://example.com/rpc/math/add?value=1&value=2&value=3

The order in which parameters are specified does not matter. Omitting a value for a scalar parameter produces a null argument value for that parameter. Omitting all values for a vector parameter produces an empty collection argument for the parameter.

Return values may be any JSON type, including string, number, boolean (true/false), object, array, and null. No content is returned by a method that does not produce a value.

An HTTP 200 is returned on successful completion, and HTTP 500 is returned in the case of an error (i.e. an exception). Note that exceptions are intended to represent unexpected failures, not application-specific errors. No other HTTP status codes are supported.

## Implementations
Support currently exists for implementing web RPC services in Java and consuming services in Java or Objective-C/Swift. Support for other platforms may be added in the future. Contributions are welcome.

# Java Server
The Java server implementation of WebRPC allows developers to build web RPC services in Java. It is distributed as a JAR file that contains the following classes:

* _`vellum.webrpc`_
    * `WebRPCService` - abstract base class for web RPC services
    * `WebRPCServlet` - servlet that hosts web RPC services
    * `Roles` - interface for determining user role membership
    * `Result` - abstract base class for custom result types
* _`vellum.webrpc.sql`_
    * `ResultSetAdapter` - exposes the contents of a JDBC result set as an iterable list, suitable for streaming a JSON response

Each of these classes is discussed in more detail below. 

The JAR file for the Java server implementation of WebRPC can be downloaded [here](https://github.com/gk-brown/WebRPC/releases). Java 8 and a servlet container supporting servlet specification 3.1 or later are required.

## WebRPCService Class
`WebRPCService` is the abstract base class for web RPC services. All services must extend this class, and must provide a zero-argument constructor so they can be instantiated by `WebRPCServlet`, which is discussed in more detail below.

Service methods are defined by adding public methods to a concrete service class. All public methods defined by the service class automatically become available for remote execution when the service is published, as described later. Note that overloaded methods are not supported; every method name must be unique. 

Scalar method arguments can be any numeric primitive type, a boolean primitive, or `String`. Object wrappers for primitive types are also supported. Multi-value ("vector") arguments are specified as lists of any supported scalar type; e.g. `List<Double>`.

Methods must return a numeric or boolean primitive type, one of the following reference types, or `void`:

* `java.lang.String`
* `java.lang.Number`
* `java.lang.Boolean`
* `java.util.List`
* `java.util.Map`

Nested structures (i.e. list and maps contained within other lists or maps) are supported.

`Map` implementations must use `String` values for keys. `List` and `Map` types are not required to support random access; iterability is sufficient. Iterator-only, or "forward-scrolling", implementations can simply implement the `iterator()` method and throw `UnsupportedOperationException` from collection accessor methods such as `get()` and `size()`.

### Auto-Closeable Types
`List` and `Map` types that implement `java.lang.AutoCloseable` will be automatically closed after their values have been written to the output stream. This allows service implementations to stream response data rather than buffering it in memory before it is written. 

For example, the `ResultSetAdapter` class discussed below wraps an instance of `java.sql.ResultSet` and exposes its contents as an auto-closeable list of map values. Calling `close()` on the list closes the underlying result set, ensuring that database resources are not leaked.

### Request Metadata
`WebRPCService` provides a set of protected methods that allow an extending class to obtain additional information about a request that is not included in the method arguments:

* `getLocale()` - returns the locale associated with the current request
* `getUserPrincipal()` - returns the user principal associated with the current request
* `isUserInRole()` - verifies that the user making the current request belongs to a given logical role

These methods correspond directly to the similarly-named methods defined by the `javax.servlet.http.HttpServletRequest` interface. See the servlet specification for more information on their use.

### Unit Testing
`WebRPCService` defines an additional method that is used to provide a service instance with information about the current request:

    protected void initialize(Locale locale, Principal userPrincipal, Roles roles)

This method is called once by `WebRPCServlet` for each request. However, it can also be used to facilitate unit testing of RPC services. By calling this method, the test framework can simulate a request from an actual web RPC client. 

The `Roles` interface is provided to allow test code to simulate user roles. It is discussed in more detail below. 

### Examples
The following example contains a possible Java implementation of the hypothetical "math" service discussed earlier:

    // Sample implementation of hypothetical math service
    public class MathService {
        // Adds two numbers
        public double add(double a, double b) {
            return a + b;
        }

        // Adds a list of double values specified in a list
        public double addValues(List<Double> values) {
            if (values == null) {
                throw new IllegalArgumentException();
            }

            double total = 0;

            for (double value : values) {
                total += value;
            }

            return total;
        }
    }

Executing an HTTP GET request for the following URL would produce the number 9 in response:

    /math/add?a=6&b=3
    
Similarly, a GET for the following URL would also produce the number 9:

    /math/addValues?values=1&values=3&values=5

## WebRPCServlet Class
Web RPC services are "published", or made available, via the `WebRPCServlet` class. This class is resposible for translating HTTP request parameters to method arguments, invoking the service method, and serializing the return value to JSON. 

Java objects are mapped to their JSON equivalents as follows:

* `java.lang.String`: string
* `java.lang.Number` or numeric primitive: number
* `java.lang.Boolean` or boolean primitive: true/false
* `java.util.List`: array
* `java.util.Map`: object

Each servlet instance hosts a single web RPC service. The name of the service type is passed to the servlet via the "serviceClassName" initialization parameter. For example:

	<servlet>
		<servlet-name>MathServlet</servlet-name>
		<servlet-class>vellum.webrpc.WebRPCServlet</servlet-class>
        <init-param>
            <param-name>serviceClassName</param-name>
            <param-value>com.example.MathService</param-value>
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>MathServlet</servlet-name>
        <url-pattern>/math/*</url-pattern>
    </servlet-mapping>

A new service instance is created and initialized for each request. The servlet then converts the request parameters to the argument types expected by the named method, invokes the method, and writes the return value to the response stream as JSON.

The servlet returns an HTTP 200 status code on successful method completion. If any exception is thrown, HTTP 500 is returned.

Servlet security is provided by the underlying servlet container. See the Java EE documentation for more information.

## Result Class
Although service methods can return instances of `Map` directly, it is often preferable to work with more strongly-typed data structures in code. The `Result` class provides an abstract base class for custom result types. It implements the `Map` interface and exposes any Bean properties defined by a subclass as entries in the map, allowing custom types to be serialized to JSON. 

For example, the following class might be used to represent some simple statistical information about a set of values:

    public class Statistics extends Result {
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

A service method that calculates the statistical values and returns them to the caller might look like this:

    public Statistics getStatistics(List<Double> values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        Statistics statistics = new Statistics();

        int n = values.size();

        statistics.setCount(n);

        for (double value : values) {
            statistics.setSum(statistics.getSum() + value);
        }

        statistics.setAverage(statistics.getSum() / n);

        return statistics;
    }

Executing GET for this URL would invoke the `getStatistics()` method:

    /math/getStatistics?values=1&values=3&values=5
    
producing the following result:

    {"average":3.0, "count":3, "sum":9.0}    

### Roles Interface
The `Roles` interface provides an abstraction for determining user role membership. It is provided primarily to facilitate unit testing of service methods. It defines a single `isUserInRole()` method that a test framework can implement to simulate different user roles. This allows services to be tested without the need to start a servlet container.

For example, the test code could store a mapping of user name to role in a text file and provide an implementation of the `Roles` interface that operates against this data. Individual tests could instantiate a concrete service class and pass an instance of the `Roles` implementation to the service's `initialize()` method, along with the locale and user principal. Once initialized, the service methods could be invoked directly by the unit test, producing the same results as if they had been invoked by the servlet.

## ResultSetAdapter Class
The `ResultSetAdapter` class allows the result of a SQL query to be efficiently returned from a an RPC method. This class implements the `List` interface and makes each row in a JDBC result set appear as an instance of `Map`, rendering the data suitable for serialization to JSON by the servlet. `ResultSetAdapter` also implements the `AutoCloseable` interface and closes the underlying result set when its `close()` method is called, ensuring that database resources are not leaked.

`ResultSetAdapter` is forward-scrolling only; its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a result set to be returned directly to the caller without any intermediate buffering. The caller can simply execute a JDBC query, pass the resulting result set to the `ResultSetAdapter` constructor, and return the adapter instance:

    public ResultSetAdapter getData() throws SQLException {
        Statement statement = connection.createStatement();
   
        return new ResultSetAdapter(statement.executeQuery("select * from some_table"));
    }

# Java Client
The Java client implementation of WebRPC enables Java-based applications to consume web RPC services. It is distributed as a JAR file that includes the following classes:

* _`vellum.webrpc`_
    * `WebRPCService` - invocation proxy for web RPC services
    * `ResultHandler` - callback interface for handling results
    * `Dispatcher` - interface for dispatching results to result handlers
    * `Result` - abstract base class for typed results

Each of these classes is discussed in more detail below. 

Note that the `WebRPCService` and `Result` classes provided by the Java client library are not the same as those provided by the Java server implementation. They simply have similar names because they serve a similar purpose.

The JAR file for the Java client implementation of WebRPC can be downloaded [here](https://github.com/gk-brown/WebRPC/releases). Java 7 or later is required.

## WebRPCService Class
The `WebRPCService` class acts as a client-side invocation proxy for web RPC services. Internally, it uses an instance of `HttpURLConnection` to send and receive data. Requests are submitted via HTTP POST.

`WebRPCService` provides a single constructor that takes the following three arguments:

* `baseURL` - an instance of `java.net.URL` representing the base URL of the service
* `executorService` - an instance of `java.util.concurrent.ExecutorService` that will be used to execute service requests
* `dispatcher` - an instance of `vellum.webrpc.Dispatcher` that will be used to dispatch results

The base URL represents the fully-qualified name of the service. Method names are appended to this URL during execution. 

The executor service is used to schedule remote method requests. Internally, requests are implemented as a `Callable` that is submitted to the service. See the `ExecutorService` Javadoc for more information.

A dispatcher is used to notify a caller that a remote method has completed. Because executor services often execute tasks on a background thread, an application needs a way to ensure that the handlers are called on the main UI thread. Since Java UI platforms have different ways of invoking code on the main thread, the `Dispatcher` interface is provided as a means of abstracting this behavior. It is discussed in more detail later.

Remote methods are executed by calling the `invoke()` method:

    public <V> Future<V> invoke(String methodName, Map<String, Object> arguments, ResultHandler<V> resultHandler) { ... }

This method takes the following arguments:

* `methodName` - the name of the remote method to invoke
* `arguments` - an instance of `java.util.Map` containing the arguments to the remote method as key/value pairs
* `resultHandler` - an instance of `vellum.webrpc.ResultHandler` that will be invoked upon completion of the remote method

`WebRPCService` also provides the following convenience method for calling remote methods that don't take any arguments. It simply delegates to the first method, passing an empty argument map:

    public <V> Future<V> invoke(String methodName, ResultHandler<V> resultHandler) { ... }

Scalar arguments can be any numeric type, a boolean, or a string. Multi-value arguments are specified as a list of any supported scalar type; e.g. `List<Double>`.

The result handler is called upon completion of the method. As noted earlier, it is invoked by the dispatcher provided to the service constructor. The `ResultHandler` interface is discussed in more detail below.

The `invoke()` methods return an instance of `java.util.concurrent.Future` representing the invocation request. This object allows a caller to cancel an outstanding request as well as obtain information about a request that has completed.

Request security provided by the underlying `HttpURLConnection` implementation. See the Javadoc for more information.

### ResultHandler Interface
The `ResultHandler` interface is the means by which applications are notified when a remote method invocation is complete. It defines a single method, `execute()`, that is called when a remote method call succeeds or fails:

    public void execute(V result, Exception exception);

On successful completion, the first argument contains the result of the remote method call. The second argument will be `null` in this case. If an error occurs, the first argument will be `null` and the second will contain an exception representing the error that occurred.

Because `ResultHandler` is a functional interface (i.e. defines only a single method), it can be implemented using a lambda expression in Java 8 or later. Using lambdas can significantly reduce the verbosity and improve the readability of callback handling code. An example is provided later.

### Dispatcher Interface
The `Dispatcher` interface is used to dispatch success or failure notifications to result handlers. It defines two methods:

    public <V> void dispatchResult(V result, ResultHandler<V> resultHandler);    
    public void dispatchException(Exception exception, ResultHandler<?> resultHandler);

The first method is called to dispatch the result of a successful remote method invocation. The second is called in case of a failure.

Implementation details will vary by platform. A Swing application might call the `SwingUtilities#invokeAndWait()` method to post a runnable to the event dispatch thread, whereas an Android application might call `Handler#post()`, an SWT application might call `Display#asyncExec()`, and a JavaFX application might call `Platform.runLater()` to execute code on the UI thread. A dispatcher for a headless application might simply invoke the callback on the current thread.

### Examples
The following code snippet demonstrates how `WebRPCService` can be used to invoke the methods of the hypothetical math service discussed earlier. It creates an instance of the `WebRPCService` class and configures it with a pool of ten threads for executing requests. Since the example does not have a user interface, a test dispatcher that simply delegates to the handler is used.

The code then invokes the `add()` method, passing a value of 2 for "a" and 4 for "b". The result of executing the method is the number 6:

    // Create service
    URL baseURL = new URL("https://localhost:8443/webrpc-test-1.0/test/");
    ExecutorService threadPool = Executors.newFixedThreadPool(10);
    TestDispatcher testDispatcher = new TestDispatcher();

    WebRPCService service = new WebRPCService(baseURL, threadPool, testDispatcher);

    // Add a + b
    HashMap<String, Object> addArguments = new HashMap<>();
    addArguments.put("a", 2);
    addArguments.put("b", 4);

    service.invoke("add", addArguments, new ResultHandler<Number>() {
        @Override
        public void execute(Number result, Exception exception) {
            // result is 6
        }
    });

Note that the above example uses an anonymous inner class to implement the result handler. In Java 8 or later, a lamba expression can be used instead. For example, the following code executes the `addValues()` method of the service, passing the values 1, 2, 3, and 4 as arguments. The result of executing the method is 10:

    // Add values
    HashMap<String, Object> addValuesArguments = new HashMap<>();
    addValuesArguments.put("values", Arrays.asList(1, 2, 3, 4));

    service.invoke("addValues", addValuesArguments, (result, exception) -> {
        // result is 10
    });

## Result Class
`Result` is an abstract base class for typed results. Using this class, applications can easily map untyped object data returned by a service method to typed values.

`Result` provides a single constructor that accepts a map of property values to be applied to the object. The keys in the map correspond to the name of the Bean properties defined by the result:

    public Result(Map<String, Object> properties) { ... }

The constructor iterates over the key/value pairs in the given map and applies the values to the Bean properties. In general, the values must be of the correct type. However, the `Result` class will perform numeric coercion as needed; for example, if the map contains an integer but the corresponding property type is `double`, the value will be converted to a double before the property's setter is invoked.

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

The result of the `getStatistics()` method can be converted to a `Statistics` instance as follows:

    service.invoke("getStatistics", getStatisticsArguments, (Map<String, Object> result, Exception exception) -> {
        Statistics statistics = new Statistics(result);

        // statistics.getCount() = 3
        // statistics.getSum() = 9.0
        // statistics.getAverage() = 3.0
    });

Note that the `Result` constructor does not perform deep initialization. Nested result properties must be explicitly initialized.

# Objective-C/Swift Client
The Objective-C/Swift client implementation of WebRPC enables Cocoa and Cocoa Touch applications to consume web RPC services. It is distributed as a modular framework that includes the following classes, discussed in more detail below:

* `NMWebRPCService` - invocation proxy for web RPC services
* `NMResult` - abstract base class for typed results

The Objective-C/Swift client framework can be downloaded [here](https://github.com/gk-brown/WebRPC/releases). iOS 8 or later is required.

## WebRPCService Class
Similar to the Java client implementation, the `NMWebRPCService` class serves as an invocation proxy for web RPC services. Internally, it uses an instance of `NSURLSession` to issue HTTP requests, which are submitted via HTTP POST. It uses the `NSJSONSerialization` class to deserialize response content.

Service proxies are initialized via the `initWithSession:baseURL:` method, which takes an `NSURLSession` instance and the service's base URL as arguments. Method names are appended to this URL during method execution.

Remote methods are invoked by calling either `invoke:resultHandler:` or `invoke:withArguments:resultHandler:` on the service proxy. The first version is a convenience method for calling remote methods that don't take any arguments. The second takes a dictionary of argument values to be passed to the remote method. The first method delegates to the second, passing an empty argument dictionary.

Scalar arguments can be any numeric type, a boolean, or a string. Multi-value arguments are specified as an array of any supported scalar type; e.g. `[Double]`.

Both invocation methods take a result handler as the final argument. The result handler is a callback that is invoked upon successful completion of the remote method, as well as if the method call fails. The callback takes two arguments: a result object and an error object. If the remote method completes successfully, the first argument contains the value returned by the method, or `nil` if the method does not return a value. If the method call fails, the second argument will be populated with an instance of `NSError` describing the error that occurred.

Both invocation methods return an instance of `NSURLSessionDataTask` representing the invocation request. This allows an application to cancel a task, if necessary.

Although requests are typically processed on a background thread, result handlers are called on the same operation queue that initially invoked the service method. This is typically the application's main queue, which allows result handlers to update the application's user interface directly, rather than posting a separate update operation to the main queue.

Request security is provided by the the underlying URL session. See the `NSURLSession` documentation for more information.

### Examples
The following code snippet demonstrates how `NMWebRPCService` can be used to invoke the methods of the hypothetical math service. It creates an instance of the `NMWebRPCService` class using a default URL session backed by a delegate queue supporting ten concurrent operations. The code then invokes the `add()` method of the service, passing a value of 2 for "a" and 4 for "b" and producing a result of 6. Finally, it executes the `addValues()` method, passing the values 1, 2, 3, and 4 as arguments and producing a result of 10:

    // Configure session
    configuration = NSURLSessionConfiguration.defaultSessionConfiguration()

    var delegateQueue = NSOperationQueue()
    delegateQueue.maxConcurrentOperationCount = 10

    session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

    // Initialize service and invoke methods
    let baseURL = NSURL(string: "https://localhost:8443/webrpc-test-1.0/test/")

    service = NMWebRPCService(session: session, baseURL: baseURL!)
        
    // Add a + b
    service.invoke("add", withArguments: ["a": 2, "b": 4]) {(result, error) in
        // result is 6
    }

    // Add values
    service.invoke("addValues", withArguments: ["values": [1, 2, 3, 4]]) {(result, error) in
        // result is 10
    }

## Result Class
Like the Java version, `NMResult` provides an abstract base class for typed results. Using this class, applications can easily map untyped object data returned by a service method to typed values.

The only initializer provided by the `NMResult` class is `initWithDictionary:`. This method initializes the result instance with the contents of a dictionary object. Internally, it uses key-value coding (KVC) to set the instance's property values by calling `setValuesForKeysWithDictionary:` on itself. As a result, subclasses of `Result` must be KVC-compliant in order to be initialized properly. See the _Key-Value Programming Guide_ for more information on KVC compliance.

For example, the following Swift class might be used to provide a typed version of the statistical data returned by the `getStatistics()` method discussed earlier:

    class Statistics: NMResult {
        var count: Int = 0
        var sum: Double = 0
        var average: Double = 0
    }

The result of the `getStatistics()` method can be converted to a `Statistics` instance as follows:

    service.invoke("getStatistics", withArguments: ["values": [1, 3, 5]]) {(result, error) in
        let statistics = Statistics(dictionary: result as! [String : AnyObject])
        
        // statistics.count = 3
        // statistics.sum = 9.0
        // statistics.average = 3.0
    }

Note that, as in the Java implementation, `initWithDictionary:` does not perform deep initialization. Result classes with nested result properties must override `initWithDictionary:` to properly map the nested values to the appropriate types.

# More Information
For more information, refer to [the wiki](https://github.com/gk-brown/WebRPC/wiki) or [the issue list](https://github.com/gk-brown/WebRPC/issues).
