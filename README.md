[![Releases](https://img.shields.io/github/release/gk-brown/HTTP-RPC.svg)](https://github.com/gk-brown/HTTP-RPC/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.httprpc/httprpc-server.svg)](http://repo1.maven.org/maven2/org/httprpc/httprpc-server/)

# Introduction
HTTP-RPC is an open-source framework for implementing REST services in Java. It is extremely lightweight and requires only a Java runtime environment and a servlet container. The entire framework is distributed as a single JAR file that is less than 30KB in size, making it an ideal choice for applications such as microservices where a minimal footprint is desired.

This guide introduces the HTTP-RPC framework and provides an overview of its key features.

# Feedback
Feedback is welcome and encouraged. Please feel free to [contact me](mailto:gk_brown@icloud.com?subject=HTTP-RPC) with any questions, comments, or suggestions. Also, if you like using HTTP-RPC, please consider [starring](https://github.com/gk-brown/HTTP-RPC/stargazers) it!

# Contents
* [Getting HTTP-RPC](#getting-http-rpc)
* [HTTP-RPC Classes](#http-rpc-classes)
    * [DispatcherServlet](#dispatcherservlet)
    * [JSONEncoder](#jsonencoder)
    * [BeanAdapter](#beanadapter)
    * [ResultSetAdapter and Parameters](#resultsetadapter-and-parameters)
    * [IteratorAdapter](#iteratoradapter)
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

HTTP-RPC requires Java 8 or later and a servlet container supporting Java Servlet specification 3.1 or later.

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
    * `ResultSetAdapter` - adapter class that presents the contents of a JDBC result set as an iterable sequence of maps
    * `Parameters` - class for simplifying execution of prepared statements 
* `org.httprpc.util`
    * `IteratorAdapter` - adapter class that presents the contents of an iterator as an iterable sequence of values

These classes are explained in more detail in the following sections.

## DispatcherServlet
`DispatcherServlet` is an abstract base class for REST services. Service operations are defined by adding public methods to a concrete service implementation. 

Methods are invoked by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `DispatcherServlet` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the output stream as JSON.

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResourcePath` annotation can be used to associate the method with a specific path relative to the servlet. If unspecified, the method is associated with the servlet itself. 

Multiple methods may be associated with the same verb and path. `DispatcherServlet` selects the best method to execute based on the provided argument values. For example, the following class might be used to implement some simple addition operations:

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
* `java.util.Date`
* `java.util.time.LocalDate`
* `java.util.time.LocalTime`
* `java.util.time.LocalDateTime`
* `java.util.List`
* `java.net.URL`

`List` arguments represent multi-value parameters. List values are automatically converted to their declared types (e.g. `List<Double>`).

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

**IMPORTANT** Service classes must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

### Return Values
Return values are converted to their JSON equivalents as follows:

* `CharSequence`: string
* `Number`: number
* `Boolean`: true/false
* `java.util.Date`: long value representing epoch time in milliseconds
* `java.util.time.LocalDate`: "yyyy-mm-dd"
* `java.util.time.LocalTime`: "hh:mm"
* `java.util.time.LocalDateTime`: "yyyy-mm-ddThh:mm"
* `Iterable`: array
* `java.util.Map`: object

Methods may also return `void` or `Void` to indicate that they do not produce a value. Unsupported types are returned as `null`.

For example, this method returns a `Map` instance containing three values:

```java
@RequestMethod("GET")
@ResourcePath("/map")
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

### Exceptions
If an exception is thrown by a service method, an HTTP 500 response will be returned. If the response has not yet been committed, the exception message will be returned as plain text in the response body. This allows a service to provide the caller with insight into the cause of the failure. For example:

```java
@RequestMethod("GET")
@ResourcePath("/error")
public void generateError() throws Exception {
    throw new Exception("This is an error message.");
}
```

### Request and Repsonse Properties
`DispatcherServlet` provides the following methods to allow a service to access the request and response objects associated with the current operation:

    protected HttpServletRequest getRequest() { ... }
    protected HttpServletResponse getResponse() { ... }

For example, a service might access the request to get the name of the current user, or use the response to return a custom header.

The response object can also be used to produce a custom result. If a service method commits the response by writing to the output stream, the return value (if any) will be ignored by `DispatcherServlet`. This allows a service to return content that cannot be easily represented as JSON, such as image data or other response formats such as XML.

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
The `JSONEncoder` class is used internally by `DispatcherServlet` to serialize a JSON response. However, it can also be used by application code. For example, the following method would produce the same result as the map example shown earlier (albeit more verbosely):

```java
@RequestMethod("GET")
@ResourcePath("/map")
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

Values are converted to their JSON equivalents as described earlier.

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

An example service method that returns a `TreeNode` structure is shown below:

```java
@RequestMethod("GET")
public Map<String, ?> getTree() {
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

    return new BeanAdapter(root);
}
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

## ResultSetAdapter and Parameters
The `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing query results to be serialized as an array of JSON objects. For example:

```java
JSONEncoder jsonEncoder = new JSONEncoder();

try (ResultSet resultSet = statement.executeQuery()) {
    jsonEncoder.writeValue(new ResultSetAdapter(resultSet), getResponse().getOutputStream());
}
```

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

Parameter values are specified via the `put()` method:

```java
parameters.put("a", "hello");
parameters.put("b", 3);
```

The values are applied to the statement via the `apply()` method:

```java
parameters.apply(statement);
```

Once applied, the statement can be executed:

```java
return new ResultSetAdapter(statement.executeQuery());    
```

A complete example that uses both classes is shown below. It is based on the MySQL sample database, and retrieves a list of all pets belonging to a given owner:

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

## IteratorAdapter
The `IteratorAdapter` class implements the `Iterable` interface and makes each value produced by an iterator appear to be an element of the adapter, allowing the iterator's contents to be serialized as a JSON array.

`IteratorAdapter` is typically used to transform result data produced by NoSQL databases such as MongoDB. For example, the following method (based on the MongoDB sample database) returns a list of restaurants in a given zip code:

```java
@RequestMethod("GET")
public void getRestaurants(String zipCode) throws IOException {
    MongoDatabase db = mongoClient.getDatabase("test");

    FindIterable<Document> iterable = db.getCollection("restaurants").find(new Document("address.zipcode", zipCode));

    try (MongoCursor<Document> cursor = iterable.iterator()) {
        JSONEncoder jsonEncoder = new JSONEncoder();

        jsonEncoder.writeValue(new IteratorAdapter(cursor), getResponse().getOutputStream());
    } finally {
        getResponse().flushBuffer();
    }
}
```

The service would return something like the following:

```json
[
  {
    "_id": null,
    "name": "Morris Park Bake Shop",
    "restaurant_id": "30075445",
    "address": {
      "building": "1007",
      "coord": [
        -73.856077,
        40.848447
      ],
      "street": "Morris Park Ave",
      "zipcode": "10462"
    },
    "borough": "Bronx",
    "cuisine": "Bakery",
    "grades": [
      {
        "date": 1393804800000,
        "grade": "A",
        "score": 2
      },
      {
        "date": 1378857600000,
        "grade": "A",
        "score": 6
      },
      {
        "date": 1358985600000,
        "grade": "A",
        "score": 10
      },
      {
        "date": 1322006400000,
        "grade": "A",
        "score": 9
      },
      {
        "date": 1299715200000,
        "grade": "B",
        "score": 14
      }
    ]
  },
  ...  
```

Note that the value of "_id" is `null` because MongoDB's `ObjectId` class is not a valid JSON type.

### Adapting Streams
`IteratorAdapter` can also be used to transform the result of stream operations on Java collection types. For example:

```java
@RequestMethod("GET")
public Iterable<?> getStream() {
    return new IteratorAdapter(Arrays.asList("a", "b", "c").stream().iterator());
}
```

A call to this service method would produce the following:

```json
[
  "a",
  "b",
  "c"
]
```

# Additional Information
This guide introduced the HTTP-RPC framework and provided an overview of its key features. For additional information, see the the [examples](https://github.com/gk-brown/HTTP-RPC/tree/master/httprpc-server-test/src/org/httprpc/test).
