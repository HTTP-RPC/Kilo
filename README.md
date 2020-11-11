[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc-server.svg)](https://repo1.maven.org/maven2/org/httprpc/httprpc-server/)

# Introduction
HTTP-RPC is an open-source framework for creating and consuming RESTful and REST-like web services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is less than 90KB in size, making it an ideal choice for applications where a minimal footprint is desired.

This guide introduces the HTTP-RPC framework and provides an overview of its key features.

# Contents
* [Getting HTTP-RPC](#getting-http-rpc)
* [HTTP-RPC Classes](#http-rpc-classes)
* [Kotlin Support](#kotlin-support)
* [Additional Information](#additional-information)

# Getting HTTP-RPC
HTTP-RPC is distributed via Maven Central: 

* [org.httprpc:httprpc-client](https://repo1.maven.org/maven2/org/httprpc/httprpc-client/) - provides support for consuming web services and interacting with common file formats and relational databases
* [org.httprpc:httprpc-server](https://repo1.maven.org/maven2/org/httprpc/httprpc-server/) - depends on client; provides support for implementing web services

**NOTE** The legacy `org.httprpc:httprpc` artifact is deprecated. `org.httprpc:httprpc-client` or `org.httprpc:httprpc-server` should be used for new development. 

Java 8 or later is required. Web service support requires a servlet container supporting Java Servlet specification 3.1 or later.

# HTTP-RPC Classes
Classes provided by the HTTP-RPC framework include:

* [WebService](#webservice) - abstract base class for web services
* [WebServiceProxy](#webserviceproxy) - client-side invocation proxy for web services
* [JSONEncoder and JSONDecoder](#jsonencoder-and-jsondecoder) - encodes/decodes an object hierarchy to/from JSON
* [CSVEncoder and CSVDecoder](#csvencoder-and-csvdecoder) - encodes/decodes an iterable sequence of values to/from CSV
* [TemplateEncoder](#templateencoder) - encodes an object hierarchy using a template document
* [BeanAdapter](#beanadapter) - map adapter for Java beans
* [ResultSetAdapter and Parameters](#resultsetadapter-and-parameters) - iterable adapter for JDBC result sets/applies named parameter values to prepared statements
* [ElementAdapter](#elementadapter) - map adapter for XML elements
* [StreamAdapter](#streamadapter) - iterable adapter for streams
* [Collections](#collections) - utility methods for working with collections

Each is discussed in more detail in the following sections.

## WebService
`WebService` is an abstract base class for web services. It extends the similarly abstract `HttpServlet` class provided by the servlet API. 

Service operations are defined by adding public methods to a concrete service implementation. Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `WebService` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as JSON. Service classes must be compiled with the `-parameters` flag so the names of their method parameters are available at runtime. 

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the servlet. If unspecified, the method is associated with the servlet itself. If no matching handler method is found for a given request, the default handler (e.g. `doGet()`) is called.

Multiple methods may be associated with the same verb and path. `WebService` selects the best method to execute based on the provided argument values. For example, the following service class implements some simple addition operations:

```java
@WebServlet(urlPatterns={"/math/*"}, loadOnStartup = 1)
public class MathService extends WebService {
    @RequestMethod("GET")
    @ResourcePath("sum")
    public double getSum(double a, double b) {
        return a + b;
    }
    
    @RequestMethod("GET")
    @ResourcePath("sum")
    public double getSum(List<Double> values) {
        double total = 0;
    
        for (double value : values) {
            total += value;
        }
    
        return total;
    }
}
```

The following HTTP request would cause the first method to be invoked:

```
GET /math/sum?a=2&b=4
```
 
This request would invoke the second method:

```
GET /math/sum?values=1&values=2&values=3
```

In either case, the service would return the value 6 in response.

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
* `java.util.Date` (from a long value representing epoch time in milliseconds)
* `java.util.time.LocalDate` ("yyyy-mm-dd")
* `java.util.time.LocalTime` ("hh:mm")
* `java.util.time.LocalDateTime` ("yyyy-mm-ddThh:mm")
* `java.util.List`
* `java.net.URL`

Missing or `null` values are automatically converted to `0` or `false` for primitive types.

`List` arguments represent multi-value parameters. List values are automatically converted to their declared types (e.g. `List<Double>`).

`URL` and `List<URL>` arguments represent file uploads. They may be used only with `POST` requests submitted using the multi-part form data encoding. For example:

```java
@WebServlet(urlPatterns={"/upload/*"}, loadOnStartup = 1)
@MultipartConfig
public class FileUploadService extends WebService {
    @RequestMethod("POST")
    public void upload(URL file) throws IOException {
        try (InputStream inputStream = file.openStream()) {
            ...
        }
    }

    @RequestMethod("POST")
    public void upload(List<URL> files) throws IOException {
        for (URL file : files) {
            try (InputStream inputStream = file.openStream()) {
                ...
            }
        }
    }
}
```

The methods could be invoked using this HTML form:

```html
<form action="/upload" method="post" enctype="multipart/form-data">
    <input type="file" name="file"/><br/>
    <input type="file" name="files" multiple/><br/>
    <input type="submit"/><br/>
</form>
```

If no method is found that matches the provided arguments, an HTTP 405 response is returned.

### Path Variables
Path variables may be specified by a "?" character in the resource path. For example:

```java
@RequestMethod("GET")
@ResourcePath("contacts/?/addresses/?")
public List<Map<String, ?>> getContactAddresses() { ... }
```

The `getKey()` method returns the value of a path variable associated with the current request:

```java
protected String getKey(int index) { ... }
```
 
For example, given the following request:

```
GET /contacts/jsmith/addresses/home
```

the value of the key at index 0 would be "jsmith", and the value at index 1 would be "home".

#### Named Variables
Path variables can optionally be assigned a name by appending a colon and key name to the "?" character:

```java
@RequestMethod("GET")
@ResourcePath("contacts/?:contactID/addresses/?:addressType")
public List<Map<String, ?>> getContactAddresses() { ... }
```

A named variable can be retrieved via this `getKey()` overload:

```java
protected String getKey(String name) { ... }
```
 
For example, given the preceding request, the key with name "contactID" would be "jsmith" and the key with name "addressType" would be "home".

### Return Values
Return values are converted to their JSON equivalents as follows:

* `CharSequence`: string
* `Number`: number
* `Boolean`: true/false
* `Enum`: ordinal value
* `java.util.Date`: long value representing epoch time in milliseconds
* `java.util.time.LocalDate`: "yyyy-mm-dd"
* `java.util.time.LocalTime`: "hh:mm"
* `java.util.time.LocalDateTime`: "yyyy-mm-ddThh:mm"
* `java.net.URL`: string (external form)
* `Iterable`: array
* `java.util.Map` or Java bean: object

If a method returns `void` or `Void`, an HTTP 204 response will be returned to the caller. If a method returns `null`, an HTTP 404 response will be returned.

#### Custom Result Encodings
Although return values are encoded as JSON by default, subclasses can override the `encodeResult()` method of the `WebService` class to provide a custom encoding. See the method documentation for more information.

### Request and Repsonse Properties
`WebService` provides the following methods to allow a service method to access the request and response objects associated with the current invocation:

```java
protected HttpServletRequest getRequest() { ... }
protected HttpServletResponse getResponse() { ... }
```

For example, a service might use the request to get the name of the current user, or use the response to return a custom header.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the method's return value (if any) will be ignored by `WebService`. This allows a service to return content that cannot be easily represented as JSON, such as image data.

### Authorization
Service requests can be authorized by overriding the following method:

```java
protected boolean isAuthorized(HttpServletRequest request, Method method) { ... }
```

The first argument contains the current request, and the second the service method to be invoked. If `isAuthorized()` returns `true` (the default), method execution will proceed. Otherwise, the method will not be invoked, and an HTTP 403 response will be returned.

### Exceptions
If an exception is thrown by a service method and the response has not yet been committed, the exception message (if any) will be returned as plain text in the response body. If the exception is an instance of `IllegalArgumentException` or `UnsupportedOperationException`, an HTTP 403 response will be returned. For `IllegalStateException`, HTTP 409 will be returned. For any other exception type, HTTP 500 will be returned. 

### Inter-Service Communication
A service implementation can obtain a reference to another service instance via the `getService()` method of the `WebService` class. This can be useful when the behavior of one service relies on logic provided by a different service. The target service must be annotated with `javax.servlet.annotation.WebServlet`.

Methods on the target service are executed in the same thread that handled the initial request. However, the servlet request and response values from the source service are not propagated to the target. Any required values from the request must be passed as arguments to the target method; similarly, any information destined for the response must be returned by the method.

### API Documentation
API documentation can be viewed by appending "?api" to a service URL; for example:

```
GET /math?api
```

Methods are grouped by resource path. Parameter and return types are encoded as follows:

* `Object`: "any"
* `Void` or `void`: "void"
* `Byte` or `byte`: "byte"
* `Short` or `short`: "short"
* `Integer` or `int`: "integer"
* `Long` or `long`: "long"
* `Float` or `float`: "float"
* `Double` or `double`: "double"
* Any other `Number`: "number"
* `CharSequence`: "string"
* `Enum`: "enum"
* `java.util.Date`: "date"
* `java.util.time.LocalDate`: "date-local"
* `java.util.time.LocalTime`: "time-local"
* `java.util.time.LocalDateTime`: "datetime-local"
* `java.net.URL`: "file" for parameters, "url" for return values
* `java.lang.Iterable`, `java.util.Collection`, or `java.util.List`: "[<em>element type</em>]"
* `java.util.Map`: "[<em>key type</em>: <em>value type</em>]"
* Any other type: "{property1: <em>property1 type</em>, property2: <em>property2 type</em>, ...}"

Implementations can provide additional information about service types and operations using the `Description` annotation. For example:

```java
@WebServlet(urlPatterns={"/math/*"}, loadOnStartup = 1)
@Description("Math example service.")
public class MathService extends WebService {
    @RequestMethod("GET")
    @ResourcePath("sum")
    @Description("Calculates the sum of two numbers.")
    public double getSum(
        @Description("The first number.") double a, 
        @Description("The second number.") double b
    ) {
        return a + b;
    }
    
    ...
}
```

If a method is tagged with the `Deprecated` annotation, it will be identified as such in the output.

## WebServiceProxy
The `WebServiceProxy` class is used to issue API requests to a server. This class provides a single constructor that accepts the following arguments:

* `method` - the HTTP method to execute
* `url` - the URL of the requested resource

Request headers and arguments are specified via the `setHeaders()` and `setArguments()` methods, respectively. Like HTML forms, arguments are submitted either via the query string or in the request body. Arguments for `GET`, `PUT`, and `DELETE` requests are always sent in the query string. `POST` arguments are typically sent in the request body, and may be submitted as either "application/x-www-form-urlencoded" or "multipart/form-data" (specified via the proxy's `setEncoding()` method). However, if the request body is provided via a custom request handler (specified via the `setRequestHandler()` method), `POST` arguments will be sent in the query string.

The `toString()` method is generally used to convert an argument to its string representation. However, `Date` instances are automatically converted to a long value representing epoch time. Additionally, `Iterable` instances represent multi-value parameters and behave similarly to `<select multiple>` tags in HTML. Further, when using the multi-part encoding, `URL` and `Iterable<URL>` values represent file uploads, and behave similarly to `<input type="file">` tags in HTML forms.

Service operations are invoked via one of the following methods:

```java
public <T> T invoke() throws IOException { ... }
public <T> T invoke(ResponseHandler<T> responseHandler) throws IOException { ... }
```

The first version automatically deserializes a successful server response using `JSONDecoder`, which is discussed in more detail later. The second allows a caller to provide a custom response handler. `ResponseHandler` is a functional interface that is defined as follows:

```java
public interface ResponseHandler<T> {
    T decodeResponse(InputStream inputStream, String contentType, Map<String, String> headers) throws IOException;
}
```

If a service returns an error response, a `WebServiceException` will be thrown. If the content type of the response is "text/plain", the body of the response will be provided in the exception message.

The following code snippet demonstrates how `WebServiceProxy` might be used to access the operations of the simple math service discussed earlier. `listOf()`, `mapOf()`, and `entry()` are static utility methods provided by the `org.httprpc.util.Collections` class that can be used to declaratively create immutable collection instances:

```java
WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(serverURL, "math/sum"));

// GET /math/sum?a=2&b=4
webServiceProxy.setArguments(mapOf(
    entry("a", 4),
    entry("b", 2)
));

System.out.println((Number)webServiceProxy.invoke()); // 6.0

// GET /math/sum?values=1&values=2&values=3
webServiceProxy.setArguments(mapOf(
    entry("values", listOf(1, 2, 3))
));

System.out.println((Number)webServiceProxy.invoke()); // 6.0
```

### Typed Access
The `adapt()` methods of the `WebServiceProxy` class can be used to facilitate type-safe access to web services:

```java
public static <T> T adapt(URL baseURL, Class<T> type) { ... }
public static <T> T adapt(URL baseURL, Class<T> type, Map<String, ?> headers) { ... }
public static <T> T adapt(URL baseURL, Class<T> type, BiFunction<String, URL, WebServiceProxy> factory) { ... }
```

All three versions take a base URL and an interface type as arguments and return an instance of the given type that can be used to invoke service operations. The second version accepts a map of HTTP header values that will be submitted with every service request. The third accepts a callback that is used to produce web service proxy instances. Interface types must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

The `RequestMethod` annotation is used to associate an HTTP verb with an interface method. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the base URL. If unspecified, the method is associated with the base URL itself. If the provided interface type extends the `Map` interface, the `put()` method can be used to supply values for any named path variables.

`POST` requests are always submitted using the multi-part encoding. Return values are handled as described for `WebServiceProxy`, and are automatically coerced to the correct type.

For example, the following interface might be used to model the operations of the math service:

```java
public interface MathService {
    @RequestMethod("GET")
    @ResourcePath("sum")
    double getSum(double a, double b) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("sum")
    double getSum(List<Double> values) throws IOException;
}
```

This code uses the `adapt()` method to create an instance of `MathService`, then invokes the `getSum()` method on the returned instance. The results are identical to the previous example:

```java
MathService mathService = WebServiceProxy.adapt(new URL(serverURL, "math/"), MathService.class);

// GET /math/sum?a=2&b=4
System.out.println(mathService.getSum(4, 2)); // 6.0

// GET /math/sum?values=1&values=2&values=3
System.out.println(mathService.getSum(listOf(1.0, 2.0, 3.0))); // 6.0
```

## JSONEncoder and JSONDecoder
The `JSONEncoder` class is used internally by `WebService` to serialize a service response. However, it can also be used by application code. For example: 

```java
Map<String, Object> map = mapOf(
    entry("vegetables", listOf(
        "carrots", 
        "peas", 
        "potatoes"
    )),
    entry("desserts", listOf(
        "cookies",
        "cake",
        "ice cream"
    ))
);

JSONEncoder jsonEncoder = new JSONEncoder();

jsonEncoder.write(map, System.out);
```

This code would produce the following output:

```json
{
  "vegetables": [
    "carrots",
    "peas",
    "potatoes"
  ],
  "desserts": [
    "cookies",
    "cake",
    "ice cream"
  ]
}
``` 

Values are converted to their JSON equivalents as described earlier. Unsupported types are treated as `null`.

The `JSONDecoder` class (used internally by `WebServiceProxy`) deserializes a JSON document into a Java object hierarchy. JSON values are mapped to their Java equivalents as follows:

* string: `String`
* number: `Number`
* true/false: `Boolean`
* array: `java.util.List`
* object: `java.util.Map`

For example, given the following JSON document:

```json
[
  {
    "name": "January",
    "days": 31
  },
  {
    "name": "February",
    "days": 28
  },
  {
    "name": "March",
    "days": 31
  },
  ...
]
```

`JSONDecoder` could be used to parse the data into a list of maps as shown below:

```java
JSONDecoder jsonDecoder = new JSONDecoder();

List<Map<String, Object>> months = jsonDecoder.read(inputStream);

for (Map<String, Object> month : months) {
    System.out.println(String.format("%s has %d days", month.get("name"), month.get("days")));
}
```

## CSVEncoder and CSVDecoder
Although `WebService` automatically serializes return values as JSON, in some cases it may be preferable to return a CSV (comma-separated value) document instead. Because field keys are specified only at the beginning of the document rather than being duplicated for every record, CSV generally requires less bandwidth than JSON. Additionally, consumers can begin processing CSV as soon as the first record arrives, rather than waiting for the entire document to download.

### CSVEncoder
The `CSVEncoder` class can be used to serialize a sequence of map values to CSV. For example, the following code could be used to export the month/day list from the previous example as CSV. The string values passed to the constructor represent the columns in the output document and the map keys to which those columns correspond:

```java
CSVEncoder csvEncoder = new CSVEncoder(listOf("name", "days"));

csvEncoder.write(months, System.out);
```

This code would produce the following output:

```csv
"name","days"
"January",31
"February",28
"March",31
...
```

String values are automatically wrapped in double-quotes and escaped. Enums are encoded using their ordinal values. Instances of `java.util.Date` are encoded as a long value representing epoch time. All other values are encoded via `toString()`. 

### CSVDecoder
The `CSVDecoder` class deserializes a CSV document into an iterable sequence of maps. Rather than loading the entire payload into memory and returning the data as a list, `CSVDecoder` returns the data as a forward-scrolling cursor, allowing consumers to process rows as soon as they are read.

For example, given the CSV above as input, the following code would produce the same results as `JSONDecoder` example:

```java
CSVDecoder csvDecoder = new CSVDecoder();

Iterable<Map<String, String>> months = csvDecoder.read(inputStream);

for (Map<String, String> month : months) {
    System.out.println(String.format("%s has %d days", month.get("name"), month.get("days")));
}
```

Columns with empty headings are ignored. Empty field values are treated as null.

## TemplateEncoder
The `TemplateEncoder` class transforms an object hierarchy into an output format using a [template document](template-reference.md). It provides the following constructors:

```java
public TemplateEncoder(URL url) { ... }
public TemplateEncoder(URL url, Charset charset) { ... }
```

The first argument specifies the URL of the template document (typically as a resource on the application's classpath). The escape modifier corresponding to the document's extension (if any) will be applied by default. The optional second argument represents the character encoding used by the template document. If unspecified, UTF-8 is assumed.
 
Templates are applied using one of the following methods:

```java
public void write(Object value, OutputStream outputStream) { ... }
public void write(Object value, OutputStream outputStream, Locale locale) { ... }
public void write(Object value, OutputStream outputStream, Locale locale, TimeZone timeZone) { ... }
public void write(Object value, Writer writer) { ... }
public void write(Object value, Writer writer, Locale locale) { ... }
public void write(Object value, Writer writer, Locale locale, TimeZone timeZone) { ... }
```

The first argument represents the value to write (i.e. the data dictionary), and the second the output destination. The optional third and fourth arguments represent the target locale and time zone, respectively. If unspecified, system defaults are used.

For example, the following code snippet applies a template named _example.txt_ to a map instance:

```java
Map<String, Object> map = mapOf(
    entry("a", "hello"),
    entry("b", 123),
    entry("c", true)
);

TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource("example.txt"));

templateEncoder.write(map, System.out);
```

### Custom Modifiers
Modifiers are created by implementing the `TemplateEncoder.Modifier` interface, which defines the following method:

```java
Object apply(Object value, String argument, Locale locale, TimeZone timeZone);
```
 
The first argument to this method represents the value to be modified, and the second is the optional argument value following the "=" character in the modifier string. If an argument is not specified, this value will be `null`. The third argument contains the encoder's locale.

Custom modifiers are added to a template encoder instance via the `getModifiers()` method. For example, the following code creates a modifier that converts values to uppercase:

```java
templateEncoder.getModifiers().put("uppercase", (value, argument, locale, timeZone) -> value.toString().toUpperCase(locale));
```

Note that modifiers must be thread-safe, since they are shared and may be invoked concurrently by multiple encoder instances.

## BeanAdapter
The `BeanAdapter` class provides access to the properties of a Java bean instance via the `Map` interface. If a property value is `null` or an instance of one of the following types, it is returned as is:

* `CharSequence`
* `Number`
* `Boolean`
* `Enum`
* `java.util.Date`
* `java.util.time.LocalDate`
* `java.util.time.LocalTime`
* `java.util.time.LocalDateTime`
* `java.net.URL`

If the value is an instance of `Iterable` or `Map`, it is wrapped in an adapter of the same type that automatically adapts its sub-elements. Otherwise, the value is assumed to be a bean and is wrapped in an instance of `BeanAdapter`.

For example, the following class might be used to represent a node in a hierarchical object graph:

```java
public class TreeNode {
    private String name;

    private List<TreeNode> children = null;

    public TreeNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }
}
```

This class could be used to create a simple hierarchy as shown below:

```java
TreeNode root = new TreeNode("Seasons");

TreeNode winter = new TreeNode("Winter");
winter.setChildren(listOf(new TreeNode("January"), new TreeNode("February"), new TreeNode("March")));

TreeNode spring = new TreeNode("Spring");
spring.setChildren(listOf(new TreeNode("April"), new TreeNode("May"), new TreeNode("June")));

TreeNode summer = new TreeNode("Summer");
summer.setChildren(listOf(new TreeNode("July"), new TreeNode("August"), new TreeNode("September")));

TreeNode fall = new TreeNode("Fall");
fall.setChildren(listOf(new TreeNode("October"), new TreeNode("November"), new TreeNode("December")));

root.setChildren(listOf(winter, spring, summer, fall));
```

The following code could be used to write this tree structure to the console as JSON:

```java
JSONEncoder jsonEncoder = new JSONEncoder();

jsonEncoder.write(new BeanAdapter(root), System.out);
```

producing the following output:

```json
{
  "children": [
    {
      "children": [
        {
          "children": null,
          "name": "January"
        },
        {
          "children": null,
          "name": "February"
        },
        {
          "children": null,
          "name": "March"
        }
      ],
      "name": "Winter"
    },
    ...
  ]
}
```

Note that properties are traversed in alphabetical order rather than the order in which they were declared. Because the original declaration order is not available at runtime, `BeanAdapter` internally sorts properties alphabetically by key. 

### Excluding Values
Any property tagged with the `Ignore` annotation will be excluded from the map. For example:

```java
@Ignore
public int getX() {
    return x;
}
```

This will cause the `get()` method to return `null` for the key "x".

### Typed Access
`BeanAdapter` can also be used to facilitate type-safe access to loosely typed data structures:

```java
public static <T> T adapt(Object value, Type type) { ... }
```

If the value is already an instance of the requested type, it is returned as is. Otherwise:

* If the target type is a number or boolean, the value is parsed or coerced using the appropriate conversion method (e.g. `Integer#valueOf()`). Missing or `null` values are automatically converted to `0` or `false` for primitive types.
* If the target type is a `String`, the value is adapted via its `toString()` method.
* If the target type is `java.util.Date`, the value is parsed or coerced to a long value representing epoch time in milliseconds and then converted to a `Date`. 
* If the target type is `java.util.time.LocalDate`, `java.util.time.LocalTime`, or `java.util.time.LocalDateTime`, the value is converted to a string and parsed using the appropriate `parse()` method.
* If the target type is `java.util.List` or `java.util.Map`, the value is wrapped in an adapter of the same type that automatically adapts its sub-elements.

Otherwise, the target is assumed to be a bean interface, and the value is assumed to be a map. The return value is a proxy implementation of the given interface that maps accessor methods to entries in the map. Property values are adapted as described above. `Object` methods such as `toString()` are delegated to the underlying map.

For example, given the following interface:

```java
public interface TreeNode {
    String getName();
    List<TreeNode> getChildren();
}
```

the `adapt()` method can be used to model the JSON data from the previous section as a collection of `TreeNode` values:

```java
JSONDecoder jsonDecoder = new JSONDecoder();

Map<String, Object> map = jsonDecoder.read(inputStream);

TreeNode root = BeanAdapter.adapt(map, TreeNode.class);

System.out.println(root.getName()); // "Seasons"
System.out.println(root.getChildren().get(0).getName()); // "Winter"
System.out.println(root.getChildren().get(0).getChildren().get(0).getName()); // "January"
```

### Custom Property Keys
The `Key` annotation can be used to associate a custom key with a bean property. This association is bi-directional; when adapting a bean for access via the `Map` interface, the annotation represents the key that will be used to obtain the value from the bean. Conversely, when adapting a map for typed access, it represents the key that will be used to obtain the value from the map. 

For example, when an instance of this class is wrapped in a `BeanAdapter`, the value returned by `getFirstName()` will be accessible via "first_name" rather than "firstName": 

```java
public class Person {
    private String firstName = null;
    
    @Key("first_name")
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
```

Similarly, when a proxy instance of this interface is created by the `adapt()` method, `getFirstName()` will return the value associated with "first_name" in the underlying map, not "firstName":  

```java
public interface Person {
    @Key("first_name")
    String getFirstName();
}
```

## ResultSetAdapter and Parameters
The `ResultSetAdapter` class provides access to the contents of a JDBC result set via the `Iterable` interface. Access to individual rows is provided via the `Map` interface: 

```java
public class ResultSetAdapter implements Iterable<Map<String, Object>>, AutoCloseable { ... }
```

`ResultSetAdapter` also implements `AutoCloseable` and ensures that the underlying result set is closed when the adapter is closed.

For example, the following code could be used to serialize the results of a database query to JSON:

```java
try (ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery())) {
    JSONEncoder jsonEncoder = new JSONEncoder();
    
    jsonEncoder.write(resultSetAdapter, System.out);
}
```

The `Parameters` class is used to simplify execution of prepared statements. It provides a means for executing statements using named parameter values rather than indexed arguments. Parameter names are specified by a leading ":" character. For example:

```sql
select name from pet where owner = :owner
```

Colons within single quotes and occurrences of two successive unquoted colons ("::") are ignored.

The `parse()` method is used to create a `Parameters` instance from a SQL statement. It takes a string or reader containing the SQL text as an argument; for example:

```java
Parameters parameters = Parameters.parse(sql);
```

The `getSQL()` method returns the parsed SQL in standard JDBC syntax:

```sql
select name from pet where owner = ?
```

This value is used to create the actual prepared statement. Arguments values are specified via the `apply()` method:

```java
PreparedStatement statement = connection.prepareStatement(parameters.getSQL());

parameters.apply(statement, mapOf(
  entry("owner", "Gwen")
));
```

Once applied, the statement can be executed:

```java
ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery());    
```

## ElementAdapter
The `ElementAdapter` class provides access to the contents of an XML DOM `Element` via the `Map` interface. The resulting map can then be transformed to another representation via a template document or accessed via a strongly typed interface proxy, as described earlier. 

For example, the following markup might be used to represent the status of a bank account:

```xml
<account id="101">
    <holder>
        <firstName>John</firstName>
        <lastName>Smith</lastName>
    </holder>
    <transactions>
        <credit>
            <amount>100.00</amount>
            <date>10/5/2020</date>
        </credit>
        <credit>
            <amount>50.00</amount>
            <date>10/12/2020</date>
        </credit>
        <debit>
            <amount>25.00</amount>
            <date>10/14/2020</date>
        </debit>
        <credit>
            <amount>75.00</amount>
            <date>10/19/2020</date>
        </credit>
    </transactions>
</account>
```

This code could be used to display the account holder's name:

```java
ElementAdapter accountAdapter = new ElementAdapter(document.getDocumentElement());

Map<String, Object> holder = (Map<String, Object>)accountAdapter.get("holder");

System.out.println(String.format("%s, %s", holder.get("lastName"), holder.get("firstName")));
```

Attribute values can be obtained by prepending an "@" symbol to the attribute name:

```java
System.out.println(accountAdapter.get("@id")); // "101"
```

A list of sub-elements can be obtained by appending an asterisk to the element name:

```java
Map<String, Object> transactions = (Map<String, Object>)accountAdapter.get("transactions");

List<Map<String, Object>> credits = (List<Map<String, Object>>)transactions.get("credit*");

for (Map<String, Object> credit : credits) {
    ...
}
```

Finally, the text content of an element can be obtained by calling `toString()` on the adapter instance:

```java
System.out.println(credit.get("amount").toString());
System.out.println(credit.get("date").toString());
```

## StreamAdapter
The `StreamAdapter` class provides access to the contents of a stream via the `Iterable` interface. For example, it can be used to serialize the result of a stream operation without needing to first collect the results, which could be expensive if the stream is large:

```java
  List<Integer> values = listOf(1, 2, 3);

  JSONEncoder jsonEncoder = new JSONEncoder(true);

  jsonEncoder.write(new StreamAdapter<>(values.stream().map(element -> element * 2)), System.out);
```

`StreamAdapter` also implements `AutoCloseable` and ensures that the underlying stream is closed when the adapter is closed.

## Collections
The `Collections` class provides a set of static utility methods for instantiating immutable list and map values:

```java
public static <E> List<E> listOf(E... elements) { ... }
public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) { ... }
public static <K, V> Map.Entry<K, V> entry(K key, V value) { ... }
```

These methods are provided primarily as a convenience for applications using Java 8. Applications targeting Java 9 and higher can use the standard `List.of()` and `Map.of()` methods provided by the JDK.

# Kotlin Support
In addition to Java, HTTP-RPC web services can be implemented using the [Kotlin](https://kotlinlang.org) programming language. For example, the following service provides some basic information about the host system:

```kotlin
@WebServlet(urlPatterns = ["/system-info/*"], loadOnStartup = 1)
@Description("System info service.")
class SystemInfoService : WebService() {
    class SystemInfo(
        val availableProcessors: Int,
        val freeMemory: Long,
        val totalMemory: Long
    )

    @RequestMethod("GET")
    @Description("Returns system info.")
    fun getSystemInfo(): SystemInfo {
        val runtime = Runtime.getRuntime()

        return SystemInfo(
            runtime.availableProcessors(),
            runtime.freeMemory(),
            runtime.totalMemory()
        )
    }
}
```

A response produced by the service might look like this:

```json
{
  "availableProcessors": 16,
  "freeMemory": 85845656,
  "totalMemory": 134217728
}
```

# Additional Information
This guide introduced the HTTP-RPC framework and provided an overview of its key features. For additional information, see the [examples](https://github.com/gk-brown/HTTP-RPC/tree/master/httprpc-test/src/main/java/org/httprpc/test).
