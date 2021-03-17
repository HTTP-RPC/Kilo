[![Releases](https://img.shields.io/github/release/HTTP-RPC/HTTP-RPC.svg)](https://github.com/HTTP-RPC/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc-server.svg)](https://repo1.maven.org/maven2/org/httprpc/httprpc-server/)

# Introduction
HTTP-RPC is an open-source framework for creating and consuming RESTful and REST-like web services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is about 100KB in size, making it an ideal choice for applications where a minimal footprint is desired.

This guide introduces the HTTP-RPC framework and provides an overview of its key features.

# Contents
* [Getting HTTP-RPC](#getting-http-rpc)
* [HTTP-RPC Classes](#http-rpc-classes)
* [Kotlin Support](#kotlin-support)
* [Additional Information](#additional-information)

# Getting HTTP-RPC
HTTP-RPC is distributed via Maven Central: 

* [org.httprpc:httprpc-client](https://repo1.maven.org/maven2/org/httprpc/httprpc-client/) - provides support for consuming web services and interacting with common file formats and relational databases (Java 8 or later required)
* [org.httprpc:httprpc-server](https://repo1.maven.org/maven2/org/httprpc/httprpc-server/) - depends on client; provides support for implementing web services (Java Servlet specification 3.1 or later required)

**NOTE** The legacy `org.httprpc:httprpc` artifact is deprecated. `org.httprpc:httprpc-client` or `org.httprpc:httprpc-server` should be used for new development. 

# HTTP-RPC Classes
Classes provided by the HTTP-RPC framework include:

* [WebService](#webservice) - abstract base class for web services
* [WebServiceProxy](#webserviceproxy) - client-side invocation proxy for web services
* [JSONEncoder and JSONDecoder](#jsonencoder-and-jsondecoder) - encodes/decodes an object hierarchy to/from JSON
* [CSVEncoder and CSVDecoder](#csvencoder-and-csvdecoder) - encodes/decodes an iterable sequence of values to/from CSV
* [TextEncoder and TextDecoder](#textencoder-and-textdecoder) - encodes/decodes text content
* [TemplateEncoder](#templateencoder) - encodes an object hierarchy using a [template document](template-reference.md)
* [BeanAdapter](#beanadapter) - map adapter for Java beans
* [ResultSetAdapter and Parameters](#resultsetadapter-and-parameters) - iterable adapter for JDBC result sets/applies named parameter values to prepared statements
* [QueryBuilder](#querybuilder) - programmatically constructs a SQL query
* [ElementAdapter](#elementadapter) - map adapter for XML elements
* [ResourceBundleAdapter](#resourcebundleadapter) - map adapter for resource bundles
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
* `java.time.Instant` ("yyyy-mm-ddThh:mm:ss[.sss]Z")
* `java.time.LocalDate` ("yyyy-mm-dd")
* `java.time.LocalTime` ("hh:mm")
* `java.time.LocalDateTime` ("yyyy-mm-ddThh:mm")
* `java.net.URL`
* `java.util.List`

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
 
For example, given the preceding request, the key with name "contactID" would be "jsmith" and the key with name "addressType" would be "home".

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
* `java.net.URL`: string
* `Iterable`: array
* `java.util.Map` or Java bean: object

By default, an HTTP 200 response is returned when a service method completes successfully. However, if a method returns `void` or `Void`, an HTTP 204 response will be returned. If a method returns `null`, HTTP 404 will be returned.

#### Custom Result Encodings
Although return values are encoded as JSON by default, subclasses can override the `encodeResult()` method of the `WebService` class to support alternative encodings. See the method documentation for more information.

### Request and Repsonse Properties
`WebService` provides the following methods to allow a service method to access the request and response objects associated with the current invocation:

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
* Any other exception type - HTTP 500 (internal server error)

### API Documentation
API documentation can be viewed by appending "?api" to a service URL; for example:

```
GET /math?api
```

Methods are grouped by resource path. Parameter, body, and return types are encoded as follows:

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
* `java.util.Date`: "date"
* `java.time.Instant`: "instant"
* `java.time.LocalDate`: "date-local"
* `java.time.LocalTime`: "time-local"
* `java.time.LocalDateTime`: "datetime-local"
* `java.net.URL`: "url"
* `java.lang.Iterable`: "[<em>element type</em>]"
* `java.util.Map`: "[<em>key type</em>: <em>value type</em>]"

Any other type is described by its simple class name.

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
public <T> T invoke(Class<? extends T> type) throws IOException { ... }
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
WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL(baseURL, "math/sum"));

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
`WebServiceProxy` also supports a fluent (i.e. chained) invocation model. For example, the following code is equivalent to the previous example:

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
JSONDecoder jsonDecoder = new JSONDecoder();

List<Map<String, Object>> months = jsonDecoder.read(inputStream);

for (Map<String, Object> month : months) {
    System.out.println(String.format("%s has %d days", month.get("name"), month.get("days")));
}
```

## CSVEncoder and CSVDecoder
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

String values are automatically wrapped in double-quotes and escaped. Instances of `java.util.Date` are encoded as a long value representing epoch time. All other values are encoded via `toString()`. 

`CSVDecoder` deserializes a CSV document into an iterable sequence of maps. Rather than loading the entire payload into memory and returning the data as a list, `CSVDecoder` returns the data as a forward-scrolling cursor, allowing consumers to process rows as soon as they are read.

For example, given the CSV above as input, the following code would produce the same results as `JSONDecoder` example:

```java
CSVDecoder csvDecoder = new CSVDecoder();

Iterable<Map<String, String>> months = csvDecoder.read(inputStream);

for (Map<String, String> month : months) {
    System.out.println(String.format("%s has %d days", month.get("name"), month.get("days")));
}
```

Columns with empty headings are ignored. Empty field values are treated as null.

## TextEncoder and TextDecoder
The `TextEncoder` and `TextDecoder` classes can be used to serialize and deserialize plain text content, respectively. For example:

```java
TextEncoder textEncoder = new TextEncoder();

try (FileOutputStream outputStream = new FileOutputStream(file)) {
    textEncoder.write("Hello, World!", outputStream);
}

TextDecoder textDecoder = new TextDecoder();

String text;
try (FileInputStream inputStream = new FileInputStream(file)) {
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
TreeNode root = TreeNode("Seasons", listOf(
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

JSONEncoder jsonEncoder = new JSONEncoder();

jsonEncoder.write(new BeanAdapter(root), System.out);
```

or used as a data dictionary for a template document like this:

```java
TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource("tree.html"));

templateEncoder.write(new BeanAdapter(root), System.out);
```

### Typed Access
`BeanAdapter` can also be used to facilitate type-safe access to loosely typed data structures, such as decoded JSON objects:

```java
public static <T> T adapt(Object value, Type type) { ... }
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
JSONDecoder jsonDecoder = new JSONDecoder();

Map<String, Object> map = jsonDecoder.read(inputStream);

TreeNode root = BeanAdapter.adapt(map, TreeNode.class);

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

## QueryBuilder
The `QueryBuilder` class provides a fluent API for programmatically constructing SQL queries. 

For example, the query from the previous section could be created as follows using `QueryBuilder`:

```java
String sql = QueryBuilder.select("name", "species", "sex", "birth")
    .from("pet")
    .where("owner = :owner").toString();
```

Insert, update, and delete operations are also supported. In general, string values provided to the `insertInto()` and `set()` methods are wrapped in single quotes, and any embdedded single quotes are replaced with two successive single quotes. However, any string that starts with ":" or is equal to "?" is assumed to be a parameter reference and is not escaped. 

If an instance of `QueryBuilder` is passed to `insertInto()` or `set()`, it is considered a subquery and wrapped in parentheses.

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

## ResourceBundleAdapter
The `ResourceBundleAdapter` class provides access to the contents of a resource bundle via the `Map` interface. It can be used to localize the contents of a template document, for example:

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
TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource("list.html"));

ResourceBundle resourceBundle = ResourceBundle.getBundle(getClass().getPackage().getName() + ".headings");

templateEncoder.write(mapOf(
    entry("headings", new ResourceBundleAdapter(resourceBundle)),
    entry("items", items)
), System.out);
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

`Collections` additionally provides the `valueAt()` method, which can be used to access nested values in an object hierarchy. For example:

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

int value = valueAt(map, "a", "b", "c", 1); // 2
``` 

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
This guide introduced the HTTP-RPC framework and provided an overview of its key features. For additional information, see the [examples](https://github.com/HTTP-RPC/HTTP-RPC/tree/master/httprpc-test/src/main/java/org/httprpc/test).
