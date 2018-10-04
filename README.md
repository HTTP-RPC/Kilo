[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc.svg)](http://repo1.maven.org/maven2/org/httprpc/httprpc/)

# Introduction
HTTP-RPC is an open-source framework for implementing and interacting with RESTful and REST-like web services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is distributed as a single JAR file that is about 65KB in size, making it an ideal choice for applications where a minimal footprint is desired.

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
HTTP-RPC provides the following classes for creating and consuming REST services:

* `org.httprpc`
    * `RequestMethod` - annotation that associates an HTTP verb with a service method
    * `RequestParameter` - annotation that associates a custom request parameter name with a method argument
    * `ResourcePath` - annotation that associates a resource path with a service method
    * `Response` - annotation that associates a custom response description with a service method
    * `WebService` - abstract base class for web services
    * `WebServiceException` - exception thrown when a service operation returns an error
    * `WebServiceProxy` - class for invoking remote web services
* `org.httprpc.io`
    * `CSVDecoder` - class that reads an iterable sequence of values from CSV
    * `CSVEncoder` - class that writes an iterable sequence of values to CSV
    * `JSONDecoder` - class that reads an object hierarchy from JSON
    * `JSONEncoder` - class that writes an object hierarchy to JSON
* `org.httprpc.beans`
    * `BeanAdapter` - class that presents the properties of a Java bean object as a map and vice versa
    * `Key` - annotation that associates a custom key with a bean property
* `org.httprpc.sql`
    * `Parameters` - class for applying named parameters values to prepared statements 
    * `ResultSetAdapter` - class that presents the contents of a JDBC result set as an iterable sequence of maps or typed row values

These classes are explained in more detail in the following sections.

## WebService
`WebService` is an abstract base class for REST services. It extends the similarly abstract `HttpServlet` class provided by the servlet API. 

Service operations are defined by adding public methods to a concrete service implementation. Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `WebService` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as [JSON](http://json.org).

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

Missing or `null` values are automatically converted to `0` or `false` for primitive types.

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

#### Custom Parameter Names
In general, service classes should be compiled with the `-parameters` flag so the names of their method parameters are available at runtime. However, the `RequestParameter` annotation can be used to customize the name of the parameter associated with a particular argument. For example, the following service might allow a caller to look up the name of the city associated with a particular zip code:

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
Return values are converted to their JSON equivalents as follows:

* `CharSequence`: string
* `Number`: number
* `Boolean`: true/false
* `Enum`: ordinal value
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

If the return value is not an instance of any of the aforementioned types, it is automatically wrapped in an instance of `BeanAdapter` and serialized as a `Map`. `BeanAdapter` is discussed in more detail [later](#beanadapter).

#### Custom Result Encodings
Although return values are encoded as JSON by default, subclasses can override the `encodeResult()` method of the `WebService` class to provide a custom result encoding. See the method documentation for more information.

### Exceptions
If any exception is thrown by a service method, an HTTP 500 response will be returned. If the response has not yet been committed, the exception message will be returned as plain text in the response body. This allows a service to provide the caller with insight into the cause of the failure. For example:

```java
@RequestMethod("GET")
@ResourcePath("error")
public void generateError() throws Exception {
    throw new Exception("This is an error message.");
}
```

### Request and Repsonse Properties
`WebService` provides the following methods to allow a service method to access the request and response objects associated with the current operation:

    protected HttpServletRequest getRequest() { ... }
    protected HttpServletResponse getResponse() { ... }

For example, a service might use the request to get the name of the current user, or use the response to return a custom header.

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

### Documentation
API documentation can be viewed by appending "?api" to a service URL; for example:

```
GET /math?api
```

Service methods are grouped by resource path. Method parameters and return values are encoded as follows:

* `Object`: "any"
* `Void` or `void`: "void"
* `Byte` or `byte`: "byte"
* `Short` or `short`: "short"
* `Integer` or `int`: "integer"
* `Long` or `long`: "long"
* `Float` or `float`: "float"
* `Double` or `double`: "double"
* Any other type that extends `Number`: "number"
* Any type that implements `CharSequence`: "string"
* Any `Enum` type: "enum"
* Any type that extends `java.util.Date`: "date"
* `java.util.time.LocalDate`: "date-local"
* `java.util.time.LocalTime`: "time-local"
* `java.util.time.LocalDateTime`: "datetime-local"
* `java.util.List`: "[<em>element type</em>]"
* `java.util.Map`: "[<em>key type</em>: <em>value type</em>]"
* Any other type: "{property1: <em>property 1 type</em>, property2: <em>property 2 type</em>, ...}"

For example, a description of the math service might look like this:

> ## /math/sum
> 
> ```
> GET (a: double, b: double) -> double
> ```
> ```
> GET (values: [double]) -> double
> ```

Note that methods tagged with the `Deprecated` annotation will be flagged as such in the generated output.

#### Custom Response Descriptions
Methods that return a custom response can use the `Response` annotation to describe the result. For example, this method declaration:

```
@RequestMethod("GET")
@ResourcePath("map")
@Response("{text: string, number: integer, flag: boolean}")
public void getMap() {
    ...
}
```

would produce a description similar to the following:

> ## /map
> 
> ```
> GET () -> {text: string, number: integer, flag: boolean}
> ```

#### Localized Service Descriptionss
Services can provide localized API documentation by including one or more resource bundles on the classpath. These resource bundles must reside in the same package and have the same base name as the service itself.

For example, the following _MathService.properties_ file could be used to provide localized method descriptions for the `MathService` class:

```
MathService = Math example service.
getSum = Calculates the sum of two or more numbers.
getSum.a = The first number.
getSum.b = The second number.
getSum.values = The numbers to add.
```

The first line describes the service itself. The remaining lines describe the service methods and their parameters. Note that an overloaded method such as `getSum()` can only have a single description, so it should be generic enough to describe all overloads.

A localized description of the math service might look like this:

> Math example service.
> 
> ## /math/sum
> ```
> GET (a: double, b: double) -> double
> ```
> Calculates the sum of two or more numbers.
> 
> - **a** The first number.
> - **b** The second number. 
> 
> ```
> GET (values: [double]) -> double
> ```
> Calculates the sum of two or more numbers.
> 
> - **values** The numbers to add.

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

System.out.println(fibonacci.get(2)); // 3
```

## CSVEncoder and CSVDecoder
Although `WebService` automatically serializes return values as JSON, in some cases it may be preferable to return a [CSV](https://tools.ietf.org/html/rfc4180) document instead. Because field keys are specified only at the beginning of the document rather than being duplicated for every record, CSV generally requires less bandwidth than JSON. Additionally, consumers can begin processing CSV as soon as the first record arrives, rather than waiting for the entire document to download.

The `CSVEncoder` class can be used to encode an iterable sequence of map values to CSV. For example, the following JSON document contains a list of objects representing the months of the year and their day counts:

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

List<Map<String, Object>> months = jsonDecoder.readValue(inputStream);
```

`CSVEncoder` could then be used to export the results as CSV. The string values passed to the encoder's constructor represent the columns in the output document (and the map keys to which the columns correspond):

```
CSVEncoder csvEncoder = new CSVEncoder(Arrays.asList("name", "days"));

csvEncoder.writeValues(months, System.out);
```

Keys actually represent "key paths" and can refer to nested map values using dot notation. String values are automatically wrapped in double-quotes and escaped. Enums are encoded using their ordinal values. Instances of `java.util.Date` are encoded as a long value representing epoch time. All other values are encoded via `toString()`. 

The preceding code snippet would produce output similar to the following:

```csv
"name","days"
"January",31
"February",28
"March",31
...
```

Due to its compact and tabular nature, CSV is ideally suited to serializing the results of relational database queries. HTTP-RPC's `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing query results to be efficiently serialized to either JSON or CSV. `ResultSetAdapter` is discussed in more detail [later](#resultsetadapter-and-parameters).

The `CSVDecoder` class deserializes a CSV document as an iterable sequence of maps. Rather than reading the entire payload into memory and returning the data as a list, `CSVDecoder` returns a cursor over the records in the document. This allows a consumer to read records essentially as they are being produced, reducing memory consumption and improving throughput. 

The following code would perform the reverse conversion (from CSV to JSON):

```java
// Read from CSV
CSVDecoder csvDecoder = new CSVDecoder();

Iterable<Map<String, String>> months = csvDecoder.readValues(inputStream);

// Write to JSON
JSONEncoder jsonEncoder = new JSONEncoder();

jsonEncoder.writeValue(months, System.out);
```

## BeanAdapter
The `BeanAdapter` class implements the `Map` interface and exposes any properties defined by a bean as entries in the map, allowing custom data types to be serialized as JSON objects or CSV records. 

If a property value is `null` or an instance of one of the following types, it is returned as is:

* `CharSequence`
* `Number`
* `Boolean`
* `Enum`
* `java.util.Date`
* `java.util.time.LocalDate`
* `java.util.time.LocalTime`
* `java.util.time.LocalDateTime`

If a property returns an instance of `List` or `Map`, the value will be wrapped in an adapter of the same type that automatically adapts its sub-elements. Otherwise, the value is assumed to be a bean and is wrapped in a `BeanAdapter`.

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

### Typed Access
`BeanAdapter` can also be used to facilitate type-safe access to deserialized JSON data. For example, `JSONDecoder` would parse the content returned by the previous example into a collection of map and list values. The `adapt()` method of the `BeanAdapter` class can be used to efficiently transform this loosely typed data structure into a strongly typed object hierarchy. This method takes an object and a result type as arguments, and returns an instance of the given type that adapts the underlying value:

```java
public static <T> T adapt(Object value, Type type) { ... }
```

If the value is already an instance of the requested type, it is returned as is. Otherwise:

* If the target type is a number or boolean, the value is parsed or coerced using the appropriate conversion method. Missing or `null` values are automatically converted to `0` or `false` for primitive types.
* If the target type is a `String`, the value is adapted via its `toString()` method.
* If the target type is `java.util.Date`, the value is parsed or coerced to a long value representing epoch time in milliseconds and then converted to a `Date`. 
* If the target type is `java.util.time.LocalDate`, `java.util.time.LocalTime`, or `java.util.time.LocalDateTime`, the value is parsed using the appropriate `parse()` method.
* If the target type is `java.util.List` or `java.util.Map`, the value is wrapped in an adapter of the same type that automatically adapts its sub-elements.

Otherwise, the target is assumed to be a bean, and the value is assumed to be a map. If the target type is a concrete class, an instance of the type is dynamically created and populated using the entries in the map. Property values are adapted as described above. If a property provides multiple setters, the first applicable setter will be applied.

If the target type is an interface, the return value is an implementation of the interface that maps accessor methods to entries in the map. For example, given the following interface definition:

```java
public interface TreeNode {
    public String getName();
    public List<TreeNode> getChildren();
}
```

the `adapt()` method can be used to model the preceding result data as a collection of `TreeNode` values:

```java
TreeNode root = BeanAdapter.adapt(map, TreeNode.class);

root.getName(); // "Seasons"
root.getChildren().get(0).getName(); // "Winter"
root.getChildren().get(0).getChildren().get(0).getName(); // "January"
```

### Custom Property Keys
The `Key` annotation can be used to associate a custom key with a bean property. For example, the following property would appear as "first_name" in the resulting map instead of "firstName":

```java
@Key("first_name")
public String getFirstName() {
    return firstName;
}
```

This code would cause the value to be imported from the "first_name" entry in the map instead of "firstName":

```java
@Key("first_name")
public void setFirstName(String firstName) {
    this.firstName = firstName;
}
```

Similarly, when using an interface type to adapt map values, the following method would return the value of the "first_name" key rather than "firstName":

```java
@Key("first_name")
public String getFirstName();
```

## ResultSetAdapter and Parameters
The `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing query results to be efficiently serialized to JSON or CSV. For example:

```java
JSONEncoder jsonEncoder = new JSONEncoder();

try (ResultSet resultSet = statement.executeQuery()) {
    jsonEncoder.writeValue(new ResultSetAdapter(resultSet), getResponse().getOutputStream());
}
```

Note that, instead of producing a new map instance for each iteration, `ResultSetAdapter` returns a single map value for all rows. The contents of this map are updated on each call to the adapter's `next()` method, reducing object construction overhead and keeping memory footprint to a minimum.

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

TODO Nested queries

### Typed Iteration
The `adapt()` method of the `ResultSetAdapter` class can be used to facilitate typed iteration of query results. This method produces an `Iterable` sequence of values of a given interface type representing the rows in the result set. The returned adapter uses dynamic proxy invocation to map properties declared by the interface to column labels in the result set. A single proxy instance is used for all rows to minimize heap allocation. 

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

The `toString()` method is generally used to convert an argument to its string representation. However, `Date` instances are automatically converted to a long value representing epoch time. Additionally, `Iterable` instances represent multi-value parameters and behave similarly to `<select multiple>` tags in HTML. Further, when using the multi-part form data encoding, instances of `URL` represent file uploads and behave similarly to `<input type="file">` tags in HTML forms. Iterables of URL values operate similarly to `<input type="file" multiple>` tags.

Service operations are invoked via one of the following methods:

```java
public <T> T invoke() throws IOException { ... }
public <T> T invoke(ResponseHandler<T> responseHandler) throws IOException { ... }
```

The first version automatically deserializes a successful response using `JSONDecoder`. The second version allows a caller to provide a custom response handler (for example, to iterate over the contents of a CSV document). 

If the server returns an error response, a `WebServiceException` will be thrown. The response code can be retrieved via the exception's `getStatus()` method. If the content type of the response is "text/plain", the body of the response will be returned in the exception message.

For example, the following code snippet demonstrates how `WebServiceProxy` might be used to access the operations of the simple math service discussed earlier:

```java
// GET /math/sum?a=2&b=4
WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/math/sum"));

HashMap<String, Object> arguments = new HashMap<>();

arguments.put("a", 4);
arguments.put("b", 2);

webServiceProxy.setArguments(arguments);

System.out.println(webServiceProxy.invoke()); // 6.0
```

```java
// GET /math/sum?values=1&values=2&values=3
WebServiceProxy webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/httprpc-test/math/sum"));

HashMap<String, Object> arguments = new HashMap<>();

arguments.put("values", Arrays.asList(1, 2, 3));

webServiceProxy.setArguments(arguments);

System.out.println(webServiceProxy.invoke()); // 6.0
```

### Typed Access
The `adapt()` methods of the `WebServiceProxy` class can be used to facilitate type-safe access to web services:

```java
public static <T> T adapt(URL baseURL, Class<T> type) { ... }
public static <T> T adapt(URL baseURL, Class<T> type, Map<String, ?> headers) { ... }
```

Both versions take a base URL and an interface type as arguments and return an instance of the given type that can be used to invoke service operations. The second version also accepts a map of HTTP header values that will be submitted with every service request.

The `RequestMethod` annotation is used to associate an HTTP verb with an interface method. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the base URL. Path variables are not supported. If unspecified, the method is associated with the base URL itself.

In general, service adapters should be compiled with the `-parameters` flag so their method parameter names are available at runtime. However, the `RequestParameter` annotation can be used to associate a custom parameter name with a request argument. 

`POST` requests are always submitted using the multi-part encoding. Values are returned as described for `WebServiceProxy` and adapted as described [earlier](#typed-map-access) based on the method return type.

For example, the following interface might be used to model the operations of the math service:

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

// GET /math/sum?a=2&b=4
System.out.println(mathService.getSum(4, 2)); // 6.0

// GET /math/sum?values=1&values=2&values=3
System.out.println(mathService.getSum(Arrays.asList(1.0, 2.0, 3.0))); // 6.0
```

### JavaScript
`WebServiceProxy` can also be used from JavaScript, via the Nashorn scripting engine. For example:

```javascript
var webServiceProxy = new org.httprpc.WebServiceProxy("GET", new java.net.URL("http://localhost:8080/httprpc-test/math/sum"));

// GET /math/sum?a=2&b=4
webServiceProxy.arguments = {"a": 4, "b": 2};

print(webServiceProxy.invoke()); // 6

// GET /math/sum?values=1&values=2&values=3
webServiceProxy.arguments = {"values": Java.asJSONCompatible([1, 2, 3])};

print(webServiceProxy.invoke()); // 6
```

Note that, because Nashorn automatically translates Java `List` and `Map` values to JavaScript array and object instances respectively, transformation or adaptation of service responses is not generally necessary.

For example, the following script would print the third value in the Fibonacci sequence:

```javascript
var webServiceProxy = new org.httprpc.WebServiceProxy("GET", new java.net.URL("http://localhost:8080/httprpc-test/test/fibonacci"));

// [1, 2, 3, 5, 8, 13]
var fibonacci = webServiceProxy.invoke();

print(fibonacci[2]); // 3
```

# Additional Information
This guide introduced the HTTP-RPC framework and provided an overview of its key features. For additional information, see the the [examples](https://github.com/gk-brown/HTTP-RPC/tree/master/httprpc-test/src/main/java/org/httprpc/test).
