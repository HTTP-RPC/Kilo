# Introduction
HTTP-RPC is an open-source framework for simplifying development of REST applications. It allows developers to access REST-based web services using a convenient, RPC-like metaphor while preserving fundamental REST principles such as statelessness and uniform resource access.

The project currently includes support for consuming web services in Objective-C/Swift, Java (including Android), and JavaScript. It provides a consistent, callback-based API that makes it easy to interact with services regardless of target device or operating system. 

For example, the following code snippets show how the various clients might be used to access a simple web service that returns a friendly greeting:

_Swift_

    serviceProxy.invoke("GET", path: "/hello") { result, error in
        print(result) // Prints "Hello, World!"
    }

_Java_

    serviceProxy.invoke("GET", "/hello", (result, exception) -> {
        System.out.println(result); // Prints "Hello, World!"
    });

_JavaScript_

    serviceProxy.invoke("GET", "/hello", function(result, error) {
        console.log(result); // Prints "Hello, World!"
    });

This guide introduces the HTTP-RPC framework and provides an overview of its key features. For examples and additional information, please see [the wiki](https://github.com/gk-brown/HTTP-RPC/wiki).

# Contents
* [Service Operations](#service-operations)
* [Client Implementations](#client-implementations)
	* [Objective-C/Swift](#objective-cswift)
	* [Java](#java)
	* [JavaScript](#javascript)
* [Additional Information](#additional-information)

# Service Operations
Services are accessed by applying an HTTP verb such as `GET` or `POST` to a target resource. The target is specified by a path representing the name of the resource, and is generally expressed as a noun such as _/calendar_ or _/contacts_. 

Arguments are provided either via the query string or in the request body, like an HTML form. Although services may produce any type of content, results are generally returned as JSON. Operations that do not return a value are also supported.

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

## PUT
The `PUT` method updates existing information on the server. `PUT` arguments are passed in the query string. For example, the following request might be used to modify the end date of a calendar event:

    PUT /calendar?eventID=102&end=2016-06-28T15:30

## DELETE
The `DELETE` method removes information from the server. `DELETE` arguments are passed in the query string. For example, this request might be used to delete a calendar event:

    DELETE /calendar?eventID=102

# Client Implementations
The project currently supports consuming services in Objective-C/Swift, Java, and JavaScript. Each implementation is discussed in more detail below. 

## Objective-C/Swift
The Objective-C/Swift client enables iOS and tvOS applications to consume REST-based web services. It is distributed as a universal framework that contains a single `WSWebServiceProxy` class, discussed in more detail below. 

The iOS and tvOS frameworks can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). They are also available via [CocoaPods](https://cocoapods.org/pods/HTTPRPC). iOS 8 or later or tvOS 10 or later is required.

### WSWebServiceProxy Class
The `WSWebServiceProxy` class serves as a client-side invocation proxy for web services. Internally, it uses an instance of `URLSession` to issue HTTP requests. GET, PUT, and DELETE arguments are submitted via the query string, and `POST` requests are submitted as "multipart/form-data", like an HTML form. 

`JSONSerialization` is used to decode JSON response data, and `UIImage` is used to decode image content. Plain text content is returned as a string. All other content is returned as `Data`.

Service proxies are initialized via `init(session:serverURL:)`, which takes a `URLSession` instance and the service's base URL as arguments. Service operations are initiated by calling the `invoke(_:path:arguments:resultHandler:)` method, which takes the following arguments:

* `method` - the HTTP method to execute
* `path` - the resource path
* `arguments` - a dictionary containing the request arguments as key/value pairs
* `resultHandler` - a callback that will be invoked upon completion of the method

A convenience method is also provided for invoking operations that don't take any arguments. Both variants return an instance of `URLSessionDataTask` representing the invocation request. This allows an application to cancel a task, if necessary.

#### Arguments and Return Values
Arguments may be of any type, and are generally converted to parameter values via the `description` method. However, the following types are given special consideration:

* Instances of `URL` represent binary content. They behave similarly to `<input type="file">` tags in HTML and can only be used with `POST` requests. 
* Arrays represent multi-value parameters and generally behave similarly to `<select multiple>` tags in HTML forms. However, arrays containing URL values are handled like `<input type="file" multiple>` tags in HTML and and can only be used with `POST` requests. 

The result handler is called upon completion of the operation. If the operation completes successfully, the first argument will contain the value returned by the server. If the operation fails, the first argument will be `nil`, and the second argument will be populated with an `Error` instance describing the error that occurred.

Note that, while requests are typically processed on a background thread, result handlers are called on the same operation queue that initially invoked the service method. This is typically the application's main queue, which allows result handlers to update the application's user interface directly, rather than posting a separate update operation to the main queue.

### Authentication
Although it is possible to use the `urlSession(_:didReceive:completionHandler:)` method of the `URLSessionDataDelegate` protocol to authenticate service requests, this method requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `authorization` property can be used to associate a set of user credentials with a proxy instance. This property accepts an instance of `URLCredential` identifying the user. When specified, the credentials are submitted with each service request using basic HTTP authentication.

**IMPORTANT** Since basic authentication transmits the encoded username and password in clear text, it should only be used with secure (i.e. HTTPS) connections.

### Examples
The following code snippet demonstrates how `WSWebServiceProxy` can be used to access the hypothetical math operations discussed earlier:

    // Create service proxy
    let serviceProxy = WSWebServiceProxy(session: URLSession.shared, serverURL: URL(string: "https://localhost:8443")!)
    
    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", path: "/math/sum", arguments: ["a": 2, "b": 4]) { result, error in
        // result is 6
    }

    // Get sum of all values
    serviceProxy.invoke("GET", path: "/math/sum", arguments: ["values": [1, 2, 3, 4]]) { result, error in
        // result is 6
    }

## Java
The Java client enables Java applications (including Android) to consume REST-based web services. It is distributed as a JAR file that contains the following types, discussed in more detail below:

* `WebServiceProxy` - web service invocation proxy
* `WebServiceException` - exception thrown when a service operation returns an error
* `ResultHandler` - callback interface for handling service results

The Java client library can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 8 or later is required.

### WebServiceProxy Class
The `WebServiceProxy` class serves as a client-side invocation proxy for web services. Internally, it uses an instance of `HttpURLConnection` to send and receive data. GET, PUT, and DELETE arguments are submitted via the query string, and `POST` requests are submitted as "multipart/form-data", like an HTML form. 

`WebServiceProxy` deserializes JSON and plain text content automatically, and can be extended to support additional content types. Custom deserialization is discussed in more detail later.

Service proxies are initialized via a constructor that takes the following arguments:

* `serverURL` - an instance of `java.net.URL` representing the base URL of the service
* `executorService` - an instance of `java.util.concurrent.ExecutorService` that is used to  dispatch service requests

Optional connection and read timeout values may also be provided.

Service operations are initiated by calling the `invoke()` method, which takes the following arguments:

* `method` - the HTTP method to execute
* `path` - the resource path
* `arguments` - a map containing the request arguments as key/value pairs
* `resultHandler` - an instance of `ResultHandler` that will be invoked upon completion of the service operation

A convenience method is also provided for invoking operations that don't take any arguments. Both variants return an instance of `java.util.concurrent.Future` representing the invocation request. This object allows a caller to cancel an outstanding request, obtain information about a request that has completed, or block the current thread while waiting for an operation to complete.

#### Arguments and Return Values
Arguments may be of any type, and are generally converted to parameter values via the `toString()` method. However, the following types are given special consideration:

* Instances of `java.net.URL` represent binary content. They behave similarly to `<input type="file">` tags in HTML and can only be used with `POST` requests. 
* Instances of `java.util.List` represent multi-value parameters and generally behave similarly to `<select multiple>` tags in HTML forms. However, lists containing URL values are handled like `<input type="file" multiple>` tags in HTML and and can only be used with `POST` requests. 

The result handler is called upon completion of the operation. `ResultHandler` is a functional interface whose single method, `execute()`, is defined as follows:

    public void execute(V result, Exception exception);

On successful completion, the first argument will contain the value returned by the server. This will typically be an instance of one of the following types or `null`:

* string: `String`
* number: `Number`
* true/false: `Boolean`
* array: `java.util.List`
* object: `java.util.Map`

If an error occurs, the first argument will be `null`, and the second will contain an exception representing the error that occurred.

#### Argument Map Creation
Since explicit creation and population of the argument map can be cumbersome, `WebServiceProxy` provides the following static convenience methods to help simplify map creation:

    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) { ... }
    public static <K> Map.Entry<K, ?> entry(K key, Object value) { ... }
    
Using these methods, argument map creation can be reduced from this:

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("a", 2);
    arguments.put("b", 4);
    
to this:

    Map<String, Object> arguments = mapOf(entry("a", 2), entry("b", 4));
    
A complete example is provided later.

#### Nested Structures
`WebServiceProxy` also provides the following static method for accessing nested map values by key path:

    public static <V> V getValue(Map<String, ?> root, String path) { ... }
    
For example, given the following JSON response, a call to `getValue(result, "foo.bar")` would return 123:

    {
        "foo": {
            "bar": 123
        }
    }

#### Custom Deserialization
Subclasses of `WebServiceProxy` can override the `decodeResponse()` method to provide custom deserialization behavior. For example, an Android client could override this method to support `Bitmap` data: 

    @Override
    protected Object decodeResponse(InputStream inputStream, String contentType) throws IOException {
        Object value;
        if (contentType.startsWith("image/")) {
            value = BitmapFactory.decodeStream(inputStream);
        } else {
            value = super.decodeResponse(inputStream, contentType);
        }

        return value;
    }

#### Multi-Threading Considerations
By default, a result handler is called on the thread that executed the remote request. In most cases, this will be a background thread. However, user interface toolkits generally require updates to be performed on the main thread. As a result, handlers typically need to "post" a message back to the UI thread in order to update the application's state. For example, a Swing application might call `SwingUtilities#invokeAndWait()`, whereas an Android application might call `Activity#runOnUiThread()` or `Handler#post()`.

While this can be done in the result handler itself, `WebServiceProxy` provides a more convenient alternative. The protected `dispatchResult()` method can be overridden to process all result handler notifications. For example, the following Android-specific code ensures that all result handlers will be executed on the main UI thread:

    WebServiceProxy serviceProxy = new WebServiceProxy(serverURL, executorService) {
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        protected void dispatchResult(Runnable command) {
            handler.post(command);
        }
    };

Similar dispatchers can be configured for other Java UI toolkits such as Swing, JavaFX, and SWT. Command line applications can generally use the default dispatcher, which simply performs result handler notifications on the current thread.

### Authentication
Although it is possible to use the `java.net.Authenticator` class to authenticate service requests, this class can be difficult to work with, especially when dealing with multiple concurrent requests or authenticating to multiple services with different credentials. It also requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `setAuthorization()` method can be used to associate a set of user credentials with a proxy instance. This method takes an instance of `java.net.PasswordAuthentication` identifying the user. When specified, the credentials are submitted with each service request using basic HTTP authentication.

**IMPORTANT** Since basic authentication transmits the encoded username and password in clear text, it should only be used with secure (i.e. HTTPS) connections.

### Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to access the hypothetical math operations discussed earlier:

    // Create service proxy
    WebServiceProxy serviceProxy = new WebServiceProxy(new URL("https://localhost:8443"), Executors.newSingleThreadExecutor());

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

Note that lambda expressions can optionally be used instead of anonymous inner classes to implement result handlers, reducing the invocation code to the following:

    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("a", 2), entry("b", 4)), (result, exception) -> {
        // result is 6
    });

    // Get sum of all values
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("values", listOf(1, 2, 3))), (result, exception) -> {
        // result is 6
    });

## JavaScript
The JavaScript client enables browser-based applications to consume REST-based web services. It is distributed as a JavaScript source file that contains a single `WebServiceProxy` class, discussed in more detail below. 

The source code for the JavaScript client can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases).

### WebServiceProxy Class
The `WebServiceProxy` class serves as a client-side invocation proxy for web services. Internally, it uses an instance of `XMLHttpRequest` to communicate with the server. GET, PUT, and DELETE arguments are submitted via the query string, and `POST` requests are submitted as "application/x-www-form-urlencoded", like an HTML form. `JSON.parse()` is used to decode response data.

Service proxies are initialized via a constructor that takes a single optional argument representing the request timeout. Service operations are executed by calling the `invoke()` method, which takes the following arguments:

* `method` - the HTTP method to execute
* `path` - the resource path
* `arguments` - an object containing the request arguments as key/value pairs
* `resultHandler` - a callback that will be invoked upon completion of the method

Arguments may be of any type, and are generally converted to parameter values via `toString()`. However, array arguments represent multi-value parameters, and behave similarly to `<select multiple>` tags in HTML forms.

The result handler is called upon completion of the operation. If the operation completes successfully, the first argument will contain the value returned by the server. If the operation fails, the first argument will be `null`, and the second argument will contain the HTTP status code corresponding to the error that occurred.

The `invoke()` method returns the `XMLHttpRequest` instance used to execute the remote call. This allows an application to cancel a request, if necessary.

### Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to access the hypothetical math operations discussed earlier:

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

# Additional Information
For additional information and examples, see the [the wiki](https://github.com/gk-brown/HTTP-RPC/wiki).
