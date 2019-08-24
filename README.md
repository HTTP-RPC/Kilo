[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc.svg)](http://repo1.maven.org/maven2/org/httprpc/httprpc/)

# Introduction
HTTP-RPC is an open-source framework for implementing RESTful and REST-like web services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is distributed as a single JAR file that is about 50KB in size, making it an ideal choice for applications where a minimal footprint is desired.

This guide introduces the HTTP-RPC framework and provides an overview of its key features.

# Feedback
Feedback is welcome and encouraged. Please feel free to [contact me](mailto:gk_brown@icloud.com?subject=HTTP-RPC) with any questions, comments, or suggestions. Also, if you like using HTTP-RPC, please consider [starring](https://github.com/gk-brown/HTTP-RPC/stargazers) it!

# Contents
* [Getting HTTP-RPC](#getting-http-rpc)
* [HTTP-RPC Classes](#http-rpc-classes)
    * [WebService](#webservice)
        * [Method Arguments](#method-arguments)
        * [Path Variables](#path-variables)
        * [Return Values](#return-values)
        * [Request and Repsonse Properties](#request-and-repsonse-properties)
        * [Authorization](#authorization)
        * [Exceptions](#exceptions)
        * [API Documentation](#api-documentation)
    * [JSONEncoder and JSONDecoder](#jsonencoder-and-jsondecoder)
    * [CSVEncoder and CSVDecoder](#csvencoder-and-csvdecoder)
    * [XMLEncoder](#xmlencoder)
    * [BeanAdapter](#beanadapter)
    * [ResultSetAdapter and Parameters](#resultsetadapter-and-parameters)
* [Kotlin Support](#kotlin-support)
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
The HTTP-RPC framework includes the following classes:

* `org.httprpc`
    * `RequestMethod` - annotation that associates an HTTP verb with a service method
    * `RequestParameter` - annotation that associates a custom request parameter name with a method argument
    * `ResourcePath` - annotation that associates a resource path with a service method
    * `Response` - annotation that associates a custom response description with a service method
    * `WebService` - abstract base class for web services
* `org.httprpc.io`
    * `CSVDecoder` - class that reads an iterable sequence of values from CSV
    * `CSVEncoder` - class that writes an iterable sequence of values to CSV
    * `JSONDecoder` - class that reads an object hierarchy from JSON
    * `JSONEncoder` - class that writes an object hierarchy to JSON
    * `XMLEncoder` - class that writes an object hierarchy to XML
* `org.httprpc.beans`
    * `BeanAdapter` - class that presents the properties of a Java bean object as a map
    * `Ignore` - annotation indicating that a bean property should be ignored
    * `Key` - annotation that associates a custom key with a bean property
* `org.httprpc.sql`
    * `Parameters` - class for applying named parameter values to prepared statements 
    * `ResultSetAdapter` - class that presents the contents of a JDBC result set as an iterable sequence of maps

These classes are discussed in more detail in the following sections.

## WebService
`WebService` is an abstract base class for web services. It extends the similarly abstract `HttpServlet` class provided by the servlet API. 

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

#### Parameter Names
In general, service classes should be compiled with the `-parameters` flag so the names of their method parameters are available at runtime. However, the `RequestParameter` annotation can be used to customize the name of the parameter associated with a particular argument. For example:

```java
@RequestMethod("GET")
public double getTemperature(@RequestParameter("zip_code") String zipCode) { 
    ... 
}
```

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

    protected HttpServletRequest getRequest() { ... }
    protected HttpServletResponse getResponse() { ... }

For example, a service might use the request to get the name of the current user, or use the response to return a custom header.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the method's return value (if any) will be ignored by `WebService`. This allows a service to return content that cannot be easily represented as JSON, such as image data or other response formats such as XML.

### Authorization
Service requests can be authorized by overriding the following method:

```java
protected boolean isAuthorized(HttpServletRequest request, Method method) { ... }
```

The first argument contains the current request, and the second the service method to be invoked. If `isAuthorized()` returns `true` (the default), method execution will proceed. Otherwise, the method will not be invoked, and an HTTP 403 response will be returned.

### Exceptions
If any exception is thrown by a service method, an HTTP 500 response will be returned. If the response has not yet been committed, the exception message will be returned as plain text in the response body. This allows a service to provide the caller with insight into the cause of the failure. For example:

```java
@RequestMethod("GET")
@ResourcePath("error")
public void generateError() throws Exception {
    throw new Exception("This is an error message.");
}
```

### API Documentation
API documentation can be viewed by appending "?api" to a service URL; for example:

```
GET /math?api
```

Methods are grouped by resource path. Parameters and return values are encoded as follows:

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

For example, a description of the math service might look like this:

> ## /math/sum
> 
> ```
> GET (a: double, b: double) -> double
> ```
> ```
> GET (values: [double]) -> double
> ```

If a method is tagged with the `Deprecated` annotation, it will be identified as such in the generated output.

#### Custom Response Descriptions
Methods that return a custom response can use the `Response` annotation to describe the result. For example, given this method declaration:

```
@RequestMethod("GET")
@ResourcePath("map")
@Response("{text: string, number: integer, flag: boolean}")
public Map<String, ?> getMap() {
    ...
}
```

the service would produce a description similar to the following:

> ## /map
> 
> ```
> GET () -> {text: string, number: integer, flag: boolean}
> ```

#### Localized Service Descriptions
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

The `JSONDecoder` class deserializes a JSON document into a Java object hierarchy. JSON values are mapped to their Java equivalents as follows:

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

```
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

## XMLEncoder
The `XMLEncoder` class can be used to serialize an object hierarchy as XML (for example, to prepare it for further transformation via [XSLT](https://www.w3.org/TR/xslt/all/)). 

The root object provided to the encoder is an iterable sequence of map values. For example:

```java
List<Map<String, ?>> values = ...;

XMLEncoder xmlEncoder = new XMLEncoder();

xmlEncoder.write(values, writer);
```

Sequences are serialized as shown below. Each `<item>` element corresponds to a map value produced by the sequence's iterator:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<root>
    <item/>
    <item/>
    <item/>
    ...
</root>
```

Map values are generally encoded as XML attributes. For example, given this map:

```json
{
  "a": 1, 
  "b": 2, 
  "c": 3
}
```

the resulting XML would be as follows:

```xml
<item a="1" b="2" c="3"/>
```

Nested maps are encoded as sub-elements. For example, given this map:

```json
{
  "d": { 
    "e": 4,
    "f": 5
  }
}
```

the XML output would be as follows: 

```xml
<item>
    <d e="4" f="5"/>
</item>
```

Nested sequences are also supported. For example, this JSON:

```json
{
  "g": [
    {
      "h": 6
    },
    {
      "h": 7
    },
    {
      "h": 8
    }
  ]
}
```

would produce the following output:

```xml
<item>
    <g>
        <item h="6"/>
        <item h="7"/>
        <item h="8"/>
    </g>
</item>
```

Enums are encoded using their ordinal values. Instances of `java.util.Date` are encoded as a long value representing epoch time. All other values are encoded via `toString()`. Unsupported (i.e. non-map) sequence elements are ignored.

## BeanAdapter
The `BeanAdapter` class implements the `Map` interface and exposes any properties defined by a bean as entries in the map, allowing custom data structures to be easily serialized to JSON, CSV, or XML. 

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

If a property returns an instance of `Iterable` or `Map`, the value is wrapped in an adapter of the same type that automatically adapts its sub-elements. Otherwise, the value is assumed to be a bean and is wrapped in a `BeanAdapter`. Any property tagged with the `Ignore` annotation will be excluded from the map.

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

## ResultSetAdapter and Parameters
The `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing query results to be efficiently serialized to JSON, CSV, or XML. For example:

```java
JSONEncoder jsonEncoder = new JSONEncoder();

try (ResultSet resultSet = statement.executeQuery()) {
    jsonEncoder.write(new ResultSetAdapter(resultSet), getResponse().getOutputStream());
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

Arguments values are specified via the `apply()` method:

```java
HashMap<String, Object> arguments = new HashMap<>();

arguments("a", "hello");
arguments("b", 3);

parameters.apply(statement, arguments);
```

Once applied, the statement can be executed:

```java
return new ResultSetAdapter(statement.executeQuery());    
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
public void getPets(String owner) throws SQLException, IOException {
    try (Connection connection = DriverManager.getConnection(DB_URL)) {
        Parameters parameters = Parameters.parse("SELECT name, species, sex, birth FROM pet WHERE owner = :owner");

        HashMap<String, Object> arguments = new HashMap<>();

        arguments.put("owner", owner);

        try (PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            parameters.apply(statement, arguments);

            try (ResultSet resultSet = statement.executeQuery()) {
                JSONEncoder jsonEncoder = new JSONEncoder();
                
                jsonEncoder.write(new ResultSetAdapter(resultSet), getResponse().getOutputStream());
            }
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

### Nested Queries
`ResultSetAdapter` can also be used to return the results of nested queries. The `attach()` method assigns a subquery to a key in the result map:

```java
public void attach(String key, String subquery) { ... }
public void attach(String key, Parameters subquery) { ... }
```

Each attached query is executed once per row in the result set. The resulting rows are returned in a list that is associated with the corresponding key. 

Internally, subqueries are executed as prepared statements using the `Parameters` class. All values in the base row are supplied as parameter values to each subquery. 

An example based on the MySQL "employees" sample database is shown below. The base query retreives the employee's number, first name, and last name from the "employees" table. Subqueries to return the employee's salary and title history are optionally attached based on the values provided in the `details` parameter:

```java
@RequestMethod("GET")
@ResourcePath("?:employeeNumber")
public void getEmployee(List<String> details) throws SQLException, IOException {
    String employeeNumber = getKey("employeeNumber");

    Parameters parameters = Parameters.parse("SELECT emp_no AS employeeNumber, "
        + "first_name AS firstName, "
        + "last_name AS lastName "
        + "FROM employees WHERE emp_no = :employeeNumber");

    parameters.put("employeeNumber", employeeNumber);

    try (Connection connection = DriverManager.getConnection(DB_URL);
        PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {

        parameters.apply(statement);

        try (ResultSet resultSet = statement.executeQuery()) {
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(resultSet);

            for (String detail : details) {
                switch (detail) {
                    case "titles": {
                        resultSetAdapter.attach("titles", "SELECT title, "
                            + "from_date AS fromDate, "
                            + "to_date AS toDate "
                            + "FROM titles WHERE emp_no = :employeeNumber");

                        break;
                    }

                    case "salaries": {
                        resultSetAdapter.attach("salaries", "SELECT salary, "
                            + "from_date AS fromDate, "
                            + "to_date AS toDate "
                            + "FROM salaries WHERE emp_no = :employeeNumber");

                        break;
                    }
                }
            }

            getResponse().setContentType("application/json");

            JSONEncoder jsonEncoder = new JSONEncoder();

            jsonEncoder.write(resultSetAdapter.next(), getResponse().getOutputStream());
        }
    }
}
```

A sample response including both titles and salaries is shown below:

```json
{
  "employeeNumber": 10004,
  "firstName": "Chirstian",
  "lastName": "Koblick",
  "titles": [
    {
      "title": "Senior Engineer",
      "fromDate": 817794000000,
      "toDate": 253370782800000
    },
    ...
  ],
  "salaries": [
    {
      "salary": 74057,
      "fromDate": 1006837200000,
      "toDate": 253370782800000
    },
    ...
  ]
}
```

# Kotlin Support
In addition to Java, HTTP-RPC web services can be implemented using the [Kotlin](https://kotlinlang.org) programming language. For example, the following service provides some basic information about the host system:

```kotlin
@WebServlet(urlPatterns = ["/system-info/*"], loadOnStartup = 1)
class SystemInfoService : WebService() {
    class SystemInfo(
        val hostName: String,
        val hostAddress: String,
        val availableProcessors: Int,
        val freeMemory: Long,
        val totalMemory: Long
    )

    @RequestMethod("GET")
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

The API documentation for this service might look something like the following:

> ## /system-info
> 
> ```
> GET () -> SystemInfo
> ```
>
> ## SystemInfo
>
> ```
> {
>   hostAddress: string,
>   hostName: string,
>   availableProcessors: integer,
>   freeMemory: long,
>   totalMemory: long
> }
> ```

Data returned by the service might look like this:

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
