[![Releases](https://img.shields.io/github/release/HTTP-RPC/Kilo.svg)](https://github.com/HTTP-RPC/Kilo/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/kilo-client.svg)](https://repo1.maven.org/maven2/org/httprpc/kilo-client/)
[![javadoc](https://javadoc.io/badge2/org.httprpc/kilo-client/javadoc.svg)](https://javadoc.io/doc/org.httprpc/kilo-client)

# Introduction
Kilo is an open-source framework for creating and consuming RESTful and REST-like web services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is about 150KB in size, making it an ideal choice for applications where a minimal footprint is desired. 

The project's name comes from the nautical _K_ or _Kilo_ flag, which means "I wish to communicate with you":

![](kilo.png)

This guide introduces the Kilo framework and provides an overview of its key features.

# Contents
* [Getting Kilo](#getting-kilo)
* [Kilo Classes](#kilo-classes)
* [Kotlin Support](#kotlin-support)
* [Additional Information](#additional-information)

# Getting Kilo
Kilo is distributed via Maven Central: 

* [org.httprpc:kilo-client](https://repo1.maven.org/maven2/org/httprpc/kilo-client/) - includes support for consuming web services, interacting with relational databases, and working with common file formats (Java 17 or later required)
* [org.httprpc:kilo-server](https://repo1.maven.org/maven2/org/httprpc/kilo-server/) - depends on client; includes support for creating web services (Jakarta Servlet specification 5.0 or later required)

# Kilo Classes
Classes provided by the Kilo framework include:

* [WebService](#webservice)
* [WebServiceProxy](#webserviceproxy)
* [JSONEncoder and JSONDecoder](#jsonencoder-and-jsondecoder)
* [CSVEncoder and CSVDecoder](#csvencoder-and-csvdecoder)
* [TextEncoder and TextDecoder](#textencoder-and-textdecoder)
* [TemplateEncoder](#templateencoder)
* [BeanAdapter](#beanadapter)
* [QueryBuilder and ResultSetAdapter](#querybuilder-and-resultsetadapter)
* [ElementAdapter](#elementadapter)
* [ResourceBundleAdapter](#resourcebundleadapter)
* [Pipe](#pipe)
* [Collections and Optionals](#collections-and-optionals)

Each is discussed in more detail below.

## WebService
`WebService` is an abstract base class for web services. It extends `HttpServlet` and provides a thin, REST-oriented layer on top of the standard [servlet API](https://jakarta.ee/specifications/servlet/5.0/).

Service operations are defined by adding public methods to a concrete service implementation. Methods are invoked by submitting an HTTP request for a path associated with a service instance. Arguments may be provided via the query string, resource path, or request body. `WebService` converts the values to the expected types, invokes the method, and writes the return value (if any) to the output stream as JSON.

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the servlet. If unspecified, the method is associated with the servlet itself.

Multiple methods may be associated with the same verb and path. `WebService` selects the best method to execute based on the provided argument values. For example, the following service class implements some simple mathematical operations:

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

    @RequestMethod("GET")
    @ResourcePath("sum")
    @Description("Calculates the sum of a list of numbers.")
    public double getSum(
        @Description("The numbers to add.") List<Double> values
    ) {
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

Arguments may also be submitted as [form data](https://www.w3.org/TR/2014/REC-html5-20141028/forms.html#attr-fs-enctype). If no matching handler method is found for a given request, the default handler (e.g. `doGet()`) will be called.

At least one URL pattern is required, and it must be a path mapping (i.e. begin with a leading slash and end with a trailing slash and asterisk). It is recommended that services be configured to load automatically on startup. This ensures that they will be immediately available to [other services](#inter-service-communication) and included in the [generated documentation](#api-documentation).

### Method Parameters
Method parameters may be any of the following types:

* `Byte`/`byte`
* `Short`/`short`
* `Integer`/`int`
* `Long`/`long`
* `Float`/`float`
* `Double`/`double`
* `Boolean`/`boolean`
* `Character`/`char`
* `String`
* `java.util.Date`
* `java.time.Instant`
* `java.time.LocalDate`
* `java.time.LocalTime`
* `java.time.LocalDateTime`
* `java.time.Duration`
* `java.time.Period`
* `java.util.UUID`
* `java.util.List`, `java.util.Set`, array/varargs
* `java.net.URL`

Additionally, `java.util.Map`, bean, and record types are supported for [body content](#body-content).

Unspecified values are automatically converted to `0`, `false`, or the null character for primitive types. `Date` values are parsed from a long value representing epoch time in milliseconds. Other values are parsed from their string representations.

`List`, `Set`, and array elements are automatically converted to their declared types. If no values are provided for a list, set, or array parameter, an empty value (not `null`) will be passed to the method.

`URL` parameters represent file uploads. They may be used only with `POST` requests submitted using the multi-part form data encoding. See the [file upload](https://github.com/HTTP-RPC/Kilo/blob/master/kilo-test/src/main/java/org/httprpc/kilo/test/FileUploadService.java) example for more information.

If a provided value cannot be coerced to the expected type, an HTTP 403 (forbidden) response will be returned. If no method is found that matches the provided arguments, HTTP 405 (method not allowed) will be returned.

Note that service classes must be compiled with the `-parameters` flag so that parameter names are available at runtime.

#### Required Parameters
Parameters that must be provided by the caller can be indicated by the `Required` annotation. For example, the following service method accepts a single required `file` argument:

```java
@RequestMethod("POST")
@Description("Uploads a single file.")
@Empty
public long uploadFile(
    @Description("The file to upload.") @Required URL file
) throws IOException {
    ...
}
```

`List`, `Set`, and array parameters are implicitly required, since these values will never be `null` (though they may be empty). For all other parameter types, HTTP 403 will be returned if a required value is not provided.

The `Empty` annotation indicates that the method does not accept a body and is discussed in more detail [later](#body-content).

#### Custom Parameter Names
The `Name` annotation can be used to associate a custom name with a method parameter. For example:

```java
@WebServlet(urlPatterns = {"/members/*"}, loadOnStartup = 1)
public class MemberService extends WebService {
    @RequestMethod("GET")
    public List<Person> getMembers(
        @Name("first_name") String firstName,
        @Name("last_name") String lastName
    ) {
        ...
    }
}
```

This method could be invoked as follows: 

```
GET /members?first_name=foo*&last_name=bar*
```

### Path Variables
Path variables (or "keys") are specified by a "?" character in an endpoint's resource path. For example, the `itemID` argument in the method below is provided by a path variable:

```java
@RequestMethod("GET")
@ResourcePath("items/?")
@Description("Returns detailed information about a specific item.")
public ItemDetail getItem(
    @Description("The item ID.") Integer itemID
) throws SQLException { ... }
```

Path parameters must precede query parameters in the method signature and are implicitly required. Values are mapped to method arguments in declaration order.

### Body Content
Body content may be declared as the final parameter in a `POST` or `PUT` handler. For example, this method accepts an item ID as a path variable and an instance of `ItemDetail` as a body argument:

```java
@RequestMethod("PUT")
@ResourcePath("items/?")
@Description("Updates an item.")
public void updateItem(
    @Description("The item ID.") Integer itemID,
    @Description("The updated item.") ItemDetail item
) throws SQLException { ... }
```

Like path parameters, body parameters are implicitly required. By default, content is assumed to be JSON and is automatically [converted](#type-coercion) to the specified type. However, subclasses can override the `decodeBody()` method to perform custom conversions.

The `Empty` annotation can be used to indicate that a service method does not accept a body. It is only required for empty `POST` or `PUT` requests (`GET` and `DELETE` requests are inherently empty). Handlers for `POST` requests submitted as form data must include this annotation.

### Return Values
Return values are converted to JSON as follows:

* `String`: string
* `Number`/numeric primitive: number
* `Boolean`/`boolean`: boolean
* `java.util.Date`: number representing epoch time in milliseconds
* `Iterable`: array
* `java.util.Map`: object

Additionally, instances of the following types are automatically converted to their string representations:

* `Character`/`char`
* `Enum`
* `java.time.TemporalAccessor`
* `java.time.TemporalAmount`
* `java.util.UUID`
* `java.net.URL`

All other values are assumed to be beans and are serialized as objects.

By default, an HTTP 200 (OK) response is returned when a service method completes successfully. However, if the handler method is annotated with `Creates`, HTTP 201 (created) will be returned instead. If the handler's return type is `void` or `Void`, HTTP 204 (no content) will be returned.

If a service method returns `null`, an HTTP 404 (not found) response will be returned.

#### Custom Result Encodings
Although return values are encoded as JSON by default, subclasses can override the `encodeResult()` method of the `WebService` class to support alternative representations. See the method documentation for more information.

### Request and Repsonse Properties
The following methods provide access to the request and response objects associated with the current invocation:

```java
protected HttpServletRequest getRequest() { ... }
protected HttpServletResponse getResponse() { ... }
```

For example, a service might use the request to get the name of the current user, or use the response to return a custom header.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the method's return value (if any) will be ignored by `WebService`. This allows a service to return content that cannot be easily represented as JSON, such as image data.

### Exceptions
If an exception is thrown by a service method and the response has not yet been committed, the exception message (if any) will be returned as plain text in the response body. Error status is returned as shown below:

* `IllegalArgumentException` or `UnsupportedOperationException` - HTTP 403 (forbidden)
* `NoSuchElementException` - HTTP 404 (not found)
* `IllegalStateException` - HTTP 409 (conflict)
* Any other exception - HTTP 500 (internal server error)

Subclasses can override the `reportError()` method to perform custom error handling.

### Inter-Service Communication
A reference to any active service can be obtained via the `getInstance()` method of the `WebService` class. This can be useful when the implementation of one service depends on functionality provided by another service, for example.

### API Documentation
An index of all active services can be found at the application's context root:

```
GET http://localhost:8080/kilo-test/
```

<img src="README/api-index.png" width="640px"/>

Documentation for a specific service can be viewed by appending "?api" to the service's base URL:

```
GET http://localhost:8080/kilo-test/catalog?api
```

<img src="README/catalog-api.png" width="640px"/>

Endpoints are grouped by resource path. Implementations can provide additional information about service types and operations using the `Description` annotation. For example:

```java
@WebServlet(urlPatterns = {"/catalog/*"}, loadOnStartup = 1)
@Description("Catalog example service.")
public class CatalogService extends AbstractDatabaseService {
    @RequestMethod("GET")
    @ResourcePath("items")
    @Description("Returns a list of all items in the catalog.")
    public List<Item> getItems() throws SQLException {
        ...
    }
    
    ...
}
```

Descriptions can also be associated with bean types, enums, and records:

```java
@Table("item")
@Description("Represents an item in the catalog.")
public interface Item {
    @Name("id")
    @Column("id")
    @PrimaryKey
    @Description("The item's ID.")
    Integer getID();
    void setID(Integer id);

    @Column("description")
    @Index
    @Description("The item's description.")
    @Required
    String getDescription();
    void setDescription(String description);

    @Column("price")
    @Description("The item's price.")
    @Required
    Double getPrice();
    void setPrice(Double price);
}
```

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

```java
@Description("Represents an x/y coordinate pair.")
public record Coordinates(
    @Description("The x-coordinate.") @Required int x,
    @Description("The y-coordinate.") @Required int y
) {
}
```

Types or methods tagged with the `Deprecated` annotation will be identified as such in the output.

#### JSON Documentation
A JSON version of the generated documentation can be obtained by specifying an "Accept" type of "application/json" in the request headers. The response can be used to process an API definition programatically; for example, to generate client-side stub code. 

## WebServiceProxy
The `WebServiceProxy` class is used to submit API requests to a server. It provides the following two constructors:

```java
public WebServiceProxy(String method, URL url) { ... }
public WebServiceProxy(String method, URL baseURL, String path, Object... arguments) throws MalformedURLException { ... }
```

The first version accepts a string representing the HTTP method to execute and the URL of the requested resource. The second accepts the HTTP method, a base URL, and a relative path (as a format string, to which the optional trailing arguments are applied).

Request arguments are specified via a map passed to the `setArguments()` method. Argument values for `GET`, `PUT`, and `DELETE` requests are always sent in the query string. `POST` arguments are typically sent in the request body, and may be submitted as either "application/x-www-form-urlencoded" or "multipart/form-data" (specified via the proxy's `setEncoding()` method).

Any value may be used as an argument and will generally be encoded using its string representation. However, `Date` instances are automatically converted to a long value representing epoch time in milliseconds. Additionally, `Collection` or array instances represent multi-value parameters and behave similarly to `<select multiple>` tags in HTML. When using the multi-part encoding, instances of `URL` represent file uploads and behave similarly to `<input type="file">` tags in HTML forms.

Body content can be provided via the `setBody()` method. By default, it will be serialized as JSON; however, the `setRequestHandler()` method can be used to facilitate arbitrary encodings:

```java
public interface RequestHandler {
    String getContentType();
    void encodeRequest(OutputStream outputStream) throws IOException;
}
```

Service operations are invoked via one of the following methods:

```java
public Object invoke() throws IOException { ... }
public <T> T invoke(Function<Object, ? extends T> resultHandler) throws IOException { ... }
public <T> T invoke(ResponseHandler<T> responseHandler) throws IOException { ... }
```

The first version deserializes a successful JSON response (if any). The second applies a result handler to the deserialized response. The third version allows a caller to provide a custom response handler:

```java
public interface ResponseHandler<T> {
    T decodeResponse(InputStream inputStream, String contentType) throws IOException;
}
```

If a service returns an error response, the default error handler will throw a `WebServiceException` (a subclass of `IOException`). If the content type of the error response is "text/*", the deserialized response body will be provided in the exception message. A custom error handler can be supplied via `setErrorHandler()`:

```java
public interface ErrorHandler {
    void handleResponse(InputStream errorStream, String contentType, int statusCode) throws IOException;
}
```

The following code demonstrates how `WebServiceProxy` might be used to access the operations of the simple math service discussed earlier:

```java
// GET /math/sum?a=2&b=4
var webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/kilo-test/math/sum"));

webServiceProxy.setArguments(mapOf(
    entry("a", 4),
    entry("b", 2)
));

System.out.println(webServiceProxy.invoke()); // 6.0
```

```java
// GET /math/sum?values=1&values=2&values=3
var webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/kilo-test/math/sum"));

webServiceProxy.setArguments(mapOf(
    entry("values", listOf(1, 2, 3))
));

System.out.println(webServiceProxy.invoke()); // 6.0
```

`POST`, `PUT`, and `DELETE` operations are also supported. The `listOf()` and `mapOf()` methods are discussed in more detail [later](#collections-and-optionals).

### Typed Invocation
`WebServiceProxy` additionally provides the following methods to facilitate convenient, type-safe access to web APIs:

```java
public static <T> T of(Class<T> type, URL baseURL) { ... }
public static <T> T of(Class<T> type, URL baseURL, Consumer<WebServiceProxy> initializer) { ... }
```

Both versions return an implementation of a given interface that submits requests to the provided URL. An optional initializer accepted by the second version will be called prior to each service invocation; for example, to apply common request headers.

The `RequestMethod` and `ResourcePath` annotations are used as described [earlier](#webservice) for `WebService`. Proxy methods must include a throws clause that declares `IOException`, so that callers can handle unexpected failures. For example:

```java
public interface MathServiceProxy {
    @RequestMethod("GET")
    @ResourcePath("sum")
    double getSum(double a, double b) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("sum")
    double getSum(List<Double> values) throws IOException;
}
```

```java
var mathServiceProxy = WebServiceProxy.of(MathServiceProxy.class, new URL("http://localhost:8080/kilo-test/math/"));

System.out.println(mathServiceProxy.getSum(4, 2)); // 6.0
System.out.println(mathServiceProxy.getSum(listOf(1.0, 2.0, 3.0))); // 6.0
```

The [`Name`](#custom-parameter-names) and [`Required`](#required-parameters) annotations may also be applied to proxy method parameters. Path variables and body content are handled as described for `WebService`. The `FormData` annotation can be used in conjunction with `Empty` to submit `POST` requests using either the URL or multi-part form encoding.

Note that proxy types must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

## JSONEncoder and JSONDecoder
The `JSONEncoder` class is used internally by `WebService` and `WebServiceProxy` to serialize request and response data. However, it can also be used directly by application logic. For example: 

```java
var map = mapOf(
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

Values are converted to their JSON equivalents as described [earlier](#return-values). Note that Java bean values must first be wrapped in an instance of `BeanAdapter`, which is discussed in more detail [later](#beanadapter). `BeanAdapter` implements the `Map` interface, which allows `JSONEncoder` to serialize the values as JSON objects. `ResultSetAdapter` (also discussed [later](#querybuilder-and-resultsetadapter)) provides a similar capability for JDBC result sets. 

`JSONDecoder` deserializes a JSON document into an object hierarchy. JSON values are mapped to their Java equivalents as follows:

* string: `String`
* number: `Number`
* boolean: `Boolean`
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

var months = (List<Map<String, Object>>)jsonDecoder.read(inputStream);

for (var month : months) {
    System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
}
```

## CSVEncoder and CSVDecoder
The `CSVEncoder` class can be used to serialize a sequence of map values to CSV. For example, the month/day-count list from the previous section could be exported to CSV as shown below. The string values passed to the constructor represent both the columns in the output document and the map keys to which those columns correspond:

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

String values are automatically wrapped in double-quotes and escaped. Instances of `java.util.Date` are encoded as a long value representing epoch time in milliseconds. All other values are encoded via `toString()`. 

`CSVDecoder` deserializes a CSV document into a list of map values. For example, given the preceding document as input, this code would produce the same output as the `JSONDecoder` example:

```java
var csvDecoder = new CSVDecoder();

var months = csvDecoder.read(inputStream);

for (var month : months) {
    System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
}
```

## TextEncoder and TextDecoder
The `TextEncoder` and `TextDecoder` classes can be used to serialize and deserialize plain text content, respectively. For example:

```java
try (var outputStream = new FileOutputStream(file)) {
    var textEncoder = new TextEncoder();
    
    textEncoder.write("Hello, World!", outputStream);
}

String text;
try (var inputStream = new FileInputStream(file)) {
    var textDecoder = new TextDecoder();

    text = textDecoder.read(inputStream);
}

System.out.println(text); // Hello, World!
```

## TemplateEncoder
The `TemplateEncoder` class transforms an object hierarchy into an output format using a [template document](template-reference.md). Template syntax is based loosely on the [Mustache](https://mustache.github.io) specification and supports most Mustache features. 

`TemplateEncoder` provides the following constructors:

```java
public TemplateEncoder(URL url) { ... }
public TemplateEncoder(URL url, ResourceBundle resourceBundle) { ... }
```

Both versions accept an argument specifying the location of the template document (typically as a resource on the application's classpath). The second version additionally accepts an optional resource bundle that, when present, is used to resolve resource markers. 

Templates are applied via one of the following methods:

```java
public void write(Object value, OutputStream outputStream) { ... }
public void write(Object value, OutputStream outputStream, Locale locale) { ... }
public void write(Object value, OutputStream outputStream, Locale locale, TimeZone timeZone) { ... }
public void write(Object value, Writer writer) { ... }
public void write(Object value, Writer writer, Locale locale) { ... }
public void write(Object value, Writer writer, Locale locale, TimeZone timeZone) { ... }
```

The first argument represents the value to write (i.e. the "data dictionary"), and the second the output destination. The optional third and fourth arguments represent the target locale and time zone, respectively. If unspecified, system defaults are used.

For example, this code applies a template named "example.html" to a map instance:

```java
var map = mapOf(
    entry("a", "hello"),
    entry("b", 123),
    entry("c", true)
);

var templateEncoder = new TemplateEncoder(getClass().getResource("example.html"));

templateEncoder.write(map, System.out);
```

### Custom Modifiers
Custom modifiers can be associated with a template encoder instance via the `bind()` method:

```java
public void bind(String name, Modifier modifier) { ... }
```

This method accepts a name for the modifier and an implementation of the `TemplateEncoder.Modifer` interface:

```java
public interface Modifier {
    Object apply(Object value, String argument, Locale locale, TimeZone timeZone);
}
```
 
The first argument to the `apply()` method represents the value to be modified. The second is the optional argument text that follows the "=" character in the modifier string. If an argument is not specified, this value will be `null`. The third argument contains the encoder's locale.

For example, this code creates a modifier named "upper" that converts values to uppercase:

```java
templateEncoder.bind("upper", (value, argument, locale, timeZone) -> value.toString().toUpperCase(locale));
```

The modifier can be applied as shown below:

```
{{.:upper}}
```

## BeanAdapter
The `BeanAdapter` class provides access to Java bean properties via the `Map` interface. For example, the following class might be used to represent a node in a hierarchical object graph:

```java
public class TreeNode {
    private String name;
    private List<TreeNode> children;

    public TreeNode() {
        this(null, null);
    }

    public TreeNode(String name, List<TreeNode> children) {
        this.name = name;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }
}
```

A simple tree structure could be created and serialized to JSON as shown below:

```java
var root = new TreeNode("Seasons", listOf(
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

jsonEncoder.write(new BeanAdapter(root), writer);
```

The resulting output would look something like this (`BeanAdapter` traverses properties in alphabetical order):

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
  ],
  "name": "Seasons"
}
```

### Type Coercion
`BeanAdapter` can also be used to facilitate type-safe access to loosely typed data structures, such as decoded JSON objects:

```java
public static <T> T coerce(Object value, Class<T> type) { ... }
```

For example, the following code could be used to deserialize the JSON produced by the previous example back into a collection of `TreeNode` instances:

```java
var jsonDecoder = new JSONDecoder();

var root = BeanAdapter.coerce(jsonDecoder.read(reader), TreeNode.class);

System.out.println(root.getName()); // Seasons
System.out.println(root.getChildren().get(0).getName()); // Winter
System.out.println(root.getChildren().get(0).getChildren().get(0).getName()); // January
```

Note that an interface can be used instead of a class to provide a strongly typed "view" of the underlying map data. For example:

```java
public interface AssetPricing {
    Instant getDate();
    double getOpen();
    double getHigh();
    double getLow();
    double getClose();
    long getVolume();
}
```

```java
var map = mapOf(
    entry("date", "2024-04-08T00:00:00Z"),
    entry("open", 169.03),
    entry("close", 168.45),
    entry("high", 169.20),
    entry("low", 168.24),
    entry("volume", 37216858)
);

var assetPricing = BeanAdapter.coerce(map, AssetPricing.class);

System.out.println(assetPricing.getDate()); // 2024-04-08T00:00:00Z
System.out.println(assetPricing.getOpen()); // 169.03
System.out.println(assetPricing.getClose()); // 168.45
System.out.println(assetPricing.getHigh()); // 169.2
System.out.println(assetPricing.getLow()); // 168.24
System.out.println(assetPricing.getVolume()); // 37216858
```

Mutator methods are also supported.

### Required Properties
The `Required` annotation introduced [previously](#required-parameters) can also be used to indicate that a property must contain a value. For example:

```java
public class Vehicle {
    private String manufacturer;
    private Integer year;

    @Required
    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Required
    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
```

Because both "manufacturer" and "year" are required, an attempt to coerce an empty map to a `Vehicle` instance would produce an `IllegalArgumentException`:

```java
var vehicle = BeanAdapter.coerce(mapOf(), Vehicle.class); // throws
```

Additionally, although the annotation will not prevent a caller from programmatically assigning a `null` value to either property, attempting to dynamically set an invalid value will generate an `IllegalArgumentException`:

```java
var vehicle = new Vehicle();

var vehicleAdapter = new BeanAdapter(vehicle);

vehicleAdapter.put("manufacturer", null); // throws
```

Similarly, attempting to dynamically access an invalid value will result in an `UnsupportedOperationException`:

```java
vehicleAdapter.get("manufacturer"); // throws
```

### Custom Property Names
The `Name` annotation introduced [previously](#custom-parameter-names) can also be used with properties. For example:

```java
public class Person {
    private String firstName = null;
    private String lastName = null;

    @Name("first_name")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Name("last_name")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
```

The preceding class would be serialized to JSON like this:

```json
{
  "first_name": "John",
  "last_name": "Smith"
}
```

rather than this:

```json
{
  "firstName": "John",
  "lastName": "Smith"
}
```

## QueryBuilder and ResultSetAdapter
The `QueryBuilder` class provides support for programmatically constructing and executing SQL queries. For example, given the following table from the MySQL sample database:

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

this code could be used to create a query that returns all rows associated with a particular owner:

```java
var queryBuilder = new QueryBuilder();

queryBuilder.appendLine("select * from pet where owner = :owner");
```

The colon character identifies "owner" as a parameter, or variable. Parameter values, or arguments, can be passed to `QueryBuilder`'s `executeQuery()` method as shown below:

```java
try (var statement = queryBuilder.prepare(getConnection());
    var results = queryBuilder.executeQuery(statement, mapOf(
        entry("owner", owner)
    ))) {
    ...
}
```

The `ResultSetAdapter` type returned by `executeQuery()` provides access to the contents of a JDBC result set via the `Iterable` interface. Individual rows are represented by `Map` instances produced by the adapter's iterator. The results could be coerced to a list of `Pet` instances and returned to the caller, or used as the data dictionary for a template document:

```java
return results.stream().map(result -> BeanAdapter.coerce(result, Pet.class)).toList();
```

```java
var templateEncoder = new TemplateEncoder(getClass().getResource("pets.html"), resourceBundle);

templateEncoder.write(results, response.getOutputStream());
```

### Schema Annotations
`QueryBuilder` also offers a simplified approach to query construction using "schema annotations". For example, given this type definition:

```java
@Table("pets")
public interface Pet {
    @Column("name")
    String getName();
    @Column("owner")
    String getOwner();
    @Column("species")
    String getSpecies();
    @Column("sex")
    String getSex();
    @Column("birth")
    Date getBirth();
    @Column("death")
    Date getDeath();
}
```

the preceding query could be written as follows:

```java
var queryBuilder = QueryBuilder.select(Pet.class);

queryBuilder.appendLine(" where owner = :owner");
```

Additional `QueryBuilder` methods can be used with the following annotations to filter or sort the returned rows:

* `PrimaryKey`
* `ForeignKey`
* `Identifier`
* `Index`

Insert, update, and delete operations are also supported. See the [pet](https://github.com/HTTP-RPC/Kilo/tree/master/kilo-test/src/main/java/org/httprpc/kilo/test/PetService.java) and [catalog](https://github.com/HTTP-RPC/Kilo/tree/master/kilo-test/src/main/java/org/httprpc/kilo/test/CatalogService.java) service examples for more information.

## ElementAdapter
The `ElementAdapter` class provides access to the contents of an XML DOM `Element` via the `Map` interface. For example, the following markup might be used to represent the status of a bank account:

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

This code could be used to load the document and adapt the root element: 

```java
var documentBuilderFactory = DocumentBuilderFactory.newInstance();

documentBuilderFactory.setExpandEntityReferences(false);
documentBuilderFactory.setIgnoringComments(true);

var documentBuilder = documentBuilderFactory.newDocumentBuilder();

Document document;
try (var inputStream = getClass().getResourceAsStream("account.xml")) {
    document = documentBuilder.parse(inputStream);
}

var accountAdapter = new ElementAdapter(document.getDocumentElement());
```

Attribute values can be obtained by prepending an "@" symbol to the attribute name:

```java
var id = accountAdapter.get("@id");

System.out.println(id); // 101
```

Individual sub-elements can be accessed by name. The text content of an element can be obtained by calling `toString()` on the returned adapter instance; for example:

```java
var holder = (Map<String, Object>)accountAdapter.get("holder");

var firstName = holder.get("firstName");
var lastName = holder.get("lastName");

System.out.println(String.format("%s, %s", lastName, firstName)); // Smith, John
```

An element's text content can also be accessed via the reserved "." key.

Multiple sub-elements can be obtained by appending an asterisk to the element name:

```java
var transactions = (Map<String, Object>)accountAdapter.get("transactions");
var credits = (List<Map<String, Object>>)transactions.get("credit*");

for (var credit : credits) {
    System.out.println(credit.get("amount"));
    System.out.println(credit.get("date"));
}
```

`ElementAdapter` also supports `put()` and `remove()` for modifying an element's contents.

## ResourceBundleAdapter
The `ResourceBundleAdapter` class provides access to the contents of a resource bundle via the `Map` interface. It can be used to localize the headings in a CSV document, for example:

```
name = Name
description = Description
quantity = Quantity
```

```java
var csvEncoder = new CSVEncoder(listOf("name", "description", "quantity"));

var resourceBundle = ResourceBundle.getBundle(getClass().getPackageName() + ".labels");

csvEncoder.setLabels(new ResourceBundleAdapter(resourceBundle));

csvEncoder.write(listOf(
    mapOf(
        entry("name", "Item 1"),
        entry("description", "Item number 1"),
        entry("quantity", 3)
    ),
    mapOf(
        entry("name", "Item 2"),
        entry("description", "Item number 2"),
        entry("quantity", 5)
    ),
    mapOf(
        entry("name", "Item 3"),
        entry("description", "Item number 3"),
        entry("quantity", 7)
    )
), System.out);
```

This code would produce the following output:

```csv
"Name","Description","Quantity"
"Item 1","Item number 1",3
"Item 2","Item number 2",5
"Item 3","Item number 3",7
```

## Pipe
The `Pipe` class provides a vehicle by which a producer thread can submit a sequence of elements for retrieval by a consumer thread. It implements the `Iterable` interface and returns values as they become available, blocking if necessary.

For example, the following code executes a SQL query that retrieves all rows from an `employees` table:

```java
@Table("employees")
public interface Employee {
    @Column("emp_no")
    @PrimaryKey
    Integer getEmployeeNumber();
    @Column("first_name")
    String getFirstName();
    @Column("last_name")
    String getLastName();
    @Column("gender")
    String getGender();
    @Column("birth_date")
    LocalDate getBirthDate();
    @Column("hire_date")
    LocalDate getHireDate();
}
```

```java
var queryBuilder = QueryBuilder.select(Employee.class);

try (var connection = getConnection();
    var statement = queryBuilder.prepare(connection);
    var results = queryBuilder.executeQuery(statement)) {
    return results.stream().map(result -> BeanAdapter.coerce(result, Employee.class)).toList();
}
```

All rows are processed and added to the list before anything is returned to the caller. For small result sets, the latency and memory implications associated with this approach might be acceptable. However, for larger data volumes the following alternative may be preferable. The query is executed on a background thread, and the transformed results are streamed back to the caller via a pipe:

```java
var pipe = new Pipe<Employee>(4096, 15000);

executorService.submit(() -> {
    var queryBuilder = QueryBuilder.select(Employee.class);

    try (var connection = getConnection();
        var statement = queryBuilder.prepare(connection);
        var results = queryBuilder.executeQuery(statement)) {
        pipe.accept(results.stream().map(result -> BeanAdapter.coerce(result, Employee.class)));
    } catch (SQLException exception) {
        throw new RuntimeException(exception);
    }
});

return pipe;
```

The pipe is configured with a capacity of 4K elements and a timeout of 15s. Limiting the capacity ensures that the producer does not do more work than necessary if the consumer fails to retrieve all of the data. Similarly, specifying a timeout ensures that the consumer does not wait indefinitely if the producer stops submitting data.

This implementation is slightly more verbose than the first one. However, because no intermediate buffering is required, results are available to the caller sooner, and CPU and memory load is reduced.

For more information, see the [employee service](https://github.com/HTTP-RPC/Kilo/blob/master/kilo-test/src/main/java/org/httprpc/kilo/test/EmployeeService.java) example.

## Collections and Optionals
The `Collections` class provides a set of static utility methods for declaratively instantiating list, map, and set values:

```java
public static <E> List<E> listOf(E... elements) { ... }
public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) { ... }
public static <K, V> Map.Entry<K, V> entry(K key, V value) { ... }
public static <E> Set<E> setOf(E... elements) { ... }
```

They offer an alternative to similar methods defined by the `List`, `Map`, and `Set` interfaces, which produce immutable instances and do not permit `null` values. The following immutable variants are also provided:

```java
public static <E> List<E> immutableListOf(E... elements) { ... }
public static <K, V> Map<K, V> immutableMapOf(Map.Entry<K, V>... entries) { ... }
public static <E> Set<E> immutableSetOf(E... elements) { ... }
```

Additionally, `Collections` includes the following methods for creating empty lists and maps:

```java
public static <E> List<E> emptyListOf(Class<E> elementType) { ... }
public static <K, V> Map<K, V> emptyMapOf(Class<K> keyType, Class<V> valueType) { ... }
public static <E> Set<E> emptySetOf(Class<E> elementType) { ... }
```

These provide a slightly more readable alternative to `java.util.Collections#emptyList()` and `java.util.Collections#emptyMap()`, respectively:

```java
var list1 = java.util.Collections.<Integer>emptyList();
var list2 = emptyListOf(Integer.class);

var map1 = java.util.Collections.<String, Integer>emptyMap();
var map2 = emptyMapOf(String.class, Integer.class);

var set1 = java.util.Collections.<Integer>emptySet();
var set2 = emptySetOf(Integer.class);
```

The following methods can be used to identify the index of the first or last element in a list that matches a given predicate:

```java
public static <E> int firstIndexWhere(List<E> list, Predicate<E> predicate) { ... }
public static <E> int lastIndexWhere(List<E> list, Predicate<E> predicate) { ... }
```

For example:

```java
var list = listOf("a", "b", "c", "b", "d");

var i = Collections.firstIndexWhere(list, element -> element.equals("b")); // 1
var j = Collections.lastIndexWhere(list, element -> element.equals("e")); // -1
```

Finally, the `valueAt()` method can be used to access nested values in an object hierarchy. For example:

```java
var map = mapOf(
    entry("a", mapOf(
        entry("b", mapOf(
            entry("c", listOf(1, 2, 3))
        ))
    ))
);

var value = Collections.valueAt(map, "a", "b", "c", 1); // 2
```

The `Optionals` class contains methods for working with optional (or "nullable") values:

```java
public static <T> T coalesce(T... values) { ... }

public static <T, U> U map(T value, Function<? super T, ? extends U> transform) { ... }
public static <T, U> U map(T value, Function<? super T, ? extends U> transform, U defaultValue) { ... }

public static <T> void perform(T value, Consumer<? super T> action) { ... }
public static <T> void perform(T value, Consumer<? super T> action, Runnable defaultAction) { ... }
```

These methods are provided as a less verbose alternative to similar methods defined by the `java.util.Optional` class. For example:

```java
var value = "xyz";

var a = Optional.ofNullable(null).orElse(Optional.ofNullable(null).orElse(value)); // xyz
var b = Optionals.coalesce(null, null, value); // xyz
```

```java
var value = "hello";

var a = Optional.ofNullable(value).map(String::length).orElse(null); // 5
var b = Optionals.map(value, String::length); // 5
```

```java
var value = new AtomicInteger(0);

Optional.ofNullable(value).ifPresent(AtomicInteger::incrementAndGet);
Optionals.perform(value, AtomicInteger::incrementAndGet);
```

# Kotlin Support
Kilo-based web services and consumers can be also implemented using the [Kotlin](https://kotlinlang.org) programming language. For example, the following is a simple web service written in Kotlin:

```kotlin
@WebServlet(urlPatterns = ["/*"], loadOnStartup = 1)
@Description("Greeting example service.")
class GreetingService: WebService() {
    @RequestMethod("GET")
    @Description("Returns a friendly greeting.")
    fun getGreeting(): String {
        return "Hello, World!"
    }
}
```

An example of a [typed invocation](#typed-invocation) proxy implemented in Kotlin can be found [here](https://github.com/HTTP-RPC/Kilo/blob/master/kilo-test/src/test/java/org/httprpc/kilo/test/UserTest.kt).

Note that Kotlin code should be compiled with the `-java-parameters` flag so that method parameter names are available at runtime. 

# Additional Information
This guide introduced the Kilo framework and provided an overview of its key features. For additional information, see the [examples](https://github.com/HTTP-RPC/Kilo/tree/master/kilo-test/src/main/java/org/httprpc/kilo/test).
