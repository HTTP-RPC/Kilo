[![Releases](https://img.shields.io/github/release/HTTP-RPC/Kilo.svg)](https://github.com/HTTP-RPC/Kilo/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/kilo-server.svg)](https://repo1.maven.org/maven2/org/httprpc/kilo-server/)

# Introduction
Kilo is an open-source framework for creating and consuming RESTful and REST-like web services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is about 125KB in size, making it an ideal choice for applications where a minimal footprint is desired. 

The project's name comes from the nautical _K_ or _Kilo_ flag, which means "I wish to communicate with you":

![](kilo.png)

This guide introduces the Kilo framework and provides an overview of its key features.

# Contents
* [Getting Kilo](#getting-kilo)
* [Kilo Classes](#kilo-classes)
* [Additional Information](#additional-information)

# Getting Kilo
Kilo is distributed via Maven Central: 

* [org.httprpc:kilo-client](https://repo1.maven.org/maven2/org/httprpc/kilo-client/) - provides support for consuming web services, interacting with relational databases, and working with common file formats (Java 11 or later required)
* [org.httprpc:kilo-server](https://repo1.maven.org/maven2/org/httprpc/kilo-server/) - depends on client; provides support for implementing web services (Java Servlet specification 5.0 or later required)

# Kilo Classes
Classes provided by the Kilo framework include:

* [WebService](#webservice) - abstract base class for web services
* [WebServiceProxy](#webserviceproxy) - client-side invocation proxy for web services
* [JSONEncoder and JSONDecoder](#jsonencoder-and-jsondecoder) - encodes/decodes an object hierarchy to/from JSON
* [CSVEncoder and CSVDecoder](#csvencoder-and-csvdecoder) - encodes/decodes an iterable sequence of values to/from CSV
* [TextEncoder and TextDecoder](#textencoder-and-textdecoder) - encodes/decodes plain text content
* [TemplateEncoder](#templateencoder) - encodes an object hierarchy using a [template document](template-reference.md)
* [BeanAdapter](#beanadapter) - map adapter for Java beans
* [QueryBuilder and ResultSetAdapter](#querybuilder-and-resultsetadapter) - provides a fluent API for programmatically constructing and executing SQL queries/iterable adapter for JDBC result sets
* [ElementAdapter](#elementadapter) - map adapter for XML elements
* [ResourceBundleAdapter](#resourcebundleadapter) - map adapter for resource bundles
* [Collections and Optionals](#collections-and-optionals) - utility methods for working with collections and optional values, respectively

Each is discussed in more detail in the following sections.

## WebService
`WebService` is an abstract base class for web services. It extends the similarly abstract `HttpServlet` class provided by the servlet API. 

Service operations are defined by adding public methods to a concrete service implementation. Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `WebService` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as JSON. Service classes must be compiled with the `-parameters` flag so the names of their method parameters are available at runtime. 

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the servlet. If unspecified, the method is associated with the servlet itself. If no matching handler method is found for a given request, the default handler (e.g. `doGet()`) is called.

Multiple methods may be associated with the same verb and path. `WebService` selects the best method to execute based on the provided argument values. For example, the following service class implements some simple mathematical operations:

```java
@WebServlet(urlPatterns = {"/math/*"}, loadOnStartup = 1)
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

This request would cause the first method to be invoked:

```
GET /math/sum?a=2&b=4
```
 
while this request would invoke the second method:

```
GET /math/sum?values=1&values=2&values=3
```

In either case, the service would return the value 6 in response.

At least one URL pattern is required, and it must be a path mapping (i.e. begin with a leading slash and end with a trailing slash and asterisk). It is recommended that services be configured to load automatically on startup. This ensures that they will be immediately available to [other services](#inter-service-communication) and included in the generated [documentation](#api-documentation).

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
* `java.time.Instant` ("yyyy-mm-ddThh:mm:ss[.sss]Z")
* `java.time.LocalDate` ("yyyy-mm-dd")
* `java.time.LocalTime` ("hh:mm")
* `java.time.LocalDateTime` ("yyyy-mm-ddThh:mm")
* `java.time.Duration`: ISO-8601 duration
* `java.time.Period`: ISO-8601 period
* `java.util.UUID`
* `java.net.URL`
* `java.util.List`

Unspecified values are automatically converted to `0` or `false` for primitive types.

`List` arguments represent multi-value parameters. List values are automatically converted to their declared types (e.g. `List<Double>`).

`URL` and `List<URL>` arguments represent file uploads. They may be used only with `POST` requests submitted using the multi-part form data encoding. For example:

```java
@WebServlet(urlPatterns = {"/upload/*"}, loadOnStartup = 1)
@MultipartConfig
public class FileUploadService extends WebService {
    @RequestMethod("POST")
    public void upload(URL file) throws IOException {
        try (var inputStream = file.openStream()) {
            ...
        }
    }

    @RequestMethod("POST")
    public void upload(List<URL> files) throws IOException {
        for (var file : files) {
            try (var inputStream = file.openStream()) {
                ...
            }
        }
    }
}
```

The methods could be invoked using this HTML form, for example, or by Kilo's `WebServiceProxy` class:

```html
<form action="/upload" method="post" enctype="multipart/form-data">
    <input type="file" name="file"/><br/>
    <input type="file" name="files" multiple/><br/>
    <input type="submit"/><br/>
</form>
```

If an argument value cannot be coerced to the expected type, an HTTP 400 (bad request) response will be returned. If no method is found that matches the provided arguments, an HTTP 405 (method not allowed) response is returned.

### Path Variables
Path variables may be specified by a "?" character in the resource path. For example:

```java
@RequestMethod("GET")
@ResourcePath("contacts/?/addresses/?")
public List<Address> getContactAddresses() { ... }
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
public List<Address> getContactAddresses() { ... }
```

A named variable can be retrieved via this `getKey()` overload:

```java
protected String getKey(String name) { ... }
```
 
For example, given the preceding `GET` request, the value of the key named "contactID" would be "jsmith", and the value of "addressType" would be "home".

#### Typed Access
Although key values are returned as strings by default, they can be easily converted to other types via one of the following overloads:

```java
protected <T> T getKey(int index, Class<T> type) { ... }
protected <T> T getKey(String name, Class<T> type) { ... }
```

For example, this code would return the value of the first path variable as an integer:

```java
int id = getKey(0, Integer.class);
```

### Custom Body Content
The `Content` annotation can be used to associate custom body content with a service method. It defines a single `value()` attribute representing the expected body type. Annotated methods can access the decoded content via the `getBody()` method. 

For example, the following service method might be used to create a new account record using data passed in the request body:

```java
@RequestMethod("POST")
@Content(Account.class)
public createAccount() {
    Account account = getBody();

    ...
}
```

By default, body data is assumed to be JSON. However, subclasses can override the `decodeBody()` method to support other representations. If the provided data cannot be deserialized to the specified type, an HTTP 415 response will be returned.

### Return Values
Return values are converted to their JSON equivalents as follows:

* `CharSequence`: string
* `Number`: number
* `Boolean`: true/false
* `Enum`: string
* `java.util.Date`: number representing epoch time in milliseconds
* `java.time.TemporalAccessor`: string
* `java.time.TemporalAmount`: string
* `java.util.UUID`: string
* `java.net.URL`: string
* `Iterable`: array
* `java.util.Map` or Java bean: object

By default, an HTTP 200 response is returned when a service method completes successfully. However, if a method returns `void` or `Void`, an HTTP 204 response will be returned. If a method returns `null`, HTTP 404 will be returned.

#### Custom Result Encodings
Although return values are encoded as JSON by default, subclasses can override the `encodeResult()` method of the `WebService` class to support alternative encodings. See the method documentation for more information.

### Request and Repsonse Properties
The following methods provide access the request and response objects associated with the current invocation:

```java
protected HttpServletRequest getRequest() { ... }
protected HttpServletResponse getResponse() { ... }
```

For example, a service might use the request to get the name of the current user, or use the response to return a custom header or status code.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the method's return value (if any) will be ignored by `WebService`. This allows a service to return content that cannot be easily represented as JSON, such as image data.

### Authorization
Service requests can be authorized by overriding the following method:

```java
protected boolean isAuthorized(HttpServletRequest request, Method method) { ... }
```

The first argument contains the current request, and the second the service method to be invoked. If `isAuthorized()` returns `true` (the default), method execution will proceed. Otherwise, the method will not be invoked, and an HTTP 403 response will be returned.

### Exceptions
If an exception is thrown by a service method and the response has not yet been committed, the exception message (if any) will be returned as plain text in the response body. Error status will be returned as shown below:

* `IllegalArgumentException` or `UnsupportedOperationException` - HTTP 403 (forbidden)
* `NoSuchElementException` - HTTP 404 (not found)
* `IllegalStateException` - HTTP 409 (conflict)
* Any other exception - HTTP 500 (internal server error)

### Inter-Service Communication
A reference to any service annotated with `jakarta.servlet.annotation.WebServlet` can be obtained via the `getInstance()` method of the `WebService` class. This can be useful when the implementation of one service depends on functionality provided by another service, for example.

### API Documentation
API documentation can be viewed by appending "?api" to a service URL; for example:

```
GET /math?api
```

<img src="README/api.png" width="640px"/>

Methods are grouped by resource path. Implementations can provide additional information about service types and operations using the `Description` annotation. For example:

```java
@WebServlet(urlPatterns = {"/math/*"}, loadOnStartup = 1)
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

The `Description` annotation can also be applied to bean types and properties:

```java
@Description("Represents an item in a product catalog.")
public class Item {
    ...

    @Description("The item's description.")
    public String getDescription() {
        return description;
    }

    @Description("The item's price.")
    public double getPrice() {
        return price;
    }
}
```

as well as enumerated types:

```java
@Description("Represents a size option.")
public enum Size {
    @Description("A small size.")
    SMALL,
    @Description("A medium size.")
    MEDIUM,
    @Description("A large size.")
    LARGE
}
```  

If a method is tagged with the `Deprecated` annotation, it will be identified as such in the output.

The `Keys` annotation can be used to provide descriptions for an endpoint's keys. See the [catalog](https://github.com/HTTP-RPC/Kilo/tree/master/kilo-test/src/main/java/org/httprpc/kilo/test/CatalogService.java) example for more information.

#### IndexServlet
An index of all active services can be enabled by declaring an instance of `org.httprpc.kilo.IndexServlet` in an application's deployment descriptor and mapping it to an appropriate path. For example, the following configuration would make the index available at the application's context root:

```xml
<servlet>
    <servlet-name>index-servlet</servlet-name>
    <servlet-class>org.httprpc.kilo.IndexServlet</servlet-class>
</servlet>

<servlet-mapping>
    <servlet-name>index-servlet</servlet-name>
    <url-pattern/>
</servlet-mapping>
```

## WebServiceProxy
The `WebServiceProxy` class is used to issue API requests to a server. It provides a single constructor that accepts the following arguments:

* `method` - the HTTP method to execute
* `url` - the URL of the requested resource

Request headers and arguments are specified via the `setHeaders()` and `setArguments()` methods, respectively. Custom body content can be provided via the `setBody()` method. When specified, body content is serialized as JSON; however, the `setRequestHandler()` method can be used to facilitate custom request encodings.

Like HTML forms, arguments are submitted either via the query string or in the request body. Arguments for `GET`, `PUT`, and `DELETE` requests are always sent in the query string. `POST` arguments are typically sent in the request body, and may be submitted as either "application/x-www-form-urlencoded" or "multipart/form-data" (specified via the proxy's `setEncoding()` method). However, if a custom body is provided either via `setBody()` or by a custom request handler, `POST` arguments will be sent in the query string.

Any value may be used as an argument. However, `Date` instances are automatically converted to a long value representing epoch time. Additionally, `Iterable` instances represent multi-value parameters and behave similarly to `<select multiple>` tags in HTML. When using the multi-part encoding, instances of `URL` represent file uploads and behave similarly to `<input type="file">` tags in HTML forms.

Service operations are invoked via one of the following methods:

```java
public <T> T invoke() throws IOException { ... }
public <T> T invoke(Type type) throws IOException { ... }
public <T> T invoke(ResponseHandler<T> responseHandler) throws IOException { ... }
```

The first two versions automatically deserialize a successful JSON response (if any). The third allows a caller to provide a custom response handler:

```java
public interface ResponseHandler<T> {
    T decodeResponse(InputStream inputStream, String contentType) throws IOException;
}
```

If a service returns an error response, the default error handler will throw a `WebServiceException`. If the content type of the error response is "text/*", the deserialized response body will be provided in the exception message. A custom error handler can be supplied via the `setErrorHandler()` method.

The following code snippet demonstrates how `WebServiceProxy` might be used to access the operations of the simple math service discussed earlier:

```java
var webServiceProxy = new WebServiceProxy("GET", new URL(baseURL, "math/sum"));

// GET /math/sum?a=2&b=4
webServiceProxy.setArguments(mapOf(
    entry("a", 4),
    entry("b", 2)
));

System.out.println(webServiceProxy.invoke(Double.class)); // 6.0

// GET /math/sum?values=1&values=2&values=3
webServiceProxy.setArguments(mapOf(
    entry("values", listOf(1, 2, 3))
));

System.out.println(webServiceProxy.invoke(Double.class)); // 6.0
```

### Fluent Invocation
`WebServiceProxy` supports a fluent (i.e. chained) invocation model. For example, the following code is equivalent to the previous example:

```java
// GET /math/sum?a=2&b=4
System.out.println(WebServiceProxy.get(baseURL, "math/sum").setArguments(mapOf(
    entry("a", 4),
    entry("b", 2)
)).invoke(Double.class)); // 6.0

// GET /math/sum?values=1&values=2&values=3
System.out.println(WebServiceProxy.get(baseURL, "math/sum").setArguments(mapOf(
    entry("values", listOf(1, 2, 3))
)).invoke(Double.class)); // 6.0
```

POST, PUT, and DELETE operations are also supported.

### Monitoring Service Invocations
Service request and response data can be captured by setting the monitor stream on a proxy instance. For example:

```java
List<Integer> result = WebServiceProxy.get(baseURL, "test/fibonacci").setArguments(
    mapOf(
        entry("count", 8)
    )
).setMonitorStream(System.out).invoke(BeanAdapter.typeOf(List.class, Integer.class));
```

This code would produce the following output:

```
GET http://localhost:8080/kilo-test-1.0/test/fibonacci?count=8
HTTP 200
[
  0,
  1,
  1,
  2,
  3,
  5,
  8,
  13
]
```

## JSONEncoder and JSONDecoder
The `JSONEncoder` class is used internally by `WebService` and `WebServiceProxy` to serialize request and response data. However, it can also be used by application code. For example: 

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

var jsonEncoder = new JSONEncoder();

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

Values are converted to their JSON equivalents as described [earlier](#return-values). Unsupported types are treated as `null`.

`JSONDecoder` deserializes a JSON document into a Java object hierarchy. JSON values are mapped to their Java equivalents as follows:

* string: `String`
* number: `Number`
* true/false: `Boolean`
* array: `java.util.List`
* object: `java.util.Map`

For example, given the following document:

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
var jsonDecoder = new JSONDecoder();

List<Map<String, Object>> months = jsonDecoder.read(inputStream);

for (var month : months) {
    System.out.println(String.format("%s has %d days", month.get("name"), month.get("days")));
}
```

## CSVEncoder and CSVDecoder
The `CSVEncoder` class can be used to serialize a sequence of map values to CSV. For example, the following code could be used to export the month/day list from the previous example as CSV. The string values passed to the constructor represent the columns in the output document and the map keys to which those columns correspond:

```java
var csvEncoder = new CSVEncoder(listOf("name", "days"));

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

String values are automatically wrapped in double-quotes and escaped. Instances of `java.util.Date` are encoded as a long value representing epoch time. All other values are encoded via `toString()`. 

`CSVDecoder` deserializes a CSV document into a list of map values. For example, given the preceding CSV as input, the following code would produce the same output as the earlier `JSONDecoder` example:

```java
var csvDecoder = new CSVDecoder();

List<Map<String, String>> months = csvDecoder.read(inputStream);

for (var month : months) {
    System.out.println(String.format("%s has %d days", month.get("name"), month.get("days")));
}
```

Columns with empty headings are ignored. Empty field values are treated as null.

## TextEncoder and TextDecoder
The `TextEncoder` and `TextDecoder` classes can be used to serialize and deserialize plain text content, respectively. For example:

```java
var textEncoder = new TextEncoder();

try (var outputStream = new FileOutputStream(file)) {
    textEncoder.write("Hello, World!", outputStream);
}

var textDecoder = new TextDecoder();

String text;
try (var inputStream = new FileInputStream(file)) {
    text = textDecoder.read(inputStream); // Hello, World!
}
```

## TemplateEncoder
The `TemplateEncoder` class transforms an object hierarchy into an output format using a [template document](template-reference.md). Template syntax is based loosely on the [Mustache](https://mustache.github.io) format and supports most Mustache features. 

`TemplateEncoder` provides the following constructors:

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

var templateEncoder = new TemplateEncoder(getClass().getResource("example.txt"));

templateEncoder.write(map, System.out);
```

If _example.txt_ was written as follows:

```
{{a}}, {{b}}, {{c}}
```

the resulting output would look like this:

```
hello, 123, true
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
The `BeanAdapter` class provides access to the properties of a Java bean instance via the `Map` interface. For example, the following class might be used to represent a node in a hierarchical object graph:

```java
public class TreeNode {
    private String name;
    private List<TreeNode> children;

    public TreeNode(String name, List<TreeNode> children) {
        this.name = name;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public List<TreeNode> getChildren() {
        return children;
    }
}
```

A simple tree structure could be created and serialized to JSON like this:

```java
var root = TreeNode("Seasons", listOf(
    new TreeNode("Winter", listOf(
        new TreeNode("January", null),
        new TreeNode("February", null),
        new TreeNode("March", null)
    )),
    new TreeNode("Spring", listOf(
        new TreeNode("April", null),
        new TreeNode("May", null),
        new TreeNode("June", null)
    )),
    new TreeNode("Summer", listOf(
        new TreeNode("July", null),
        new TreeNode("August", null),
        new TreeNode("September", null)
    )),
    new TreeNode("Fall", listOf(
        new TreeNode("October", null),
        new TreeNode("November", null),
        new TreeNode("December", null)
    ))
));

var jsonEncoder = new JSONEncoder();

jsonEncoder.write(new BeanAdapter(root), System.out);
```

or used as a data dictionary for a template document like this:

```java
var templateEncoder = new TemplateEncoder(getClass().getResource("tree.html"));

templateEncoder.write(new BeanAdapter(root), System.out);
```

### Type Coercion
`BeanAdapter` can also be used to facilitate type-safe access to loosely typed data structures, such as decoded JSON objects:

```java
public static <T> T coerce(Object value, Type type) { ... }
```

For example, given this interface:

```java
public interface TreeNode {
    String getName();
    List<TreeNode> getChildren();
}
```

the following code could be used to translate the JSON data generated by the previous example into a collection of `TreeNode` instances:

```java
var jsonDecoder = new JSONDecoder();

Map<String, Object> map = jsonDecoder.read(inputStream);

TreeNode root = BeanAdapter.coerce(map, TreeNode.class);

System.out.println(root.getName()); // "Seasons"
System.out.println(root.getChildren().get(0).getName()); // "Winter"
System.out.println(root.getChildren().get(0).getChildren().get(0).getName()); // "January"
```

See the class documentation for more information. 

### Custom Property Keys
The `Key` annotation can be used to associate a custom name with a bean property. The provided value will be used in place of the property name when reading or writing property values. For example:

```java
public class Person {
    private String firstName = null;
    
    @Key("first_name")
    public String getFirstName() {
        return firstName;
    }
    
    @Key("first_name")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
```

## QueryBuilder and ResultSetAdapter
The `QueryBuilder` class provides a fluent API for programmatically constructing and executing SQL queries. For example, given the following table from the MySQL sample database:

```sql
create table pet (
  name varchar(20),
  owner varchar(20),
  species varchar(20),
  sex char(1),
  birth date,
  death date
);
```

this code could be used to create a query that returns all columns and rows in the table:

```java
QueryBuilder.select("*").from("pet");
```

The resulting SQL would look like this:

```sql
select * from pet
```

To select only rows associated with a particular owner, the following query could be used:

```java
QueryBuilder.select("*").from("pet").where("owner = :owner");
```

The colon character identifies "owner" as a parameter, or variable. The resulting SQL would look like this:

```sql
select * from pet where owner = ?
```

Parameter values, or arguments, can be passed to `QueryBuilder`'s `executeQuery()` method as shown below:

```java
try (var statement = queryBuilder.prepare(getConnection());
    var results = new ResultSetAdapter(queryBuilder.executeQuery(statement, mapOf(
        entry("owner", owner)
    )))) { 
    for (var result : results) {
        ...
    }
}
```

The `ResultSetAdapter` class provides access to the contents of a JDBC result set via the `Iterable` interface. Individual rows are represented by `Map` instances produced by the adapter's iterator. This approach is well-suited to serializing large amounts of data, as it does not require any intermediate buffering and has very low latency. However, for smaller data sets, the following more concise alternative can be used:

```java
var results = queryBuilder.execute(getConnection(), mapOf(
    entry("owner", owner)
)).getResults();
```

The results could be mapped to a list of `Pet` instances and returned from a service method as follows:

```java
public interface Pet {
    String getName();
    String getOwner();
    String getSpecies();
    String getSex();
    Date getBirth();
    Date getDeath();
}
```

```java
@RequestMethod("GET")
public List<Pet> getPets(String owner) throws SQLException {
    var queryBuilder = QueryBuilder.select("*").from("pet").where("owner = :owner");

    var results = queryBuilder.execute(getConnection(), mapOf(
        entry("owner", owner)
    )).getResults();

    return BeanAdapter.coerceList(results, Pet.class);
}
```

Insert, update, and delete operations are also supported. For example:

```java
// insert into item (description, price) values (?, ?)

QueryBuilder.insertInto("item").values(mapOf(
    entry("description", ":description"),
    entry("price", ":price")
)).execute(getConnection(), mapOf(
    entry("description", item.getDescription()),
    entry("price", item.getPrice())
));
```

```java
// update item set description = ?, price = ? where id = ?

QueryBuilder.update("item").set(mapOf(
    entry("description", ":description"),
    entry("price", ":price")
)).where("id = :itemID").execute(getConnection(), mapOf(
    entry("itemID", itemID),
    entry("description", item.getDescription()),
    entry("price", item.getPrice())
));
```

```java
// delete from item where id = ?

QueryBuilder.deleteFrom("item").where("id = :itemID").execute(getConnection(), mapOf(
    entry("itemID", itemID)
));
```

If an instance of `QueryBuilder` is passed to either `values()` or `set()`, it is considered a subquery and is wrapped in parentheses.

See the [pet](https://github.com/HTTP-RPC/Kilo/tree/master/kilo-test/src/main/java/org/httprpc/kilo/test/PetService.java) or [catalog](https://github.com/HTTP-RPC/Kilo/tree/master/kilo-test/src/main/java/org/httprpc/kilo/test/CatalogService.java) service examples for more information.

## ElementAdapter
The `ElementAdapter` class provides access to the contents of an XML DOM `Element` via the `Map` interface. The resulting map can then be transformed to another representation via a template document or accessed via a typed proxy, as described [earlier](#type-coercion). 

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
var accountAdapter = new ElementAdapter(document.getDocumentElement());

var holder = (Map<String, Object>)accountAdapter.get("holder");

System.out.println(String.format("%s, %s", holder.get("lastName"), holder.get("firstName")));
```

Namespaces are ignored when identifying elements by tag name. However, the namespace URI for an element (when applicable) can be obtained by requesting the value associated with the ":" key.

Attribute values can be obtained by prepending an "@" symbol to the attribute name:

```java
System.out.println(accountAdapter.get("@id")); // "101"
```

A list of sub-elements can be obtained by appending an asterisk to the element name:

```java
var transactions = (Map<String, Object>)accountAdapter.get("transactions");

var credits = (List<Map<String, Object>>)transactions.get("credit*");

for (var credit : credits) {
    ...
}
```

Finally, the text content of an element can be obtained by calling `toString()` on the adapter instance:

```java
System.out.println(credit.get("amount").toString());
System.out.println(credit.get("date").toString());
```

## ResourceBundleAdapter
The `ResourceBundleAdapter` class provides access to the contents of a resource bundle via the `Map` interface. For example, it can be used to localize the contents of a template document:

```html
<table>
    <!-- {{?headings}} -->
    <tr>
        <td>{{name}}</td>
        <td>{{description}}</td>
        <td>{{quantity}}</td>
    </tr>
    <!-- {{/headings}} -->

    <!-- {{#items}} -->
    <tr>
        <td>{{name}}</td>
        <td>{{description}}</td>
        <td>{{quantity}}</td>
    </tr>
    <!-- {{/items}} -->
</table>
```

```java
var templateEncoder = new TemplateEncoder(getClass().getResource("list.html"));

var resourceBundle = ResourceBundle.getBundle(getClass().getPackage().getName() + ".headings");

templateEncoder.write(mapOf(
    entry("headings", new ResourceBundleAdapter(resourceBundle)),
    entry("items", items)
), System.out);
```

## Collections and Optionals
The `Collections` class provides a set of static utility methods for instantiating immutable list and map values:

```java
public static <E> List<E> listOf(E... elements) { ... }
public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) { ... }
public static <K, V> Map.Entry<K, V> entry(K key, V value) { ... }
```

These methods offer an alternative to similar methods defined by the `List` and `Map` interfaces, which do not permit `null` values.

Additionally, `Collections` includes the following methods for creating empty lists and maps:

```java
public static <E> List<E> emptyListOf(Class<E> elementType) { ... }
public static <K, V> Map<K, V> emptyMapOf(Class<K> keyType, Class<V> valueType) { ... }
```

These provide a slightly more readable alternative to `java.util.Collections.<Integer>emptyList()` and `java.util.Collections.<String, Integer>emptyMap()`, respectively.

Finally, `Collections` provides the `valueAt()` method, which can be used to access nested values in an object hierarchy. For example:

```java
Map<String, Object> map = mapOf(
    entry("a", mapOf(
        entry("b", mapOf(
            entry("c", listOf(
                1, 2, 3
            ))
        ))
    ))
);

var value = Collections.valueAt(map, "a", "b", "c", 1); // 2
``` 

The `Optionals` class contains methods for working with optional (or "nullable") values:

```java
public static <T> T coalesce(T... values) { ... }
public static <T, U> U map(T value, Function<? super T, ? extends U> mapper) { ... }
```

These methods are provided as a less verbose alternative to similar methods defined by the `Optional` class. For example:

```java
String a = null;
String b = null;

var c = Optional.ofNullable(a).orElse(Optional.ofNullable(b).orElse("xyz")); // xyz
var d = Optionals.coalesce(a, b, "xyz"); // xyz
```

```java
var text = "hello";

var i = Optional.ofNullable(text).map(String::length).orElse(null); // 5
var j = Optionals.map(text, String::length); // 5
```

# Additional Information
This guide introduced the Kilo framework and provided an overview of its key features. For additional information, see the [examples](https://github.com/HTTP-RPC/Kilo/tree/master/kilo-test/src/main/java/org/httprpc/kilo/test).
