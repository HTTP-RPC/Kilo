[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc.svg)](http://repo1.maven.org/maven2/org/httprpc/httprpc/)

# Introduction
HTTP-RPC is an open-source framework for implementing RESTful and REST-like services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is distributed as a single JAR file that is less than 60KB in size, making it an ideal choice for applications where a minimal footprint is desired.

This guide introduces the HTTP-RPC framework and provides an overview of its key features.

# Feedback
Feedback is welcome and encouraged. Please feel free to [contact me](mailto:gk_brown@icloud.com?subject=HTTP-RPC) with any questions, comments, or suggestions. Also, if you like using HTTP-RPC, please consider [starring](https://github.com/gk-brown/HTTP-RPC/stargazers) it!

# Contents
* [Getting HTTP-RPC](#getting-http-rpc)
* [HTTP-RPC Classes](#http-rpc-classes)
    * [WebService](#webservice)
    * [JSONEncoder and JSONDecoder](#jsonencoder-and-jsondecoder)
    * [CSVEncoder and CSVDecoder](#csvencoder-and-csvdecoder)
    * [BeanAdapter](#beanadapter)
    * [ResultSetAdapter and Parameters](#resultsetadapter-and-parameters)
    * [WebServiceProxy](#webserviceproxy)
* [Additional Information](#additional-information)

# Getting HTTP-RPC
The HTTP-RPC JAR file can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). It is also available via Maven:

```xml
<dependency>
    <groupId>org.httprpc</groupId>
    <artifactId>httprpc</artifactId>
    <version>...</version>
</dependency>
```

HTTP-RPC requires Java 8 or later and a servlet container supporting Java Servlet specification 3.1 or later.

# HTTP-RPC Classes
HTTP-RPC provides the following classes for implementing REST services:

* `org.httprpc`
    * `WebService` - abstract base class for web services
    * `RequestMethod` - annotation that associates an HTTP verb with a service method
    * `RequestParameter` - annotation that associates a custom request parameter name with a method argument
    * `ResourcePath` - annotation that associates a resource path with a service method
    * `JSONEncoder` - class that serializes an object hierarchy to JSON
    * `JSONDecoder` - class that deserializes an object hierarchy from JSON
    * `CSVEncoder` - class that serializes an iterable sequence of values to CSV
    * `CSVDecoder` - class that deserializes an iterable sequence of values from CSV
    * `WebServiceProxy` - class for consuming remote web services
    * `WebServiceException` - exception thrown when a service operation returns an error
* `org.httprpc.beans`
    * `BeanAdapter` - class that presents the properties of a Java Bean object as a map and vice versa
    * `Key` - annotation that associates a custom key with a Bean property
* `org.httprpc.sql`
    * `ResultSetAdapter` - class that presents the contents of a JDBC result set as an iterable sequence of maps or typed row values
    * `Parameters` - class for applying named parameters values to prepared statements 

These classes are explained in more detail in the following sections.

## WebService
`WebService` is an abstract base class for REST services. It extends the similarly abstract `HttpServlet` class provided by the servlet API. 

Service operations are defined by adding public methods to a concrete service implementation. Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `WebService` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as JSON.

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the servlet. If unspecified, the method is associated with the servlet itself. 

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

The following request would cause the first method to be invoked:

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

Missing or `null` values are automatically converted to 0 or `false` for primitive types.

`List` arguments represent multi-value parameters. List values are automatically converted to their declared types (e.g. `List<Double>`).

`URL` and `List<URL>` arguments represent file uploads. They may be used only with `POST` requests submitted using the multi-part form data encoding. For example:

```java
@WebServlet(urlPatterns={"/upload/*"})
@MultipartConfig
public class FileUploadService extends WebService {
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

In general, service classes should be compiled with the `-parameters` flag so their method parameter names are available at runtime. However, the `RequestParameter` annotation can be used to customize the name of the parameter associated with a particular argument. For example, the following service might allow a caller to look up the name of the city associated with a particular zip code:

```java
@WebServlet(urlPatterns={"/lookup/*"})
public class LookupService extends WebService {
    @RequestMethod("GET")
    @ResourcePath("city")
    public String getCity(@RequestParameter("zip_code") String zipCode) { 
        ...
    }
}
```

This request would invoke the `getCity()` method, passing "02101" as the `zipCode` argument:

```
GET /lookup/city?zip_code=02101
```

### Return Values
Return values are converted to JSON as follows:

* `CharSequence`: string
* `Number`: number
* `Boolean`: true/false
* `java.util.Date`: long value representing epoch time in milliseconds
* `java.util.time.LocalDate`: "yyyy-mm-dd"
* `java.util.time.LocalTime`: "hh:mm"
* `java.util.time.LocalDateTime`: "yyyy-mm-ddThh:mm"
* `Iterable`: array
* `java.util.Map`: object

For example, this method returns a `Map` instance containing three values:

```java
@RequestMethod("GET")
@ResourcePath("map")
public Map<String, ?> getMap() {
    HashMap<String, Object> map = new HashMap<>();

    map.put("text", "Lorem ipsum");
    map.put("number", 123);
    map.put("flag", true);
    
    return map;
}
```

The service would produce the following in response:

```json
{
    "text": "Lorem ipsum",
    "number": 123,
    "flag": true
}
```

Methods may also return `void` or `Void` to indicate that they do not produce a value. 

If the return value is not an instance of any of the aforementioned types, it is automatically wrapped in an instance of `BeanAdapter` and serialized as a `Map`. `BeanAdapter` is discussed in more detail later.

### Exceptions
If an exception is thrown by a service method, an HTTP 500 response will be returned. If the response has not yet been committed, the exception message will be returned as plain text in the response body. This allows a service to provide the caller with insight into the cause of the failure. For example:

```java
@RequestMethod("GET")
@ResourcePath("error")
public void generateError() throws Exception {
    throw new Exception("This is an error message.");
}
```

### Request and Repsonse Properties
`WebService` provides the following methods to allow a service to access the request and response objects associated with the current operation:

    protected HttpServletRequest getRequest() { ... }
    protected HttpServletResponse getResponse() { ... }

For example, a service might access the request to get the name of the current user, or use the response to return a custom header.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the return value (if any) will be ignored by `WebService`. This allows a service to return content that cannot be easily represented as JSON, such as image data or other response formats such as XML.

### Path Variables
Path variables may be specified by a "?" character in the resource path. For example:

```java
@RequestMethod("GET")
@ResourcePath("contacts/?/addresses/?")
public List<Map<String, ?>> getContactAddresses() { ... }
```

The `getKey()` method returns the value of a path variable associated with the current request:

```java
protected String getKeys(int index) { ... }
```
 
For example, given the following path:

    /contacts/jsmith/addresses/home

the value of the key at index 0 would be "jsmith", and the value at index 1 would be "home".

## JSONEncoder and JSONDecoder
The `JSONEncoder` class is used internally by `WebService` to serialize a service response. However, it can also be used by application code. For example, the following method would produce the same result as the map example shown earlier (albeit more verbosely):

```java
@RequestMethod("GET")
@ResourcePath("map")
public void getMap() throws IOException {
    HashMap<String, Object> map = new HashMap<>();

    map.put("text", "Lorem ipsum");
    map.put("number", 123);
    map.put("flag", true);

    JSONEncoder jsonEncoder = new JSONEncoder();

    try {
        jsonEncoder.writeValue(map, getResponse().getOutputStream());
    } finally {
        getResponse().flushBuffer();
    }
}
```

Values are converted to their JSON equivalents as described earlier. Unsupported types are serialized as `null`.

The `JSONDecoder` class deserializes a JSON document into a Java object hierarchy. JSON values are mapped to their Java equivalents as follows:

* string: `String`
* number: `Number`
* true/false: `Boolean`
* array: `java.util.List`
* object: `java.util.Map`

For example, the following code snippet uses `JSONDecoder` to parse a JSON array containing the first 6 values of the Fibonacci sequence:

```java
JSONDecoder jsonDecoder = new JSONDecoder();

List<Number> fibonacci = jsonDecoder.readValue(new StringReader("[1, 2, 3, 5, 8, 13]"));
```

## CSVEncoder and CSVDecoder
Although `WebService` automatically serializes a method return value as JSON, in some cases it may be preferable to return a a CSV (comma-separated value) document instead. Because field keys are specified only at the beginning of the document rather than being duplicated for every record, CSV generally has a smaller payload than JSON. Additionally, consumers can begin processing CSV as soon as the first record arrives, rather than waiting for the entire document to download.

TODO

TODO Mention Date and key path support in CSVEncoder

### Typed Iteration
TODO

## BeanAdapter
The `BeanAdapter` class implements the `Map` interface and exposes any properties defined by the Bean as entries in the map, allowing custom data types to be serialized as JSON objects. 

If a property value is `null` or an instance of one of the following types, it is returned as-is:

* `String`
* `Number`
* `Boolean`
* `java.util.Date`
* `java.util.time.LocalDate`
* `java.util.time.LocalTime`
* `java.util.time.LocalDateTime`

If a property returns an instance of `List` or `Map`, the value will be wrapped in an adapter of the same type that automatically adapts its sub-elements. Otherwise, the value is considered a nested Bean and is wrapped in a `BeanAdapter`.

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

### Typed Map Access
`BeanAdapter` can also be used to facilitate type-safe access to deserialized JSON data. For example, `JSONDecoder` would parse the content returned by the previous example into a collection of map and list values. The `adapt()` method of the `BeanAdapter` class can be used to efficiently transform this loosely typed data structure into a strongly typed object hierarchy. This method takes an object and a result type as arguments and returns an instance of the given type that adapts the underlying value.

If the value is already an instance of the requested type, it is returned as-is. Otherwise:

* If the target type is a number or boolean, the value is parsed or coerced using the appropriate conversion method. Missing or `null` values are automatically converted to 0 or `false` for primitive argument types.
* If the target type is a `String`, the value is adapted via its `toString()` method.
* If the target type is `java.util.Date`, the value is parsed or coerced to a long value representing epoch time in milliseconds and then converted to a `Date`. 
* If the target type is `java.util.time.LocalDate`, `java.util.time.LocalTime`, or `java.util.time.LocalDateTime`, the value is parsed using the appropriate `parse()` method.
* If the target type is `java.util.List` or `java.util.Map`, the value is wrapped in an adapter of the same type that automatically adapts its sub-elements.

If the value is not an instance of any of the above types, it is considered a nested Bean, and the given value is assumed to be a map. If the target type is an interface, the return value is a dynamic implementation of the interface that maps accessor methods to entries in the map. Otherwise, an instance of the given class is dynamically created and populated using the entries in the map.

For example, given the following interface definition:

```java
public interface TreeNode {
    public String getName();
    public List<TreeNode> getChildren();
}
```

the `adapt()` method can be used to model the result data as a collection of `TreeNode` values:

```java
TreeNode root = BeanAdapter.adapt(map, TreeNode.class);

root.getName(); // "Seasons"
root.getChildren().get(0).getName(); // "Winter"
root.getChildren().get(0).getChildren().get(0).getName(); // "January"
```

### Custom Property Keys
The `Key` annotation can be used to associate a custom key with a Bean property. For example, the following property would appear as "first_name" in the resulting map rather than "firstName":

```java
@Key("first_name")
public String getFirstName() {
    return firstName;
}
```

Similarly, when adapting an existing map using an interface, the following method would return the value of the "first_name" key:

```java
@Key("first_name")
public String getFirstName();
```

## ResultSetAdapter and Parameters
The `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing query results to be serialized as an array of JSON objects. For example:

```java
JSONEncoder jsonEncoder = new JSONEncoder();

try (ResultSet resultSet = statement.executeQuery()) {
    jsonEncoder.writeValue(new ResultSetAdapter(resultSet), getResponse().getOutputStream());
}
```

Note that, instead of producing a new map instance for each iteration, `ResultSetAdapter` returns a single map value for all rows. The contents of this map are updated on each call to the adapter's `next()` method, reducing execution time and keeping memory footprint to a minimum.

The `Parameters` class is used to simplify execution of prepared statements. It provides a means for executing statements using named parameter values rather than indexed arguments. Parameter names are specified by a leading `:` character. For example:

```sql
SELECT * FROM some_table 
WHERE column_a = :a OR column_b = :b OR column_c = COALESCE(:c, 4.0)
```
 
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

Parameter values are set via the `put()` method, and applied to the statement via the `apply()` method:

```java
parameters.put("a", "hello");
parameters.put("b", 3);

parameters.apply(statement);
```

Once applied, the statement can be executed:

```java
return new ResultSetAdapter(statement.executeQuery());    
```

A complete example that uses both classes is shown below. It is based on the "pet" table from the MySQL sample database:

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
public void getPets(String owner) throws SQLException, IOException {
    try (Connection connection = DriverManager.getConnection(DB_URL)) {
        Parameters parameters = Parameters.parse("SELECT name, species, sex, birth FROM pet WHERE owner = :owner");

        parameters.put("owner", owner);

        try (PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement);

            try (ResultSet resultSet = statement.executeQuery()) {
                JSONEncoder jsonEncoder = new JSONEncoder();
                
                jsonEncoder.writeValue(new ResultSetAdapter(resultSet), getResponse().getOutputStream());
            }
        }
    } finally {
        getResponse().flushBuffer();
    }
}
```

For example, given this request:

```
GET /pets?owner=Gwen
```

The service would return something like the following:

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

### Typed Iteration
The `adapt()` method of the `ResultSetAdapter` class can be used to facilitate typed iteration of query results. This method produces an `Iterable` sequence of values of a given type representing the rows in the result set. The returned adapter uses dynamic proxy invocation to map properties declared by the interface to column labels in the result set. A single proxy instance is used for all rows to minimize heap allocation. 

For example, the following interface might be used to model the results of the "pet" query shown in the previous section:

```java
public interface Pet {
    public String getName();
    public String getOwner();
    public String getSpecies();
    public String getSex();
    public Date getBirth();
}
```

This service method uses `adapt()` to create an iterable sequence of `Pet` values. It wraps the adapter's iterator in a stream, and then uses the stream to calculate the average age of all pets in the database. The `getBirth()` method declared by the `Pet` interface is used to retrieve each pet's age in epoch time. The average value is converted to years at the end of the method:

```java
@RequestMethod("GET")
@ResourcePath("average-age")
public double getAverageAge() throws SQLException {
    Date now = new Date();

    double averageAge;
    try (Connection connection = DriverManager.getConnection(DB_URL);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT birth FROM pet")) {        
        ResultSetAdapter resultSetAdapter = new ResultSetAdapter(resultSet);

        Iterable<Pet> pets = resultSetAdapter.adapt(Pet.class);

        Stream<Pet> stream = StreamSupport.stream(pets.spliterator(), false);

        averageAge = stream.mapToLong(pet -> now.getTime() - pet.getBirth().getTime()).average().getAsDouble();
    }

    return averageAge / (365.0 * 24.0 * 60.0 * 60.0 * 1000.0);
}
```

## WebServiceProxy
The `WebServiceProxy` class enables an HTTP-RPC service to act as a consumer of other REST-based web services. Service proxies are initialized via a constructor that takes the following arguments:

* `method` - the HTTP method to execute
* `url` - an instance of `java.net.URL` representing the target of the operation

Request headers and arguments are specified via the `getHeaders()` and `getArguments()` methods, respectively. Like HTML forms, arguments are submitted either via the query string or in the request body. Arguments for `GET`, `PUT`, and `DELETE` requests are always sent in the query string. `POST` arguments are typically sent in the request body, and may be submitted as either "application/x-www-form-urlencoded" or "multipart/form-data" (specified via the proxy's `setEncoding()` method). However, if the request body is provided via a custom request handler (specified via the `setRequestHandler()` method), `POST` arguments will be sent in the query string.

The `toString()` method is generally used to convert an argument to its string representation. However, `Date` instances are automatically converted to a long value representing epoch time (the number of milliseconds that have elapsed since midnight on January 1, 1970). Additionally, `Iterable` instances represent multi-value parameters and behave similarly to `<select multiple>` tags in HTML. Further, when using the multi-part form data encoding, instances of `URL` represent file uploads and behave similarly to `<input type="file">` tags in HTML forms. Iterables of URL values operate similarly to `<input type="file" multiple>` tags.

Service operations are invoked via one of the following methods:

```java
public <T> T invoke() throws IOException { ... }
public <T> T invoke(ResponseHandler<T> responseHandler) throws IOException { ... }
```

The first version automatically deserializes a successful response using `JSONDecoder`. The second version allows a caller to provide a custom response handler. 

If the server returns an error response, a `WebServiceException` will be thrown. The response code can be retrieved via the exception's `getStatus()` method. If the content type of the response is "text/plain", the body of the response will be returned in the exception message.

For example, the following code snippet demonstrates how `WebServiceProxy` might be used to access the operations of the simple math service discussed earlier:

```java
WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/math/sum"));

webServiceProxy.getArguments().put("a", 4);
webServiceProxy.getArguments().put("b", 2);

Number result = webServiceProxy.invoke();

System.out.println(result); // 6.0
```

### Typed Web Service Access
The `adapt()` methods of the `WebServiceProxy` class can be used to facilitate type-safe access to web services:

```java
public static <T> T adapt(URL baseURL, Class<T> type) { ... }
public static <T> T adapt(URL baseURL, Class<T> type, Map<String, ?> headers) { ... }
```

Both versions take a base URL and an interface type as arguments and return an instance of the given type that can be used to invoke service operations. The second version also accepts a map of HTTP header values that will be submitted with every service request.

The `RequestMethod` annotation is used to associate an HTTP verb with an interface method. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the base URL. Path variables are not supported. If unspecified, the method is associated with the base URL itself.

In general, service adapters should be compiled with the `-parameters` flag so their method parameter names are available at runtime. However, the `RequestParameter` annotation can be used to associate a custom parameter name with a request argument. 

`POST` requests are always submitted using the multi-part encoding. Values are returned as described for `WebServiceProxy` and adapted as described [earlier](#typed-map-access) based on the method return type.

For example, the following interface might be used to model the addition operations of the math service:

```java
public interface MathService {
    @RequestMethod("GET")
    @ResourcePath("sum")
    public double getSum(double a, double b) throws IOException;

    @RequestMethod("GET")
    @ResourcePath("sum")
    public double getSum(List<Double> values) throws IOException;
}
```

This code uses the `adapt()` method to create an instance of `MathService`, then invokes the `getSum()` method on the returned instance. The results are identical to the previous example:

```java
MathService mathService = WebServiceProxy.adapt(new URL("http://localhost:8080/httprpc-test/math/"), MathService.class);

double result = mathService.getSum(4, 2);

System.out.println(result); // 6.0
```

# Additional Information
This guide introduced the HTTP-RPC framework and provided an overview of its key features. For additional information, see the the [examples](https://github.com/gk-brown/HTTP-RPC/tree/master/httprpc-test/src/main/java/org/httprpc/test).
