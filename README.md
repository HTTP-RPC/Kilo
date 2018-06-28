[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc.svg)](http://repo1.maven.org/maven2/org/httprpc/httprpc/)

# Introduction
HTTP-RPC is an open-source framework for implementing REST services in Java. It requires only a servlet container and is distributed as a single JAR file that is less than 30KB in size, making it an ideal choice for applications where a minimal footprint is required.

This guide introduces the HTTP-RPC framework and provides an overview of its key features.

# Feedback
Feedback is welcome and encouraged. Please feel free to [contact me](mailto:gk_brown@icloud.com?subject=HTTP-RPC) with any questions, comments, or suggestions. Also, if you like using HTTP-RPC, please consider [starring](https://github.com/gk-brown/HTTP-RPC/stargazers) it!

# Contents
* [Getting HTTP-RPC](#getting-http-rpc)
* [HTTP-RPC Classes](#http-rpc-classes)
    * [DispatcherServlet](#dispatcher-servlet)
    * [JSONEncoder](#json-encoder)
    * [BeanAdapter](#bean-adapter)
    * [ResultSetAdapter](#result-set-adapter)
    * [IteratorAdapter](#iterator-adapter)
* [Additional Information](#additional-information)

# Getting HTTP-RPC
The HTTP-RPC JAR file can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). It is also available via Maven:

```xml
<dependency>
    <groupId>org.httprpc</groupId>
    <artifactId>httprpc-server</artifactId>
    <version>...</version>
</dependency>
```

HTTP-RPC requires Java 8 or later and a servlet container supporting Servlet specification 3.1 or later.

# HTTP-RPC Classes
HTTP-RPC provides the following classes for implementing REST services:

* `org.httprpc`
    * `DispatcherServlet` - abstract base class for web services
    * `RequestMethod` - annotation that associates an HTTP verb with a service method
    * `ResourcePath` - annotation that associates a resource path with a service method
    * `JSONEncoder` - class that encodes service results as JSON
* `org.httprpc.beans`
    * `BeanAdapter` - adapter class that presents the contents of a Java Bean instance as a map
* `org.httprpc.sql`
    * `ResultSetAdapter` - adapter class that presents the contents of a JDBC result set as an iterable cursor
    * `Parameters` - class for simplifying execution of prepared statements 
* `org.httprpc.util`
    * `IteratorAdapter` - adapter class that presents the contents of an iterator as an iterable cursor

These classes are explained in more detail in the following sections.

## DispatcherServlet
`DispatcherServlet` is an abstract base class for HTTP-based web services. Service operations are defined by adding public methods to a concrete service implementation. 

Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `DispatcherServlet` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as JSON.

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

**IMPORTANT** Service classes must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

### Method Arguments
Method arguments may be any of the following types:

* `String`
* `Byte`/`byte`
* `Short`/`short`
* `Integer`/`int`
* `Long`/`long`
* `Float`/`float`
* `Double`/`double`
* `Boolean`/`boolean`
* `java.util.Date`
* `java.util.time.LocalDate`
* `java.util.time.LocalTime`
* `java.util.time.LocalDateTime`
* `java.util.List`
* `java.net.URL`

`List` arguments represent multi-value parameters. List values are automatically converted to their declared types when possible.

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

### Return Values
Return values are converted to their JSON equivalents as follows:

* `Number`: number
* `Boolean`: true/false
* `CharSequence`: string
* `java.util.Date`: long value representing epoch time in milliseconds
* `java.util.time.LocalDate`: "yyyy-mm-dd"
* `java.util.time.LocalTime`: "hh:mm"
* `java.util.time.LocalDateTime`: "yyyy-mm-ddThh:mm"
* `Iterable`: array
* `java.util.Map`: object

Methods may also return `void` or `Void` to indicate that they do not produce a value.

For example, the following method would produce a JSON object containing three values:

TODO Update example

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

### Exceptions
If an exception is thrown during execution of a method and the response has not yet been committed, the exception message will be returned as plain text in the response body. This allows a service to provide the caller with insight into the cause of the failure.

### Request and Repsonse Properties
`DispatcherServlet` provides the following methods to allow a service to access the request and response objects associated with the current operation:

    protected HttpServletRequest getRequest() { ... }
    protected HttpServletResponse getResponse() { ... }

For example, a service might access the request to get the name of the current user, or use the response to return a custom header.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the return value (if any) will be ignored by `DispatcherServlet`. This allows a service to return content that cannot be easily represented as JSON, such as image data or alternative text formats.

### Path Variables
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

## JSONEncoder
TODO

## BeanAdapter
TODO

## ResultSetAdapter
TODO

### Parameters
TODO

## IteratorAdapter
TODO

# Additional Information
For additional information and examples, see the [wiki](https://github.com/gk-brown/HTTP-RPC/wiki).


