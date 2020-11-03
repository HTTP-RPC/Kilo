[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc-server.svg)](https://repo1.maven.org/maven2/org/httprpc/httprpc-server/)

# Introduction
HTTP-RPC is an open-source framework for creating and consuming RESTful and REST-like web services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is less than 100KB in size, making it an ideal choice for applications where a minimal footprint is desired.

This guide introduces the HTTP-RPC framework and provides an overview of its key features.

# Contents
* [Getting HTTP-RPC](#getting-http-rpc)
* [HTTP-RPC Classes](#http-rpc-classes)
* [Kotlin Support](#kotlin-support)
* [Additional Information](#additional-information)

# Getting HTTP-RPC
HTTP-RPC is distributed via Maven Central: 

* [org.httprpc:httprpc-client](https://repo1.maven.org/maven2/org/httprpc/httprpc-client/) - provides support for consuming web services and working with JSON/CSV, template documents, and relational databases
* [org.httprpc:httprpc-server](https://repo1.maven.org/maven2/org/httprpc/httprpc-server/) - depends on client; provides support for implementing web services

**NOTE** The `org.httprpc:httprpc` artifact is deprecated. `org.httprpc:httprpc-server` should be used for new development. 

Java 8 or later is required. Web service support requires a servlet container supporting Java Servlet specification 3.1 or later.

# HTTP-RPC Classes
Classes provided by the HTTP-RPC framework include:

* [WebService](#webservice) - abstract base class for web services
* [WebServiceProxy](#webserviceproxy) - client-side invocation proxy for web services
* [JSONEncoder and JSONDecoder](#jsonencoder-and-jsondecoder) - encodes/decodes an object hierarchy to/from JSON
* [CSVEncoder and CSVDecoder](#csvencoder-and-csvdecoder) - encodes/decodes an iterable sequence of values to/from CSV
* [TemplateEncoder](#templateencoder) - encodes an object hierarchy using a template document
* [BeanAdapter](#beanadapter) - presents the properties of a Java bean object as a map and vice versa
* [ResultSetAdapter and Parameters](#resultsetadapter-and-parameters) - presents the contents of a JDBC result set as an iterable sequence of map values/applies named parameter values to prepared statements
* [StreamAdapter](#streamadapter) - presents the contents of a stream as an iterable sequence
* [Collections](#collections) - provides utility methods for working with collections

Each is discussed in more detail in the following sections.

## WebService
`WebService` is an abstract base class for web services. It extends the similarly abstract `HttpServlet` class provided by the servlet API. 

Service operations are defined by adding public methods to a concrete service implementation. Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `WebService` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as [JSON](http://json.org). Service classes must be compiled with the `-parameters` flag so the names of their method parameters are available at runtime. 

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the servlet. If unspecified, the method is associated with the servlet itself. If no matching handler method is found for a given request, the default handler (e.g. `doGet()`) is called.

Multiple methods may be associated with the same verb and path. `WebService` selects the best method to execute based on the provided argument values. For example, the following service class implements some simple addition operations:

```java
@WebServlet(urlPatterns={"/math/*"})
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
@WebServlet(urlPatterns={"/upload/*"})
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
@WebServlet(urlPatterns={"/math/*"})
@Description("Math example service.")
public class MathService extends WebService {
    private static final long serialVersionUID = 0;

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

The first version automatically deserializes a successful server response using `JSONDecoder`. The second allows a caller to provide a custom response handler. `ResponseHandler` is a functional interface that is defined as follows:

```java
public interface ResponseHandler<T> {
    T decodeResponse(InputStream inputStream, String contentType, Map<String, String> headers) throws IOException;
}
```

If a service returns an error response, a `WebServiceException` will be thrown. If the content type of the response is "text/plain", the body of the response will be provided in the exception message.

### Example
The following code snippet demonstrates how `WebServiceProxy` might be used to access the operations of the simple math service discussed earlier:

```java
WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-server/math/sum"));

webServiceProxy.setArguments(mapOf(
    entry("a", 4),
    entry("b", 2)
));

Number result = webServiceProxy.invoke();

System.out.println(result); // 6.0
```

### Typed Access
The `adapt()` methods of the `WebServiceProxy` class can be used to facilitate type-safe access to web services:

```java
public static <T> T adapt(URL baseURL, Class<T> type) { ... }
public static <T> T adapt(URL baseURL, Class<T> type, Map<String, ?> headers) { ... }
public static <T> T adapt(URL baseURL, Class<T> type, BiFunction<String, URL, WebServiceProxy> factory) { ... }
```

All three versions take a base URL and an interface type as arguments and return an instance of the given type that can be used to invoke service operations. The second version accepts a map of HTTP header values that will be submitted with every service request. The third accepts a callback that is used to produce web service proxy instances.

The `RequestMethod` annotation is used to associate an HTTP verb with an interface method. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the base URL. If unspecified, the method is associated with the base URL itself. Named path variables can be supplied by extending the `Map` interface in the service type.

Service adapters must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

`POST` requests are always submitted using the multi-part encoding. Values are returned as described for `WebServiceProxy` and adapted as described [earlier](#beanadapter) based on the method return type.

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
MathService mathService = WebServiceProxy.adapt(new URL("http://localhost:8080/httprpc-test/math/"), MathService.class);

// GET /math/sum?a=2&b=4
System.out.println(mathService.getSum(4, 2)); // 6.0

// GET /math/sum?values=1&values=2&values=3
System.out.println(mathService.getSum(Arrays.asList(1.0, 2.0, 3.0))); // 6.0
```

## JSONEncoder and JSONDecoder
The `JSONEncoder` class is used internally by `WebService` to serialize a service response. However, it can also be used by application code. For example, the following two methods are functionally equivalent:

```java
@RequestMethod("GET")
public List<String> getList() {
    return Arrays.asList("one", "two", "three");
}
```

```java
@RequestMethod("GET")
public void getList() {
    List<String> list = return Arrays.asList("one", "two", "three");

    JSONEncoder jsonEncoder = new JSONEncoder();

    try {
        jsonEncoder.write(list, getResponse().getOutputStream());
    }
}
```

Values are converted to their JSON equivalents as described earlier. Unsupported types are serialized as `null`.

The `JSONDecoder` class (used internally by `WebServiceProxy`) deserializes a JSON document into a Java object hierarchy. JSON values are mapped to their Java equivalents as follows:

* string: `String`
* number: `Number`
* true/false: `Boolean`
* array: `java.util.List`
* object: `java.util.Map`

For example, the following code snippet uses `JSONDecoder` to parse a JSON array containing the first 8 values of the Fibonacci sequence:

```java
JSONDecoder jsonDecoder = new JSONDecoder();

List<Number> fibonacci = jsonDecoder.read(new StringReader("[0, 1, 1, 2, 3, 5, 8, 13]"));

System.out.println(fibonacci.get(4)); // 3
```

## CSVEncoder and CSVDecoder
Although `WebService` automatically serializes return values as JSON, in some cases it may be preferable to return a [CSV](https://tools.ietf.org/html/rfc4180) document instead. Because field keys are specified only at the beginning of the document rather than being duplicated for every record, CSV generally requires less bandwidth than JSON. Additionally, consumers can begin processing CSV as soon as the first record arrives, rather than waiting for the entire document to download.

### CSVEncoder
The `CSVEncoder` class can be used to encode an iterable sequence of map values to CSV. For example, the following JSON document contains a list of objects representing the months of the year and their respective day counts:

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

`JSONDecoder` could be used to parse this document into a list of maps as shown below:

```java
JSONDecoder jsonDecoder = new JSONDecoder();

List<Map<String, Object>> months = jsonDecoder.read(inputStream);
```

`CSVEncoder` could then be used to export the results as CSV. The string values passed to the encoder's constructor represent the columns in the output document (as well as the map keys to which the columns correspond):

```java
CSVEncoder csvEncoder = new CSVEncoder(Arrays.asList("name", "days"));

csvEncoder.write(months, System.out);
```

This code would produce output similar to the following:

```csv
"name","days"
"January",31
"February",28
"March",31
...
```

Column names actually represent "key paths" and can refer to nested map values using dot notation (e.g. "name.first"). This can be useful for encoding hierarchical data structures (such as complex Java beans or MongoDB documents) as CSV.

String values are automatically wrapped in double-quotes and escaped. Enums are encoded using their ordinal values. Instances of `java.util.Date` are encoded as a long value representing epoch time. All other values are encoded via `toString()`. 

### CSVDecoder
The `CSVDecoder` class deserializes a CSV document into an iterable sequence of maps. Rather than loading the entire payload into memory and returning the data as a list, `CSVDecoder` returns a "cursor" over the records in the document. This allows a consumer to process records as they are read, reducing memory consumption and improving throughput.

The following code would perform the reverse conversion (from CSV to JSON):

```java
// Read from CSV
CSVDecoder csvDecoder = new CSVDecoder();

Iterable<Map<String, String>> months = csvDecoder.read(inputStream);

// Write to JSON
JSONEncoder jsonEncoder = new JSONEncoder();

jsonEncoder.write(months, System.out);
```

## TemplateEncoder
The `TemplateEncoder` class transforms an object hierarchy into an output format using a [template document](template-reference.md). It provides the following constructors:

```java
public TemplateEncoder(URL url) { ... }
public TemplateEncoder(URL url, Charset charset) { ... }
```

The first argument specifies the URL of the template document (generally as a resource on the application's classpath). The escape modifier corresponding to the document's extension (if any) will be applied by default.

The optional second argument represents the character encoding used by the template document. If unspecified, UTF-8 is assumed.

The following methods can be used to get and set the optional base name of the resource bundle that will be used to resolve resource references. If unspecified, any resource references will resolve to `null`:

```java
public String getBaseName() { ... }
public void setBaseName(String baseName) { ... }
```

Values can be added to the template context using the following method, which accepts a map of context entries:

```java
public void setContext(Map<String, ?>) { ... }
```
 
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

For example, the following code snippet applies a template named _map.txt_ to the contents of a data dictionary whose values are specified by a hash map:

```java
HashMap<String, Object> map = new HashMap<>();
    
map.put("a", "hello");
map.put("b", 123);
map.put("c", true);

TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("map.txt"), "text/plain");

String result;
try (StringWriter writer = new StringWriter()) {
    encoder.write(map, writer);
    
    result = writer.toString();
}
    
System.out.println(result);
```

If _map.txt_ is defined as follows:

```
a = {{a}}, b = {{b}}, c = {{c}}
```

this code would produce the following output:

```
a = hello, b = 123, c = true
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
The `BeanAdapter` class implements the `Map` interface and exposes any properties defined by a bean as entries in the map, allowing custom data structures to be easily serialized. 

If a property value is `null` or an instance of one of the following types, it is returned as is:

* `CharSequence`
* `Number`
* `Boolean`
* `Enum`
* `java.util.Date`
* `java.util.time.LocalDate`
* `java.util.time.LocalTime`
* `java.util.time.LocalDateTime`
* `java.net.URL`

If a property returns an instance of `Iterable` or `Map`, the value is wrapped in an adapter of the same type that automatically adapts its sub-elements. Otherwise, the value is assumed to be a bean and is wrapped in a `BeanAdapter`.

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
```

A service method that returns a `TreeNode` structure is shown below:

```java
@RequestMethod("GET")
public TreeNode getTree() {
    TreeNode root = new TreeNode("Seasons");

    TreeNode winter = new TreeNode("Winter");
    winter.setChildren(Arrays.asList(new TreeNode("January"), new TreeNode("February"), new TreeNode("March")));

    TreeNode spring = new TreeNode("Spring");
    spring.setChildren(Arrays.asList(new TreeNode("April"), new TreeNode("May"), new TreeNode("June")));

    TreeNode summer = new TreeNode("Summer");
    summer.setChildren(Arrays.asList(new TreeNode("July"), new TreeNode("August"), new TreeNode("September")));

    TreeNode fall = new TreeNode("Fall");
    fall.setChildren(Arrays.asList(new TreeNode("October"), new TreeNode("November"), new TreeNode("December")));

    root.setChildren(Arrays.asList(winter, spring, summer, fall));

    return root;
}
```

`WebService` automatically wraps the return value in a `BeanAdapter` so it can be serialized to JSON. However, the method could also be written (slightly more verbosely) as follows:

```java
@RequestMethod("GET")
public Map<String, ?> getTree() {
    TreeNode root = new TreeNode("Seasons");

    ...

    return new BeanAdapter(root);    
)
```

Although the values are actually stored in the strongly typed properties of the `TreeNode` object, the adapter makes the data appear as a map, producing the following output:

```json
{
  "name": "Seasons",
  "children": [
    {
      "name": "Winter",
      "children": [
        {
          "name": "January",
          "children": null
        },
        {
          "name": "February",
          "children": null
        },
        {
          "name": "March",
          "children": null
        }
      ]
    },
    ...
  ]
}
```

### Custom Property Keys
The `Key` annotation can be used to associate a custom key with a bean property. For example, the following property would appear as "first_name" in the resulting map instead of "firstName":

```java
@Key("first_name")
public String getFirstName() {
    return firstName;
}
```

### Excluding Values
Any property tagged with the `Ignore` annotation will be excluded from the map. For example:

```java
@Ignore
public int getIgnored() {
    return -1;
}
```

A call to `get()` for the key "ignored" would produce `null`.

### Typed Access
`BeanAdapter` can also be used to facilitate type-safe access to deserialized JSON or CSV data. For example, `JSONDecoder` would parse the data returned by the previous example into a collection of map and list values. The `adapt()` method of the `BeanAdapter` class can be used to efficiently map this loosely typed data structure to a strongly typed object hierarchy. This method takes an object and a result type as arguments, and returns an instance of the given type that adapts the underlying value:

```java
public static <T> T adapt(Object value, Type type) { ... }
```

If the value is already an instance of the requested type, it is returned as is. Otherwise:

* If the target type is a number or boolean, the value is parsed or coerced using the appropriate conversion method. Missing or `null` values are automatically converted to `0` or `false` for primitive types.
* If the target type is a `String`, the value is adapted via its `toString()` method.
* If the target type is `java.util.Date`, the value is parsed or coerced to a long value representing epoch time in milliseconds and then converted to a `Date`. 
* If the target type is `java.util.time.LocalDate`, `java.util.time.LocalTime`, or `java.util.time.LocalDateTime`, the value is converted to a string and parsed using the appropriate `parse()` method.
* If the target type is `java.util.List` or `java.util.Map`, the value is wrapped in an adapter of the same type that automatically adapts its sub-elements.

Otherwise, the target is assumed to be a bean interface, and the value is assumed to be a map. The return value is an implementation of the given interface that maps accessor methods to entries in the map. Property values are adapted as described above.

For example, given the following interface:

```java
public interface TreeNode {
    String getName();
    List<TreeNode> getChildren();
}
```

the `adapt()` method can be used to model the preceding result data as a collection of `TreeNode` values:

```java
TreeNode root = BeanAdapter.adapt(map, TreeNode.class);

root.getName(); // "Seasons"
root.getChildren().get(0).getName(); // "Winter"
root.getChildren().get(0).getChildren().get(0).getName(); // "January"
```

## ResultSetAdapter and Parameters
The `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing query results to be efficiently serialized. `ResultSetAdapter` also implements `AutoCloseable` and ensures that the underlying result set is closed when the adapter itself is closed. 

For example:

```java
try (ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery())) {
    JSONEncoder jsonEncoder = new JSONEncoder();
    
    jsonEncoder.write(resultSetAdapter, outputStream);
}
```

The `Parameters` class is used to simplify execution of prepared statements. It provides a means for executing statements using named parameter values rather than indexed arguments. Parameter names are specified by a leading ":" character. For example:

```sql
SELECT * FROM some_table 
WHERE column_a = :a OR column_b = :b OR column_c = COALESCE(:c, 4.0)
```

Colons within single quotes are ignored. For example, this query would search for the literal string "x:y:z":

```sql
SELECT * FROM some_table 
WHERE column_a = 'x:y:z'
```

Occurrences of two successive colons ("::") are also ignored.

The `parse()` method is used to create a `Parameters` instance from a SQL statement. It takes a string or reader containing the SQL text as an argument; for example:

```java
Parameters parameters = Parameters.parse(sql);
```

The `getSQL()` method returns the parsed SQL in standard JDBC syntax:

```sql
SELECT * FROM some_table 
WHERE column_a = ? OR column_b = ? OR column_c = COALESCE(?, 4.0)
```

This value is used to create the actual prepared statement:

```java
PreparedStatement statement = connection.prepareStatement(parameters.getSQL());
```

Arguments values are specified via the `apply()` method (`mapOf()` is a static utility method provided by the `org.httprpc.util.Collections` class):

```java
parameters.apply(statement, mapOf(
  entry("a", "hello"),
  entry("b", 3)
));
```

Once applied, the statement can be executed:

```java
ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery());    
```

A complete example that uses both classes is shown below. It is based on the "pet" table from the MySQL "menagerie" sample database:

```sql
CREATE TABLE pet (
    name VARCHAR(20),
    owner VARCHAR(20),
    species VARCHAR(20), 
    sex CHAR(1), 
    birth DATE, 
    death DATE
);
```

The following service method queries this table to retrieve a list of all pets belonging to a given owner:

```java
@RequestMethod("GET")
public void getPets(String owner, String format) throws SQLException, IOException {
    Parameters parameters = Parameters.parse("SELECT name, species, sex, birth FROM pet WHERE owner = :owner");

    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
        parameters.apply(statement, mapOf(
            entry("owner", owner)
        ));

        try (ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery())) {
            JSONEncoder jsonEncoder = new JSONEncoder();
            
            jsonEncoder.write(resultSetAdapter, getResponse().getOutputStream());
        }
    }
}
```

For example, given this request:

```
GET /pets?owner=Gwen
```

The service would return something like this:

```json
[
  {
    "name": "Claws",
    "species": "cat",
    "sex": "m",
    "birth": 763880400000
  },
  {
    "name": "Chirpy",
    "species": "bird",
    "sex": "f",
    "birth": 905486400000
  },
  {
    "name": "Whistler",
    "species": "bird",
    "sex": null,
    "birth": 881643600000
  }
]
```

### Nested Results
Key paths can be used as column labels to produce nested results. For example, given the following query:

```sql
SELECT first_name as 'name.first', last_name as 'name.last' FROM contact
```

the values of the "first_name" and "last_name" columns would be returned in a nested map structure as shown below:

```json
[
  {
    "name": {
      "first": "...",
      "last": "..."
    }
  },
  ...
]
```

## StreamAdapter
The `StreamAdapter` class presents the contents of a stream as an iterable sequence. This can be used to serialize the result of a stream operation without needing to first collect the results, which may be expensive if the stream is large:

```java
  List<Integer> values = Arrays.asList(1, 2, 3);

  JSONEncoder jsonEncoder = new JSONEncoder(true);

  jsonEncoder.write(new StreamAdapter<>(values.stream().map(element -> element * 2)), writer);
```

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
        val hostName: String,
        val hostAddress: String,
        val availableProcessors: Int,
        val freeMemory: Long,
        val totalMemory: Long
    )

    @RequestMethod("GET")
    @Description("Returns system info.")
    fun getSystemInfo(): SystemInfo {
        val localHost = InetAddress.getLocalHost()
        val runtime = Runtime.getRuntime()

        return SystemInfo(
            localHost.hostName,
            localHost.hostAddress,
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
  "hostName": "vm.local",
  "hostAddress": "192.168.1.12",
  "availableProcessors": 4,
  "freeMemory": 222234120,
  "totalMemory": 257949696
}
```

# Additional Information
This guide introduced the HTTP-RPC framework and provided an overview of its key features. For additional information, see the [examples](https://github.com/gk-brown/HTTP-RPC/tree/master/httprpc-test/src/main/java/org/httprpc/test).
