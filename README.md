[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![CocoaPods](https://img.shields.io/cocoapods/v/HTTPRPC.svg)](https://cocoapods.org/pods/HTTPRPC)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc.svg)](http://repo1.maven.org/maven2/org/httprpc/httprpc/)
[![Discussion](https://badges.gitter.im/gk-brown/HTTP-RPC.svg)](https://gitter.im/HTTP-RPC/Lobby)

# Introduction
HTTP-RPC is an open-source framework for accessing HTTP-based web services using a convenient, RPC-like metaphor. The project currently includes support for consuming services in Objective-C, Swift, and Java, and provides a consistent, callback-based API that makes it easy to interact with services regardless of target platform. An optional library for implementing services in Java is also provided.

For example, the following code snippet shows how a Swift client might access a simple web service that returns a friendly greeting:

```swift
serviceProxy.invoke("GET", path: "/hello") { (result: Any?, error: NSError?) in
    if (error == nil) {
        print(result!) // Prints "Hello, World!"
    }
}
```

In Java, the code might look like this:

```java
serviceProxy.invoke("GET", "/hello", (result, exception) -> {
    if (error == null) {
        System.out.println(result); // Prints "Hello, World!"
    }
});
```

In both cases, the request will be executed asynchronously and the result printed when the call returns.

This guide introduces the HTTP-RPC framework and provides an overview of its key features. For additional information and examples, see the [wiki](https://github.com/gk-brown/HTTP-RPC/wiki).

# Feedback
Feedback is welcome and encouraged. If you have any questions, comments, or suggestions, let me know in the [discussion forum](https://gitter.im/HTTP-RPC/Lobby). You can also contact me directly via [email](mailto:gk_brown@icloud.com?subject=HTTP-RPC).

Also, if you like using HTTP-RPC, please consider [starring](https://github.com/gk-brown/HTTP-RPC/stargazers) it!

# Contents
* [Objective-C/Swift Client](#objective-cswift-client)
* [Java Client](#java-client)
* [Java Server](#java-server)
* [Additional Information](#additional-information)

# Objective-C/Swift Client
The Objective-C/Swift client enables iOS and tvOS applications to consume HTTP-based web services. It is distributed as a universal framework that contains a single `WSWebServiceProxy` class, discussed in more detail below. 

The iOS and tvOS frameworks can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). They are also available via [CocoaPods](https://cocoapods.org/pods/HTTPRPC). Either iOS 10 or tvOS 10 or later is required.

## WSWebServiceProxy Class
The `WSWebServiceProxy` class serves as a client-side invocation proxy for web services. Internally, it uses an instance of `NSURLSession` to issue HTTP requests. 

Service proxies are initialized via the `initWithSession:serverURL:` method, which takes the following arguments:

* `session` - an `NSURLSession` instance that is used to dispatch service requests
* `serverURL` - the base URL of the service

Service operations are initiated by calling one of the following methods:

* `invoke:path:resultHandler:`
* `invoke:path:arguments:resultHandler:`
* `invoke:path:arguments:responseHandler:resultHandler:`

These methods accept the following arguments:

* `method` - the HTTP method to execute
* `path` - the path to the requested resource
* `arguments` - an optional dictionary containing the request arguments as key/value pairs
* `responseHandler` - an optional callback that will be used to decode the server response
* `resultHandler` - a callback that will be invoked upon completion of the method

The methods return an instance of `NSURLSessionTask` representing the invocation request. This allows an application to cancel a task, if necessary.

### Arguments
As with HTML forms, arguments are submitted either via the query string or in the request body. Arguments for `GET` and `DELETE` requests are always sent in the query string. `POST` arguments are always sent in the request body, and may be submitted using either standard W3C URL-encoded or multi-part form encodings or as JSON. `PUT` and `PATCH` arguments may be submitted either as JSON or via the query string.

The request encoding is specified via the `encoding` property of the service proxy instance. HTTP-RPC provides the following constants representing the supported encoding types:

* `WSApplicationXWWWFormURLEncoded`
* `WSMultipartFormData`
* `WSApplicationJSON`

The default value is `WSMultipartFormData`.

Arguments sent via the query string or using one of the form encodings are generally converted to parameter values via the argument's `description` method. However, array instances represent multi-value parameters and behave similarly to `<select multiple>` tags in HTML. Further, when using the multi-part form data encoding, instances of `NSURL` represent file uploads and behave similarly to `<input type="file">` tags in HTML forms. Arrays of URL values operate similarly to `<input type="file" multiple>` tags.

When using the JSON encoding, a single JSON object containing the entire argument dictionary is sent in the request body. The dictionary is converted to JSON using the `NSJSONSerialization` class.

### Return Values
The result handler is called upon completion of the operation. If successful, the first argument will contain a deserialized representation of the content returned by the server. Otherwise, the first argument will be `nil`, and the second argument will be populated with an `NSError` instance describing the problem that occurred.

`WSWebServiceProxy` supports the following response types:

* _application/json_
* _image/*_
* _text/*_

By default, `NSJSONSerialization` is used to decode JSON response data, and `UIImage` is used to decode image content. Text content is returned as a string. Custom deserialization can be implemented via the optional response handler callback. For example, the following code uses Swift's `JSONDecoder` class to return a strongly-typed result value:

```swift
serviceProxy.invoke("GET", path: "/example", arguments: [:], responseHandler: { data, contentType in
    let decoder = JSONDecoder()

    return try? decoder.decode(Example.self, from: data)
}) { (result: Example?, error: NSError?) in
    // Handle result
}
```

Note that, while requests are typically processed on a background thread, result handlers are executed on the queue that initially invoked the service method (usually the application's main queue). This allows result handlers to update the user interface directly, rather than posting a separate update operation to the main queue. Custom response handlers are executed on the request handler queue, before the result handler is invoked.

If the server returns an error response with a content type of "text/plain", the response body will be returned in the localized description of the error parameter. If the response type is "application/json", the response body will be returned as the `userInfo` property of the error object. Otherwise, a default error message will be returned.

### Authentication
Although it is possible to use the `URLSession:didReceiveChallenge:completionHandler:` method of the `NSURLSessionDelegate` protocol to authenticate service requests, this method requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `authorization` property can be used to associate a set of user credentials with a proxy instance. This property accepts an instance of `NSURLCredential` identifying the user. When specified, the credentials are submitted with each request using basic HTTP authentication.

**IMPORTANT** Since basic authentication transmits the encoded username and password in clear text, it should only be used with secure (i.e. HTTPS) connections.

## Example
The following code sample demonstrates how the `WSWebServiceProxy` class might be used to access the operations of a hypothetical math service:

```swift
// Create service proxy
let serviceProxy = WSWebServiceProxy(session: URLSession.shared, serverURL: URL(string: "https://localhost:8443")!)
    
// Get sum of "a" and "b"
serviceProxy.invoke("GET", path: "/math/sum", arguments: ["a": 2, "b": 4]) { (result: Int?, error: NSError?) in
    // result is 6
}

// Get sum of all values
serviceProxy.invoke("GET", path: "/math/sum", arguments: ["values": [1, 2, 3, 4]]) { (result: Int?, error: NSError?) in
    // result is 6
}
```

# Java Client
The Java client enables Java applications (including Android) to consume HTTP-based web services. It is distributed as a JAR file that contains the following types, discussed in more detail below:

* `WebServiceProxy` - web service invocation proxy
* `WebServiceException` - exception generated when a service operation returns an error
* `ResultHandler` - callback interface for handling service results

Additionally, the framework includes two classes, `JSONEncoder` and `JSONDecoder`, that are used internally for processing JSON data. However, these classes are public and may also be used by application code.

The Java client library can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). It is also available via Maven:

```xml
<dependency>
    <groupId>org.httprpc</groupId>
    <artifactId>httprpc</artifactId>
    <version>...</version>
</dependency>
```

In Android Studio:

```
dependencies {
    ...
    compile 'org.httprpc:httprpc:...'
}
```

Java 8 or later is required.

## WebServiceProxy Class
The `WebServiceProxy` class serves as a client-side invocation proxy for web services. Internally, it uses an instance of `HttpURLConnection` to send and receive data. 

Service proxies are initialized via a constructor that takes the following arguments:

* `serverURL` - an instance of `java.net.URL` representing the base URL of the service
* `executorService` - an instance of `java.util.concurrent.ExecutorService` that is used to  dispatch service requests

Service operations are initiated by calling one of the following methods:

* `public <V> Future<V> invoke(String method, String path, ResultHandler<V> resultHandler) { ... }`
* `public <V> Future<V> invoke(String method, String path, Map<String, ?> arguments, ResultHandler<V> resultHandler) { ... }`
* `public <V> Future<V> invoke(String method, String path, Map<String, ?> arguments, ResponseHandler<V> responseHandler, ResultHandler<V> resultHandler) { ... }`

These methods accept the following arguments:

* `method` - the HTTP method to execute
* `path` - the path to the requested resource
* `arguments` - an optional map containing the request arguments as key/value pairs
* `responseHandler` - an optional callback that will be used to decode the server response
* `resultHandler` - an optional callback that will be invoked upon completion of the request

The methods return an instance of `java.util.concurrent.Future` representing the invocation request. This object allows a caller to cancel an outstanding request, obtain information about a request that has completed, or block the current thread while waiting for an operation to complete.

### Arguments
As with HTML forms, arguments are submitted either via the query string or in the request body. Arguments for `GET` and `DELETE` requests are always sent in the query string. `POST` arguments are always sent in the request body, and may be submitted using either standard W3C URL-encoded or multi-part form encodings or as JSON. `PUT` and `PATCH` arguments may be submitted either as JSON or via the query string.

The request encoding is specified via the `setEncoding()` method of the service proxy instance. The `WebServiceProxy` class provides the following constants representing the supported encoding types:

* `APPLICATION_X_WWW_FORM_URLENCODED`
* `MULTIPART_FORM_DATA`
* `APPLICATION_JSON`

The default value is `MULTIPART_FORM_DATA`.

Arguments sent via the query string or using one of the form encodings are generally converted to parameter values via the argument's `toString()` method. However, `Iterable` values (such as lists) represent multi-value parameters and behave similarly to `<select multiple>` tags in HTML. Further, when using the multi-part form data encoding, instances of `java.net.URL` represent file uploads and behave similarly to `<input type="file">` tags in HTML forms. Iterables of URL values operate similarly to `<input type="file" multiple>` tags.

When using the JSON encoding, a single JSON object containing the entire argument map is sent in the request body. Arguments are converted to their JSON equivalents as follows:

* `Number`: number
* `Boolean`: true/false
* `CharSequence`: string
* `Iterable`: array
* `java.util.Map`: object

`Map` implementations must use `String` values for keys. Nested structures are supported, but reference cycles are not permitted.

Note that `PATCH` requests may not be supported by all platforms. For example, `PATCH` works correctly on Android as of SDK 24 but produces a `ProtocolException` in the Oracle Java 8 runtime.

#### Argument Map Creation
Since explicit construction and population of the argument map can be cumbersome, `WebServiceProxy` provides the following static convenience methods to help simplify map creation:

```java
public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) { ... }
public static <K> Map.Entry<K, ?> entry(K key, Object value) { ... }
```

Using these methods, argument map declaration can be reduced from this:

```java
HashMap<String, Object> arguments = new HashMap<>();
arguments.put("a", 2);
arguments.put("b", 4);
```

to this:

```java
mapOf(entry("a", 2), entry("b", 4));
```
 
A convenience method for declaring lists is also provided:

```java
public static List<?> listOf(Object... elements) { ... }
```

### Return Values
The result handler is called upon completion of the operation. `ResultHandler` is a functional interface whose single method, `execute()`, is defined as follows:

```java
public void execute(V result, Exception exception);
```

If successful, the first argument will contain a deserialized representation of the content returned by the server. Otherwise, the first argument will be `null`, and the second argument will contain an exception representing the error that occurred.

`WebServiceProxy` supports the following response types:

* _application/json_
* _image/*_
* _text/*_

JSON values are mapped to their Java equivalents as follows:

* string: `String`
* number: `Number`
* true/false: `Boolean`
* array: `java.util.List`
* object: `java.util.Map`

Image data is decoded via the `decodeImageResponse()` method of the `WebServiceProxy` class. The default implementation throws an `UnsupportedOperationException`. However, subclasses can override this method to provide custom image deserialization behavior. For example, an Android client might override this method to produce `Bitmap` objects:

```java
@Override
protected Object decodeImageResponse(InputStream inputStream, String imageType) {
    return BitmapFactory.decodeStream(inputStream);
}
```

Text data is decoded via the `decodeTextResponse()` method. The default implentation simply returns the text content as a string. Subclasses may override this method to produce alternate representations (for example, loading an XML document into a document object model).

Custom deserialization can also be implemented via the response handler callback. For example, the following code uses the [Jackson](https://github.com/FasterXML/jackson) JSON parser to return a strongly-typed result value:

```java
serviceProxy.invoke("GET", "/example", mapOf(), (inputStream, contentType) -> {
    ObjectMapper objectMapper = new ObjectMapper();

    return objectMapper.readValue(input, Example.class);
}, (Example result, Exception exception) -> {
    // Handle result
});
```

If the server returns an error response with a content type of "text/plain", the response body will be returned in the `message` property of the exception parameter. If the response type is "application/json", the response body will be returned in the `error` property of the exception. Otherwise, a default error message will be returned.

#### Accessing Nested Structures
`WebServiceProxy` provides the following convenience method for accessing nested map values by key path:

```java
public static <V> V valueAt(Map<String, ?> root, String path) { ... }
```

For example, given the following JSON response:

```json
{
    "foo": {
        "bar": 123
    }
}
```

this method could be used to retrieve the value at "foo.bar":

```java
System.out.println(valueAt(result, "foo.bar")); // Prints 123
```

Additionally, `WebServiceProxy` provides the following method to assist in handling `null` values. This method identifies the first non-`null` value in a list of values:

```java
public static <V> V coalesce(V... values) { ... }
```

For example:

```java
System.out.println(coalesce(valueAt(result, "xyz"), "not found")); // Prints "not found"
```

### Multi-Threading Considerations
By default, a result handler is called on the thread that executed the remote request. In most cases, this will be a background thread. However, user interface toolkits generally require updates to be performed on the main thread. As a result, handlers typically need to "post" a message back to the UI thread in order to update the application's state. For example, a Swing application might call `SwingUtilities#invokeAndWait()`, whereas an Android application might call `Activity#runOnUiThread()` or `Handler#post()`.

While this can be done in the result handler itself, `WebServiceProxy` provides a more convenient alternative. The protected `dispatchResult()` method can be overridden to process result handler notifications. For example, the following Android-specific code ensures that all result handlers will be executed on the main UI thread:

```java
WebServiceProxy serviceProxy = new WebServiceProxy(serverURL, executorService) {
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void dispatchResult(Runnable command) {
        handler.post(command);
    }
};
```

Command line applications can generally use the default dispatcher, which simply performs result handler notifications on the current thread.

Note that custom response handlers are executed on the background thread, before `dispatchResult()` is invoked. 

### Authentication
Although it is possible to use the `java.net.Authenticator` class to authenticate service requests, this class can be difficult to work with, especially when dealing with multiple concurrent requests or authenticating to multiple services with different credentials. It also requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `setAuthorization()` method can be used to associate a set of user credentials with a proxy instance. This method takes an instance of `java.net.PasswordAuthentication` identifying the user. When specified, the credentials are submitted with each request using basic HTTP authentication.

**IMPORTANT** Since basic authentication transmits the encoded username and password in clear text, it should only be used with secure (i.e. HTTPS) connections.

## Example
The following code sample demonstrates how the `WebServiceProxy` class might be used to access the operations of a hypothetical math service:

```java
// Create service proxy
WebServiceProxy serviceProxy = new WebServiceProxy(new URL("https://localhost:8443"), Executors.newSingleThreadExecutor());

// Get sum of "a" and "b"
serviceProxy.invoke("GET", "/math/sum", mapOf(entry("a", 2), entry("b", 4)), (result, exception) -> {
    // result is 6
});

// Get sum of all values
serviceProxy.invoke("GET", "/math/sum", mapOf(entry("values", listOf(1, 2, 3))), (result, exception) -> {
    // result is 6
});
```

# Java Server
The optional Java server library allows developers to implement HTTP-based web services in Java. It is distributed as a JAR file containing the following types:

* `DispatcherServlet` - abstract base class for web services
* `RequestMethod` - annotation that associates an HTTP verb with a service method
* `ResourcePath` - annotation that associates a resource path with a service method

The server JAR can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). It is also available via Maven:

```xml
<dependency>
    <groupId>org.httprpc</groupId>
    <artifactId>httprpc-server</artifactId>
    <version>...</version>
</dependency>
```

The Java client library and Java 8 or later are required.

## DispatcherServlet
`DispatcherServlet` is an abstract base class for HTTP-based web services. Service operations are defined by adding public methods to a concrete service implementation. 

Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. Arguments may also be provided as JSON. `DispatcherServlet` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as JSON.

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the servlet. If unspecified, the method is associated with the servlet itself. 

Multiple methods may be associated with the same verb and path. `DispatcherServlet` selects the best method to execute based on the provided argument values. For example, the following class might be used to implement the simple addition operations discussed earlier:

```java
@WebServlet(urlPatterns={"/math/*"})
public class MathServlet extends DispatcherServlet {
    @RequestMethod("GET")
    @ResourcePath("/sum")
    public double getSum(double a, double b) {
        return a + b;
    }
    
    @RequestMethod("GET")
    @ResourcePath("/sum")
    public double getSum(List<Double> values) {
        double total = 0;
    
        for (double value : values) {
            total += value;
        }
    
        return total;
    }
}
```

The following request would cause the first method to be invoked:

    GET /math/sum?a=2&b=4
    
This request would invoke the second method:

    GET /math/sum?values=1&values=2&values=3

In either case, the service would return the value 6 in response.

Note that service classes must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

## Method Arguments
Method arguments may be any of the following types:

* Numeric primitive or wrapper class (e.g. `int` or `Integer`)
* `boolean` or `Boolean`
* `String`
* `java.util.List`
* `java.util.Map`
* `java.net.URL`

List arguments represent either multi-value parameters submitted using one of the W3C form encodings or array structures submitted as JSON. Map arguments represent object structures submitted as JSON, and must use strings for keys. List and map values are automatically converted to their declared types when possible.

`URL` arguments represent file uploads. They may be used only with `POST` requests submitted using the multi-part form data encoding. For example:

```java
@WebServlet(urlPatterns={"/upload/*"})
@MultipartConfig
public class FileUploadServlet extends DispatcherServlet {
    @RequestMethod("POST")
    public void upload(URL file) throws IOException {
        ...
    }

    @RequestMethod("POST")
    public void upload(List<URL> files) throws IOException {
        ...
    }
}
```

## Return Values
Return values are converted to their JSON equivalents as described earlier:

* `Number`: number
* `Boolean`: true/false
* `CharSequence`: string
* `Iterable`: array
* `java.util.Map`: object

Methods may also return `void` or `Void` to indicate that they do not produce a value.

For example, the following method would produce a JSON object containing three values:

```java
@RequestMethod("GET")
public Map<String, ?> getMap() {
    return mapOf(
        entry("text", "Lorem ipsum"),
        entry("number", 123),
        entry("flag", true)
    );
}
```

The service would return the following in response:

```json
{
    "text": "Lorem ipsum",
    "number": 123,
    "flag": true
}
```

## Request and Repsonse Properties
`DispatcherServlet` provides the following methods to allow a service to access the request and response objects associated with the current operation:

    protected HttpServletRequest getRequest() { ... }
    protected HttpServletResponse getResponse() { ... }

For example, a service might access the request to get the name of the current user, or use the response to return a custom header.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the return value (if any) will be ignored by `DispatcherServlet`. This allows a service to return content that cannot be easily represented as JSON, such as image data or alternative text formats.

## Exceptions
If an exception is thrown during execution of a method and the response has not yet been committed, the exception message will be returned as plain text in the response body. This allows a service to provide the caller with insight into the cause of the failure, such as an invalid argument.

## Path Variables
Path variables may be specified by a "?" character in the resource path. For example:

```java
@RequestMethod("GET")
@ResourcePath("/contacts/?/addresses/?")
public List<Map<String, ?>> getContactAddresses() { ... }
```

The `getKey()` method returns the value of a path variable associated with the current request:

```java
protected String getKeys(int index) { ... }
```
 
For example, given the following path:

    /contacts/jsmith/addresses/home

the value of the key at index 0 would be "jsmith", and the value at index 1 would be "home".

# Additional Information
For additional information and examples, see the [wiki](https://github.com/gk-brown/HTTP-RPC/wiki).


