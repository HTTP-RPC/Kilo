# Overview
WebRPC is a mechanism for executing remote procedure calls via HTTP. It is an intentionally simple protocol that is designed primarily to address common use cases in web service development. Any platform capable of submitting an HTTP request and consuming a JSON response can be a WebRPC client, and any platform capable of responding to an HTTP request and producing a JSON result can act as a WebRPC server.

WebRPC procedures, or "methods", are encapsulated by a "service", which is a collection of related methods. The service and method names are specified in the path component of the request URL. Method arguments are passed in either the query string for GET requests or in the request body for POST requests. POST requests are encoded using the `application/x-www-form-urlencoded` MIME type, like an HTML form. Other HTTP operations are not supported.

Method results are typically returned as JSON values. However, methods that do not return a value are also supported.

For example, a GET request for the following URL might invoke the "add" method of a hypothetical "math" service:

    http://example.com/rpc/math/add?a=1&b=2
    
The values 1 and 2 are passed as the "a" and "b" arguments to the method, respectively, with the service returning the value 3 in response. Alternatively, the same result could be obtained by submitting a POST request for the path part of the URL along with a request body containing the query component.

Method parameters may be either scalar (single-value) or vector (multi-value, or "array") types. Parameter values may be any simple type including string, number, or boolean (true/false). As with any HTTP request, values that include reserved characters must be URL-encoded.

Multi-value arguments are specified by providing zero or more values for a given parameter. For example, the add method above could be modified to accept a list of numbers to add rather than two fixed parameters:

    http://example.com/rpc/math/add?value=1&value=2&value=3

The order in which parameters are specified does not matter. Omitting a value for a scalar parameter produces an empty (or "null") argument value for that parameter. Omitting all values for a vector parameter produces an empty array argument for the parameter.

Return values may be any JSON type, including string, number, boolean (true/false), object, array, and null. No content is returned by a method that does not produce a value.

An HTTP 200 is returned on successful completion, and HTTP 500 is returned in the case of an error (i.e. an exception). Note that exceptions are intended to represent unexpected failures, not application-specific errors. No other HTTP status codes are supported.

## Implementations
Support currently exists for implementing web RPC services in Java and consuming web RPC services in iOS. Support for other platforms may be added in the future. Contributions are welcome.

# Java Server
The Java server implementation of WebRPC allows developers to build web RPC services in Java. It is distributed as a JAR file that includes the following classes:

* _`vellum.webrpc`_
    * `WebRPCService` - abstract base class for web RPC services
    * `WebRPCServlet` - servlet that hosts web RPC services
    * `Result` - abstract base class for custom result types
    * `Roles` - interface for determining user role membership
* _`vellum.webrpc.sql`_
    * `ResultSetAdapter` - exposes the contents of a JDBC result set as an iterable list, suitable for streaming a JSON response

Each of these classes is discussed in more detail below. 

The JAR file for the Java server implementation of WebRPC can be downloaded [here](https://github.com/gk-brown/WebRPC/releases). Java 8 and a servlet container supporting servlet specification 3.1 or later are required.

## WebRPCService Class
`WebRPCService` is the abstract base class for web RPC services. All services must extend this class, and must provide a zero-argument constructor so they can be instantiated by `WebRPCServlet`, discussed in more detail below.

Service methods are defined by adding public methods to the service class. All public methods defined by a concrete service class automatically become available for remote execution when the service is published, as described later. Note that overloaded methods are not supported; every method name must be unique. 

Scalar method arguments can be any Java numeric primitive type or `String`. Object wrappers for primitive types are also supported. 

Multi-value ("vector") arguments may be specified as arrays of any supported scalar type. Variadic ("varargs") arguments are also supported. 

Methods must return a Java numeric primitive type, one of the following reference types, or `void`:

* `java.lang.String`
* `java.lang.Number`
* `java.lang.Boolean`
* `java.util.List`
* `java.util.Map`

Nested structures (lists and maps) are supported.

`Map` implementations must use `String` values for keys. `List` and `Map` types are not required to support random access; iterability is sufficient. Iterator-only, or "forward-scrolling", implementations can simply implement the `iterator()` method and throw `UnsupportedOperationException` from collection accessor methods.

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

This method is called once by `WebRPCServlet` for each request. However, it can also be used to facilitate unit testing of RPC services. By calling this method, the test framework can simulate a request from an actual RPC client. 

The `Roles` interface is provided to allow test code to simulate user roles. It is discussed in more detail below. 

### Examples
The following example contains a possible Java implementation of the hypothetical "math" service discussed earlier:

    // Sample implementation of hypothetical math service
    public class MathService {
        // Adds two numbers
        public double add(double a, double b) {
            return a + b;
        }

        // Adds a list of double values specified in an array
        public double addArray(double[] values) {
            if (values == null) {
                throw new IllegalArgumentException();
            }

            double total = 0;

            for (double value : values) {
                total += value;
            }

            return total;
        }

        // Adds a list of double values specified as varargs
        public double addVarargs(double... values) {
            return addArray(values);
        }
    }

Executing an HTTP GET request for the following URL would produce the number 9 in response:

    /math/add?a=6&b=3
    
Similarly, a GET for either of the following URLs would produce the number 12:

    /math/addArray?values=1&values=3&values=7
    /math/addVarargs?values=1&values=3&values=7

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
Although service methods can return instances of `Map` directly, it is often more convenient to work with more strongly-typed data structures in code. The `Result` class provides an abstract base class for custom result types. It implements the `Map` interface and exposes any Bean properties defined by a subclass as entries in the map, allowing custom types to be serialized to JSON. 

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

    public Statistics getStatistics(double... values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        Statistics statistics = new Statistics();

        int n = values.length;

        statistics.setCount(n);

        for (int i = 0; i < n; i++) {
            statistics.setSum(statistics.getSum() + values[i]);
        }

        statistics.setAverage(statistics.getSum() / n);

        return statistics;
    }

Executing GET for the following URL:

    /math/getStatistics?values=1&values=3&values=5
    
would produce a result similar to the following:

    {"average":3.0,"count":3,"sum":9.0}    

## Roles Interface
The `Roles` interface provides an abstraction for determining user role membership. It is provided primarily to facilitate unit testing of service methods. It defines a single `isUserInRole()` method that testing frameworks can implement to simulate different user roles. However, a complete discussion of unit testing services is beyond the scope of this document.

## ResultSetAdapter Class
The `ResultSetAdapter` class allows the result of a SQL query to be efficiently returned from a an RPC method. This class implements the `List` interface and makes each row in a JDBC result set appear as an instance of `Map`, rendering the data suitable for serialization to JSON. `ResultSetAdapter` also implements the `AutoCloseable` interface and closes the underlying result set when its `close()` method is called, ensuring that database resources are not leaked.

`ResultSetAdapter` is forward-scrolling only; its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a result set to be returned directly to the caller without any intermediate buffering. The caller can simply execute a JDBC query, pass the resulting result set to the `ResultSetAdapter` constructor, and return the adapter instance:

    public ResultSetAdapter getData() throws SQLException {
        Statement statement = connection.createStatement();
   
        return new ResultSetAdapter(statement.executeQuery("select * from some_table"));
    }

# iOS Client
The iOS client implementation of WebRPC enables Cocoa Touch applications to consume web RPC services using a simple callback-based API. It is distributed as a modular framework that includes the following classes, discussed in more detail below:

* `WebRPCService` - invocation proxy for web RPC services
* `Result` - abstract base class for typed results

The iOS client framework can be downloaded [here](https://github.com/gk-brown/WebRPC/releases). iOS 8 or later is required.

## WebRPCService Class
The `WebRPCService` class serves as an invocation proxy for web RPC services. Internally, it uses an instance of `NSURLSession` to issue HTTP requests, and uses the `NSJSONSerialization` class to deserialize response content.

Service proxies are initialized via the `initWithSession:baseURL:` method, which takes an `NSURLSession` instance and the service's base URL as arguments. Method names are appended to this URL during method execution.

Remote methods are invoked by calling either `invoke:resultHandler:` or `invoke:withArguments:resultHandler:` on the service proxy. The first version is a convenience method for calling remote methods that don't take any arguments. The second takes a dictionary of argument values to be passed to the remote method. The first method delegates to the second, passing an empty argument dictionary.

Both versions take a result handler as the final argument. The result handler is a callback that is invoked upon successful completion of the remote method, but also if the method call fails. The callback takes two arguments: a result object and an error object. If the remote method completes successfully, the first argument contains the value returned by the method, or `nil` if the method does not return a value. If the remote method call fails, the second argument will be populated with an instance of `NSError` describing the error that occurred.

Both invocation methods return an instance of `NSURLSessionDataTask` representing the invocation request. This allows an application to monitor the progress of a request or cancel it, if necessary.

Although requests are typically processed on a background thread, result handlers are called on the same operation queue that initially invoked the service method. This is typically the application's main queue, which allows result handlers to update the application's user interface directly, rather than posting a separate update operation to the main queue.

Request security is provided by the the underlying URL session. See the `NSURLSession` documentation for more information.

### Examples
The following code snippet demonstrates how `WebRPCService` can be used to invoke the methods of the hypothetical math service discussed earlier:

    // Configure and create session
    var configuration = NSURLSessionConfiguration.defaultSessionConfiguration()

    var delegateQueue = NSOperationQueue()
    delegateQueue.maxConcurrentOperationCount = 10

    var session = NSURLSession(configuration: configuration, 
        delegate: self, delegateQueue: delegateQueue)

    // Create service and invoke remote methods
    var service = WebRPCService(session: session, baseURL: ViewController.baseURL!)
        
    service.invoke("add", withArguments: ["a": 2, "b": 4]) {(result, error) in
        // result is 6
    }

    service.invoke("addArray", withArguments: ["values": [1, 2, 3, 4]]) {(result, error) in
        // result is 10
    }

    service.invoke("addVarargs", withArguments: ["values": [1, 3, 5, 7, 9]]) {(result, error) in
        // result is 25
    }

## Result Class
`Result` is an abstract base class for typed results. While it is possible for applications to consume and present JSON data directly, it is often preferable to work with more strongly typed objects in code. Using the `Result` class, applications can easily map dictionary data returned by a service method to typed values.

The designated initializer for `Result` is the `initWithDictionary:` method. This method initializes the result instance with the contents of a dictionary object. Internally, it uses key-value coding (KVC) to set the instance's property values by calling `setValuesForKeysWithDictionary:` on itself. As a result, subclasses of `Result` must be KVC-compliant in order to be initialized properly. See the _Key-Value Programming Guide_ for more information on KVC compliance.

For example, the following class might be used to provide a typed version of the statistical data returned by the `getStatistics()` method discussed earlier:

    class Statistics : Result {
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

Note that `initWithDictionary:` does not perform deep initialization. Result classes with nested result properties must override `initWithDictionary:` to properly map the nested values to the appropriate types.

# More Information
For more information, refer to [the wiki](https://github.com/gk-brown/WebRPC/wiki) or [the issue list](https://github.com/gk-brown/WebRPC/issues).
