# Introduction
HTTP-RPC is an open-source framework for simplifying development of REST-based applications. It allows developers to create and access HTTP-based web services using a convenient, RPC-like metaphor while preserving fundamental REST principles such as statelessness and uniform resource access.

The project currently includes support for implementing REST services in Java and consuming services in Java, Objective-C/Swift, or JavaScript. The server component provides a lightweight alternative to other, larger Java-based REST frameworks, and the consistent cross-platform client API makes it easy to interact with services regardless of target device or operating system. 

# Contents
* [Service Operations](#service-operations)
* [Implementations](#implementations)
	* [Java Server](#java-server)
	* [Java Client](#java-client)
	* [Objective-C/Swift Client](#objective-cswift-client)
	* [JavaScript Client](#javascript-client)
* [Additional Information](#additional-information)

# Service Operations
HTTP-RPC services are accessed by applying an HTTP verb such as `GET` or `POST` to a target resource. The target is specified by a path representing the name of the resource, and is generally expressed as a noun such as _/calendar_ or _/contacts_. 

Arguments are provided either via the query string or in the request body, like an HTML form. Although services may produce any type of content, results are generally returned as JSON. Operations that do not return a value are also supported.

## GET
The `GET` method is used to retrive information from the server. `GET` arguments are passed in the query string. For example, the following request might be used to obtain data about a calendar event:

    GET /calendar?eventID=101

This request might retrieve the sum of two numbers, whose values are specified by the `a` and `b` query arguments:

    GET /math/sum?a=2&b=4

Alternatively, the argument values could be specified as a list rather than as two fixed variables:

    GET /math/sum?values=1&values=2&values=3
    
In either case, the service would return the value 6 in response.

## POST
The `POST` method is typically used to add new information to the server. For example, the following request might be used to create a new calendar event:

    POST /calendar

As with HTML forms, `POST` arguments are passed in the request body. If the arguments contain only text values, they can be encoded using the "application/x-www-form-urlencoded" MIME type:

    title=Planning+Meeting&start=2016-06-28T14:00&end=2016-06-28T15:00

If the arguments contain binary data such as a JPEG or PNG image, the "multipart/form-data" encoding can be used.

While it is not required, `POST` requests that create resources often return a value that can be used to identify the resource for later retrieval, update, or removal.

## PUT
The `PUT` method updates existing information on the server. `PUT` arguments are passed in the query string. For example, the following request might be used to modify the end date of a calendar event:

    PUT /calendar?eventID=102&end=2016-06-28T15:30

`PUT` requests generally do not return a value.

## DELETE
The `DELETE` method removes information from the server. `DELETE` arguments are passed in the query string. For example, this request might be used to delete a calendar event:

    DELETE /calendar?eventID=102

`DELETE` requests generally do not return a value.

## Response Codes
Although the HTTP specification defines a large number of possible response codes, only a few are applicable to HTTP-RPC services:

* _200 OK_ - The request succeeded, and the response contains a value representing the result
* _204 No Content_ - The request succeeded, but did not produce a result
* _404 Not Found_ - The requested resource does not exist
* _405 Method Not Allowed_ - The resource exists, but does not support the requested method
* _406 Not Acceptable_ - The requested representation is not available
* _500 Internal Server Error_ - An error occurred while executing the method

# Implementations
Support currently exists for implementing HTTP-RPC services in Java, and consuming services in Java, Objective-C/Swift, or JavaScript. For examples and additional information, please see the [wiki](https://github.com/gk-brown/HTTP-RPC/wiki).

## Java Server
The Java server library allows developers to create and publish HTTP-RPC web services in Java. It is distributed as a JAR file that contains the following core classes:

* _`org.httprpc`_
    * `WebService` - abstract base class for HTTP-RPC services
    * `RPC` - annotation that specifies a "remote procedure call", or web service method
    * `RequestDispatcherServlet` - servlet that dispatches requests to service instances
    * `Encoder ` - interface representing a content encoder
    * `JSONEncoder` - class that encodes a JSON response
    * `Encoding` - annotation that specifies a custom encoding
* _`org.httprpc.beans`_
    * `BeanAdapter` - adapter class that presents the contents of a Java Bean instance as a map, suitable for serialization to JSON
* _`org.httprpc.sql`_
    * `ResultSetAdapter` - adapter class that presents the contents of a JDBC result set as an iterable list, suitable for streaming to JSON
    * `Parameters` - class for simplifying execution of prepared statements
* _`org.httprpc.util`_
    * `IteratorAdapter` - adapter class that presents the contents of an iterator as an iterable list, suitable for streaming to JSON

Additionally, the server library provides the following classes for use with templates, which allow service data to be declaratively transformed into alternate representations:

* _`org.httprpc`_
    * `Template` - annotation that associates a template with a service method
* _`org.httprpc.template`_
    * `TemplateEncoder` - class for processing template documents
    * `Modifier` - interface representing a template modifier

Each of these classes is discussed in more detail below. 

The JAR file for the Java server implementation of HTTP-RPC can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 8 and a servlet container supporting servlet specification 3.1 (e.g. Tomcat 8) or later are required.

### WebService Class
`WebService` is an abstract base class for HTTP-RPC web services. All services must extend this class and must provide a public, zero-argument constructor.

Service operations are defined by adding public methods to a concrete service implementation. The `RPC` annotation is used to flag a method as remotely accessible. This annotation associates an HTTP verb and a resource path with the method. All public annotated methods automatically become available for remote execution when the service is published.

For example, the following class might be used to implement the simple addition operations discussed in the previous section:

    public class MathService extends WebService {
        @RPC(method="GET", path="sum")
        public double getSum(double a, double b) {
            return a + b;
        }
    
        @RPC(method="GET", path="sum")
        public double getSum(List<Double> values) {
            double total = 0;
    
            for (double value : values) {
                total += value;
            }
    
            return total;
        }
    }
    
Note that both methods are mapped to the path _/math/sum_. The `RequestDispatcherServlet` class discussed in the next section selects the best method to execute based on the names of the provided argument values. For example, the following request would cause the first method to be invoked:

    GET /math/sum?a=2&b=4
    
This request would invoke the second method:

    GET /math/sum?values=1&values=2&values=3

#### Method Arguments
Method arguments may be any of the following types:

* `byte`/`java.lang.Byte`
* `short`/`java.lang.Short`
* `int`/`java.lang.Integer`
* `long`/`java.lang.Long`
* `float`/`java.lang.Float`
* `double`/`java.lang.Double`
* `boolean`/`java.lang.Boolean`
* `java.lang.String`
* `java.net.URL`
* `java.util.List`

`URL` arguments represent binary content provided by the caller and can only be used with `POST` requests submitted using the "multipart/form-data" encoding. List arguments may be used with any request type, but list elements must be a supported simple type; e.g. `List<Double>` or `List<URL>`.

Omitting the value of a primitive parameter results in an argument value of 0 for that parameter. Omitting the value of a simple reference type produces a null argument value for that parameter. Omitting all values for a list parameter produces an empty list argument for the parameter.

#### Return Values
Methods may return any of the following types:

* `byte`/`java.lang.Byte`
* `short`/`java.lang.Short`
* `int`/`java.lang.Integer`
* `long`/`java.lang.Long`
* `float`/`java.lang.Float`
* `double`/`java.lang.Double`
* `boolean`/`java.lang.Boolean`
* `java.lang.CharSequence`
* `java.util.List`
* `java.util.Map`

Methods may also return `void` or `java.lang.Void` to indicate that they do not produce a value.

`Map` implementations must use `String` values for keys. Nested structures are supported, but reference cycles are not permitted.

`List` and `Map` types are not required to support random access; iterability is sufficient. Additionally, `List` and `Map` types that implement `java.lang.AutoCloseable` will be automatically closed after their values have been written to the output stream. This allows service implementations to stream response data rather than buffering it in memory before it is written. 

For example, the `ResultSetAdapter` class wraps an instance of `java.sql.ResultSet` and exposes its contents as a forward-scrolling, auto-closeable list of map values. Closing the list also closes the underlying result set, ensuring that database resources are not leaked. `ResultSetAdapter` is discussed in more detail later.

#### Request Metadata
`WebService` provides the following methods that allow an extending class to obtain additional information about the current request:

* `getLocale()` - returns the locale associated with the current request
* `getUserName()` - returns the user name associated with the current request, or `null` if the request was not authenticated
* `getUserRoles()` - returns a set representing the roles the user belongs to, or `null` if the request was not authenticated

The values returned by these methods are populated via protected setters, which are called once per request by `RequestDispatcherServlet`. These setters are not meant to be called by application code. However, they can be used to facilitate unit testing of service implementations by simulating a request from an actual client. 

### RequestDispatcherServlet Class
HTTP-RPC services are published via the `RequestDispatcherServlet` class. This class is resposible for translating HTTP request parameters to method arguments, invoking the specified method, and serializing the return value to JSON. Note that service classes must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

Java objects are mapped to their JSON equivalents as follows:

* `java.lang.Number` or numeric primitive: number
* `java.lang.Boolean` or boolean primitive: true/false
* `java.lang.CharSequence`: string
* `java.util.List`: array
* `java.util.Map`: object

Internally, `RequestDispatcherServlet` uses the `JSONEncoder` class to transform method results to JSON. This class can also be used by application code to write JSON data to arbitrary output streams.

Each servlet instance hosts a single HTTP-RPC service. The name of the service type is passed to the servlet via the "serviceClassName" initialization parameter. For example:

	<servlet>
	    <servlet-name>MathServlet</servlet-name>
	    <servlet-class>org.httprpc.RequestDispatcherServlet</servlet-class>
        <init-param>
            <param-name>serviceClassName</param-name>
            <param-value>com.example.MathService</param-value>
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>MathServlet</servlet-name>
        <url-pattern>/math/*</url-pattern>
    </servlet-mapping>

A new service instance is created and initialized for each request. `RequestDispatcherServlet` converts the request parameters to the argument types expected by the named method, invokes the method, and writes the return value to the response stream as JSON.

If the method completes successfully and returns a value, an HTTP 200 status code is returned. If the method returns `void` or `Void`, HTTP 204 is returned.

If the requested resource does not exist, the servlet returns an HTTP 404 status code. If the resource exists but does not support the requested method, HTTP 405 is returned. 

If any exception is thrown while executing the method, HTTP 500 is returned. If an exception is thrown while serializing the response, the output is truncated. In either case, the exception is logged.

Servlet security is provided by the underlying servlet container. See the Java EE documentation for more information.

#### Custom Encodings
The `Encoding` annotation is used to associate a custom encoder with a service method. This allows an application to effectively extend the set of supported return types. 

The annotation defines a single element representing the type of the encoder that will be used to serialize the return value. This type must implement the `Encoder` interface. For example:

    @RPC(method="GET", path="/customValue")
    @Encoding(CustomEncoder.class)
    public CustomType getCustomType() { ... }

All requests for `/customValue` will return the representation of `CustomType` as defined by the `CustomEncoder` type.

While custom encodings offer a great deal of flexibility, many common use cases can be addressed using the various adapter types provided by the framework. These adapters are discussed in more detail below. 

Templates are another means for customizing a resource's representation. They are discussed in a later section.

### BeanAdapter Class
The `BeanAdapter` class allows the contents of a Java Bean object to be returned from a service method. This class implements the `Map` interface and exposes any properties defined by the Bean as entries in the map, allowing custom data types to be serialized to JSON.

For example, the following Bean class might be used to represent basic statistical data about a collection of values:

    public class Statistics {
        private int count = 0;
        private double sum = 0;
        private double average = 0;
    
        public int getCount() {
            return count;
        }
    
        public void setCount(int count) {
            this.count = count;
        }
    
        public double getSum() {
            return sum;
        }
    
        public void setSum(double sum) {
            this.sum = sum;
        }
    
        public double getAverage() {
            return average;
        }
    
        public void setAverage(double average) {
            this.average = average;
        }
    }

Using this class, an implementation of a `getStatistics()` method might look like this:

    @RPC(method="GET", path="statistics")
    public Map<String, ?> getStatistics(List<Double> values) {    
        Statistics statistics = new Statistics();

        int n = values.size();

        statistics.setCount(n);

        for (int i = 0; i < n; i++) {
            statistics.setSum(statistics.getSum() + values.get(i));
        }

        statistics.setAverage(statistics.getSum() / n);

        return new BeanAdapter(statistics);
    }

Although the values are actually stored in the strongly typed `Statistics` object, the adapter makes the data appear as a map, allowing it to be returned to the caller as a JSON object; for example:

    {
        "average": 3.0, 
        "count": 3, 
        "sum": 9.0
    }

Note that, if a property returns a nested Bean type, the property's value will be automatically wrapped in a `BeanAdapter` instance. Additionally, if a property returns a `List` or `Map` type, the value will be wrapped in an adapter of the appropriate type that automatically adapts its sub-elements. This allows service methods to return recursive structures such as trees.

### ResultSetAdapter Class
The `ResultSetAdapter` class allows the result of a SQL query to be efficiently returned from a service method. This class implements the `List` interface and makes each row in a JDBC result set appear as an instance of `Map`, rendering the data suitable for serialization to JSON. It also implements the `AutoCloseable` interface, to ensure that the underlying result set is closed and database resources are not leaked.

`ResultSetAdapter` is forward-scrolling only; its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a result set to be returned directly to the caller without any intermediate buffering. The caller can simply execute a JDBC query, pass the resulting result set to the `ResultSetAdapter` constructor, and return the adapter instance:

    @RPC(method="GET", path="data")
    public ResultSetAdapter getData() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from some_table");
        
        return new ResultSetAdapter(resultSet);
    }

If a column's label contains a period, the value will be returned as a nested structure. For example, the following query might be used to retrieve a list of employees:

    SELECT first_name AS 'name.first', last_name AS 'name.last', title FROM employees
    
Because the aliases for the `first_name` and `last_name` columns contain a period, each row will contain a nested "name" structure instead of a flat collection of key/value pairs; for example:

    [
      {
        "name": {
          "first": "John",
          "last": "Smith"
        },
        "title": "Manager"
      },
      ...
    ]

### Parameters Class
The `Parameters` class provides a means for executing prepared statements using named parameter values rather than indexed arguments. Parameter names are specified by a leading `:` character. For example:

    SELECT * FROM some_table 
    WHERE column_a = :a OR column_b = :b OR column_c = COALESCE(:c, 4.0)
    
The `parse()` method is used to create a `Parameters` instance from a SQL statement. It takes a string or reader containing the SQL text as an argument; for example:

    Parameters parameters = Parameters.parse(sql);

The `getSQL()` method returns the parsed SQL in standard JDBC syntax:

    SELECT * FROM some_table 
    WHERE column_a = ? OR column_b = ? OR column_c = COALESCE(?, 4.0)

This value is used to create the actual prepared statement:

    PreparedStatement statement = DriverManager.getConnection(url).prepareStatement(parameters.getSQL());

Parameter values are specified via a map passed to the `apply()` method:

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("a", "hello");
    arguments.put("b", 3);
    
    parameters.apply(statement, arguments);

Since explicit creation and population of the argument map can be cumbersome, the `WebService` class provides the following static convenience methods to help simplify map creation:

    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) { ... }
    public static <K> Map.Entry<K, ?> entry(K key, Object value) { ... }
    
Using the convenience methods, the code that applies the parameter values can be reduced to the following:

    parameters.apply(statement, mapOf(entry("a", "hello"), entry("b", 3)));

Once applied, the statement can be executed:

    return new ResultSetAdapter(statement.executeQuery());    

### IteratorAdapter Class
The `IteratorAdapter` class allows the content of an arbitrary cursor to be efficiently returned from a service method. This class implements the `List` interface and adapts each element produced by the iterator for serialization to JSON, including nested `List` and `Map` structures. Like `ResultSetAdapter`, `IteratorAdapter` implements the `AutoCloseable` interface. If the underlying iterator type also implements `AutoCloseable`, `IteratorAdapter` will ensure that the underlying cursor is closed so that resources are not leaked.

As with `ResultSetAdapter`, `IteratorAdapter` is forward-scrolling only, so its contents are not accessible via the `get()` and `size()` methods. This allows the contents of a cursor to be returned directly to the caller without any intermediate buffering.

`IteratorAdapter` is typically used to serialize result data produced by NoSQL databases.

### Templates
Although data produced by an HTTP-RPC web service is usually returned to the caller as JSON, it can also be transformed into other representations via "templates". Templates are documents that describe an output format, such as HTML, XML, or CSV. They are merged with result data at execution time to create the final response that is sent back to the caller.

HTTP-RPC templates are based on the [CTemplate](https://github.com/OlafvdSpek/ctemplate) system, which defines a set of "markers" that are replaced with values supplied by a "data dictionary" when the template is processed. The following CTemplate marker types are supported by HTTP-RPC:

* {{_variable_}} - injects a variable from the data dictionary into the output
* {{#_section_}}...{{/_section_}} - defines a repeating section of content
* {{>_include_}} - imports content specified by another template
* {{!_comment_}} - provides informational text about a template's content

The value returned by a service method represents the data dictionary. Usually, this will be an instance of `java.util.Map` whose keys represent the values provided by the dictionary. For example, a simple template for transforming the output of the `getStatistics()` method discussed earlier into HTML is shown below:

    <html>
    <head>
        <title>Statistics</title>
    </head>
    <body>
        <p>Count: {{count}}</p>
        <p>Sum: {{sum}}</p>
        <p>Average: {{average}}</p> 
    </body>
    </html>

This method returns a map containing the result of some simple statistical calculations:

    {
        "average": 3.0, 
        "count": 3, 
        "sum": 9.0
    }

At execution time, the "count", "sum", and "average" variable markers will be replaced by their corresponding values from the data dictionary, producing the following markup:

    <html>
    <head>
        <title>Statistics</title>
    </head>
    <body>
        <p>Count: 3.0</p>
        <p>Sum: 9.0</p>
        <p>Average: 3.0</p> 
    </body>
    </html>

#### Dot Notation
Although maps are often used to provide a template's data dictionary, this is not strictly required. Non-map values are automatically wrapped in a map instance and assigned a default name of ".". This name can be used to refer to the value in a template. 

For example, the following template could be used to transform output of a method that returns a `double` value:

	The value is {{.}}.

If the value returned by the method is the number `8`, the resulting output would look like this:

	The value is 8.

#### Template Documents
The `Template` annotation is used to associate a template document with a method. The annotation's value represents the name and type of the template that will be applied to the results. For example:

    @RPC(method="GET", path="statistics")
    @Template(name="statistics.html", contentType="text/html")
    public Map<String, ?> getStatistics(List<Double> values) { ... }

The `name` element refers to the file containing the template definition. It is specified as a resource path relative to the service type.

The `contentType` element indicates the type of the content produced by the named template. It is used by `RequestDispatcherServlet` to identify the requested template. A specific representation is requested by appending a file extension associated with the desired MIME type to the service name in the URL; for example, _/math/statistics.html_.

The optional `userAgent` element can be used to associate a template with a particular user agent string. This value is a regular expression that is matched against the `User-Agent` header provided by the caller. The default value matches all user agents.

Note that it is possible to associate multiple templates with a single service method. For example, the following code associates an additional XML template with the `getStatistics()` method:

    @RPC(method="GET", path="statistics")
    @Template(name="statistics.html", contentType="text/html")
    @Template(name="statistics.xml", contentType="application/xml")
    public Map<String, ?> getStatistics(List<Double> values) { ... }

The `TemplateEncoder` class is responsible for merging a template document with a data dictionary. Although it is used internally by HTTP-RPC to transform annotated method results, it can also be used by application code to perform arbitrary transformations. See the Javadoc for more information.

#### Variable Markers
Variable markers inject a variable from the data dictionary into the output. They can be used to refer to any simple dictionary value (i.e. number, boolean, or character sequence). Nested values can be referred to using dot-separated path notation; e.g. "name.first". Missing (i.e. `null`) values are replaced with the empty string in the generated output. 

##### Resource References
Variable names beginning with the `@` character represent "resource references". Resources allow static template content to be localized. At execution time, the template processor looks for a resource bundle with the same base name as the service type, using the locale specified by the current HTTP request. If the bundle exists, it is used to provide a localized string value for the variable.

For example, the descriptive text from _statistics.html_ could be localized as follows:

    title=Statistics
    count=Count
    sum=Sum
    average=Average

The template could be updated to refer to these string resources as shown below:

    <html>
    <head>
        <title>{{@title}}</title>
    </head>
    <body>
        <p>{{@count}}: {{count}}</p>
        <p>{{@sum}}: {{sum}}</p>
        <p>{{@average}}: {{average}}</p> 
    </body>
    </html>

When the template is processed, the resource references will be replaced with their corresponding values from the resource bundle.

##### Context References
Variable names beginning with the `$` character represent "context references". Context properties provide information about the context in which the request is executing. HTTP-RPC provides the following context values:

* `scheme` - the scheme used to make the request; e.g. "http" or "https"
* `serverName` - the host name of the server to which the request was sent
* `serverPort` - the port to which the request was sent
* `contextPath` - the context path of the web application handling the request

For example, the following markup uses the `contextPath` value to embed a product image in an HTML template:

    <img src="{{$contextPath}}/images/{{productID}}.jpg"/>

##### Modifiers
The CTemplate specification defines a syntax for applying an optional set of "modifiers" to a variable. Modifiers are used to transform a variable's representation before it is written to the output stream; for example, to apply an escape sequence.

Modifiers are specified as shown below. They are invoked in order from left to right. An optional argument value may be included to provide additional information to the modifier:

    {{variable:modifier1:modifier2:modifier3=argument:...}}

HTTP-RPC provides the following set of standard modifiers:

* `format` - applies a format string
* `^html`, `^xml` - applies markup encoding to a value
* `^json` - applies JSON encoding to a value
* `^csv` - applies CSV encoding to a value
* `^url` - applies URL encoding to a value

For example, the following marker applies a format string to a value and then URL-encodes the result:

    {{value:format=0x%04x:^url}}

In addition to `printf()`-style formatting, the `format` modifier also supports the following arguments for numeric values:

  * `currency` - applies a currency format
  * `percent` - applies a percent format
  * `shortDate` - applies a short date format
  * `mediumDate` - applies a medium date format
  * `longDate` - applies a long date format
  * `fullDate` - applies a full date format
  * `shortTime` - applies a short time format
  * `mediumTime` - applies a medium time format
  * `longTime` - applies a long time format
  * `fullTime` - applies a full time format

For example, this marker applies a medium date format to a long value named "date":

    {{date:format=mediumDate}}

Applications may also define their own custom modifiers. Modifiers are created by implementing the `Modifier` interface, which defines the following method:

    public Object apply(Object value, String argument, Locale locale);
    
The first argument to this method represents the value to be modified, and the second is the optional argument value following the `=` character in the modifier string. If a modifier argument is not specified, the value of `argument` will be null. The third argument contains the caller's locale.

For example, the following class implements a modifier that converts values to uppercase:

    public class UppercaseModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale) {
            return value.toString().toUpperCase(locale);
        }
    }

Custom modifiers are registered by adding them to the modifier map returned by `TemplateEncoder#getModifiers()`. The map key represents the name that is used to apply a modifier in a template document. For example:

	TemplateEncoder.getModifiers().put("uppercase", new UppercaseModifier());

Note that modifiers must be thread-safe, since they are shared and may be invoked concurrently by multiple template engines.

#### Section Markers
Section markers define a repeating section of content. The marker name must refer to a list value in the data dictionary. Content between the markers is repeated once for each element in the list, and the element becomes the data dictionary for each successive iteration through the section. If the list is missing (i.e. `null`) or empty, the section's content is excluded from the output.

For example, a service that provides information about homes for sale might return a list of available properties as follows:

    [
        {
            "streetAddress": "17 Cardinal St.",
            "listPrice": 849000,
            "numberOfBedrooms": 4,
            "numberOfBathrooms": 3
        },
        {
            "streetAddress": "72 Wedgemere Ave.",
            "listPrice": 1650000,
            "numberOfBedrooms": 5,
            "numberOfBathrooms": 3
        },
        ...
    ]
    
A template to present these results in an HTML table is shown below. Dot notation is used to refer to the list itself, and variable markers are used to refer to the properties of the list elements. The `format` modifier is used to present the list price as a localized currency value:

    <html>
    <head>
        <title>Property Listings</title>
    </head>
    <body>
    <table>
    <tr>
        <td>Street Address</td> 
        <td>List Price</td> 
        <td># Bedrooms</td> 
        <td># Bathrooms</em></td> 
    </tr>
    {{#.}}
    <tr>
        <td>{{streetAddress}}</td> 
        <td>{{listPrice:format=currency}}</td> 
        <td>{{numberOfBedrooms}}</td> 
        <td>{{numberOfBathrooms}}</td>
    </tr>
    {{/.}}
    </table>
    </body>
    </html>

#### Includes
Include markers import content defined by another template. They can be used to create reusable content modules; for example, document headers and footers.

For example, the following template, _hello.txt_, includes another document named _world.txt_: 

    Hello, {{>world.txt}}!
    
When _hello.txt_ is processed, the include marker will be replaced with the contents of _world.txt_. For example, if _world.txt_ contains the text "World", the result of processing _hello.txt_ would be the following:

	Hello, World!

Includes inherit their context from the parent document, so they can refer to elements in the parent's data dictionary. This allows includes to be parameterized.

Includes can also be used to facilitate recursion. For example, an include that includes itself could be used to transform the output of a method that returns a hierarchical data structure:

    public class TreeNode {
        public String getName() { ... }    
        public List<TreeNode> getChildren() { ... }
    }

The result of processing the following template, _treenode.html_, would be a collection of nested unordered list elements representing each of the nodes in the tree:

    <ul>
    {{#children}}
    <li>
    <p>{{name}}</p>
    {{>treenode.html}}
    </li>
    {{/children}}
    </ul>

#### Comments
Comment markers provide informational text about a template's content. They are not included in the final output. For example, when the following template is processed, only the content between the `<p>` tags will be included:

    {{! Some placeholder text }}
    <p>Lorem ipsum dolor sit amet.</p>

## Java Client
The Java client library enables Java applications (including Android) to consume HTTP-RPC web services. It is distributed as a JAR file that includes the following types, discussed in more detail below:

* _`org.httprpc`_
    * `WebServiceProxy` - invocation proxy for HTTP-RPC services
    * `ResultHandler` - callback interface for handling results
    * `Result` - abstract base class for typed results
    * `Authentication` - interface representing an authentication provider
    * `BasicAuthentication` - HTTP basic authentication provider
    * `Decoder` - interface representing a content decoder
    * `JSONDecoder` - class that decodes a JSON response

The JAR file for the Java client implementation of HTTP-RPC can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). Java 7 or later is required.

### WebServiceProxy Class
The `WebServiceProxy` class acts as a client-side invocation proxy for HTTP-RPC web services. Internally, it uses an instance of `HttpURLConnection` to send and receive data. `POST` requests are encoded as "multipart/form-data".

`WebServiceProxy` provides a single constructor that takes the following arguments:

* `serverURL` - an instance of `java.net.URL` representing the URL of the server
* `executorService` - an instance of `java.util.concurrent.ExecutorService` that is used to  dispatch service requests

Service operations are initiated by calling the `invoke()` method:
    
    public <V> Future<V> invoke(String method, String path, 
        Map<String, ?> arguments,  
        ResultHandler<V> resultHandler) { ... }

This method takes the following arguments:

* `method` - the HTTP method to execute
* `path` - the resource path
* `arguments` - a map containing the request arguments as key/value pairs
* `resultHandler` - an instance of `ResultHandler` that will be invoked upon completion of the service operation

A convenience method is also provided for executing operations that don't take any arguments:

    public <V> Future<V> invoke(String method, String path, 
        ResultHandler<V> resultHandler) { ... }

Both variants of the `invoke()` method return an instance of `java.util.concurrent.Future` representing the invocation request. This object allows a caller to cancel an outstanding request as well as obtain information about a request that has completed.

#### Arguments and Return Values
Request arguments may be any of the following types:

* `java.lang.Number`
* `java.lang.Boolean`
* `java.lang.String`
* `java.net.URL`
* `java.util.List`

URL arguments represent binary content and can only be used with `POST` requests. List arguments may be used with any request type, but list elements must be a supported simple type; e.g. `List<Double>` or `List<URL>`.

The result handler is called upon completion of the operation. `ResultHandler` is a functional interface whose single method, `execute()`, is defined as follows:

    public void execute(V result, Exception exception);

On successful completion, the first argument will contain the result of the operation. This will typically be an instance of one of the following types or `null`, depending on the response returned by the server:

* string: `java.lang.String`
* number: `java.lang.Number`
* true/false: `java.lang.Boolean`
* array: `java.util.List`
* object: `java.util.Map`

The second argument will be `null` in this case. If an error occurs, the first argument will be `null` and the second will contain an exception representing the error that occurred.

Internally, `WebServiceProxy ` uses the `JSONDecoder` class to deserialize JSON response data returned by a service operation. This class can also be used by application code to read JSON data from arbitrary input streams.

Subclasses of `WebServiceProxy` can override the `decodeResponse()` method to provide custom deserialization behavior. For example, an Android client could override this method to support `Bitmap` data: 

    @Override
    protected Object decodeResponse(InputStream inputStream, String contentType) throws IOException {
        Object value;
        if (contentType != null && contentType.startsWith("image/")) {
            value = BitmapFactory.decodeStream(inputStream);
        } else {
            value = super.decodeResponse(inputStream, contentType);
        }

        return value;
    }

#### Argument Map Creation
Since explicit creation and population of the argument map can be cumbersome, `WebServiceProxy` provides the following static convenience methods to help simplify map creation:

    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) { ... }
    public static <K> Map.Entry<K, ?> entry(K key, Object value) { ... }
    
Using these convenience methods, argument map creation can be reduced from this:

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("a", 2);
    arguments.put("b", 4);
    
to this:

    Map<String, Object> arguments = mapOf(entry("a", 2), entry("b", 4));
    
A complete example is provided later.

#### Multi-Threading Considerations
By default, a result handler is called on the thread that executed the remote request, which in most cases will be a background thread. However, user interface toolkits generally require updates to be performed on the main thread. As a result, handlers typically need to "post" a message back to the UI thread in order to update the application's state. For example, a Swing application might call `SwingUtilities#invokeAndWait()`, whereas an Android application might call `Activity#runOnUiThread()` or `Handler#post()`.

While this can be done in the result handler itself, `WebServiceProxy` provides a more convenient alternative. The protected `dispatchResult()` method can be overridden to process all result handler notifications. For example, the following Android-specific code ensures that all result handlers will be executed on the main UI thread:

    serviceProxy = new WebServiceProxy(serverURL, Executors.newSingleThreadExecutor()) {
        private Handler handler = new Handler(Looper.getMainLooper());

        @Override
        protected void dispatchResult(Runnable command) {
            handler.post(command);
        }
    };

Similar dispatchers can be configured for other Java UI toolkits such as Swing, JavaFX, and SWT. Command line applications can generally use the default dispatcher, which simply performs result handler notifications on the current thread.

### Result Class
`Result` is an abstract base class for typed results. Using this class, applications can easily map untyped object data returned by a service operation to typed values. It provides the following constructor that is used to populate Java Bean property values from map entries:

    public Result(Map<String, Object> properties) { ... }
    
For example, the following Java class might be used to provide a typed version of the statistical data returned by the `getStatistics()` method discussed earlier:

    public class Statistics extends Result {
        private int count;
        private double sum;
        private double average;

        public Statistics(Map<String, Object> properties) {
            super(properties);
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public double getSum() {
            return sum;
        }

        public void setSum(double sum) {
            this.sum = sum;
        }

        public double getAverage() {
            return average;
        }

        public void setAverage(double average) {
            this.average = average;
        }
    }

The map data returned by `getStatistics()` can be converted to a `Statistics` instance as follows:

    serviceProxy.invoke("getStatistics", (Map<String, Object> result, Exception exception) -> {
        Statistics statistics = new Statistics(result);

        System.out.println(statistics.getCount());
        System.out.println(statistics.getSum());
        System.out.println(statistics.getAverage());
    });

Additionally, the `Result` class provides the following static method for accessing nested map values by key path (e.g. "foo.bar"):

    public static <V> V getValue(Map<String, ?> root, String path) { ... }

See the Javadoc for more information.

### Authentication
Although it is possible to use the `java.net.Authenticator` class to authenticate service requests, this class can be difficult to work with, especially when dealing with multiple concurrent requests or authenticating to multiple services with different credentials. It also requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `Authentication` interface defines a single method that is used to authenticate each request submitted by a proxy instance:

    public interface Authentication {
        public void authenticateRequest(HttpURLConnection connection);
    }

Authentication providers are associated with a proxy instance via the `setAuthentication()` method of the `WebServiceProxy` class. For example, the following code associates an instance of `BasicAuthentication` with a service proxy:

    serviceProxy.setAuthentication(new BasicAuthentication("username", "password"));

The `BasicAuthentication` class is provided by the HTTP-RPC Java client library. Applications may provide custom implementations of the `Authentication` interface to support other authentication schemes.

### Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to access the resources of the hypothetical math service discussed earlier. It first creates an instance of the `WebServiceProxy` class and configures it with a pool of ten threads for executing requests. It then invokes the `getSum(double, double)` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `getSum(List<Double>)` method, passing the values 1, 2, and 3 as arguments:

    // Create service proxy
    URL serverURL = new URL("https://localhost:8443");
    ExecutorService executorService = Executors.newFixedThreadPool(10);

    WebServiceProxy serviceProxy = new WebServiceProxy(serverURL, executorService);

    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("a", 2), entry("b", 4)), new ResultHandler<Number>() {
        @Override
        public void execute(Number result, Exception exception) {
            // result is 6
        }
    });
    
    // Get sum of all values
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("values", listOf(1, 2, 3))), new ResultHandler<Number>() {
        @Override
        public void execute(Number result, Exception exception) {
            // result is 6
        }
    });

Note that, in Java 8 or later, lambda expressions can be used instead of anonymous classes to implement result handlers, reducing the invocation code to the following:

    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("a", 2), entry("b", 4)), (result, exception) -> {
        // result is 6
    });

    // Get sum of all values
    serviceProxy.invoke("GET", "/math/sum", mapOf(entry("values", listOf(1, 2, 3))), (result, exception) -> {
        // result is 6
    });

## Objective-C/Swift Client
The Objective-C/Swift client library enables iOS applications to consume HTTP-RPC services. It is delivered as a modular framework that includes the following types, discussed in more detail below:

* `WSWebServiceProxy` - invocation proxy for HTTP-RPC services
* `WSAuthentication` - interface for authenticating requests
* `WSBasicAuthentication` - authentication implementation supporting basic HTTP authentication

The framework for the Objective-C/Swift client can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases). It is also available via [CocoaPods](https://cocoapods.org/pods/HTTP-RPC). iOS 8 or later is required.

### WSWebServiceProxy Class
The `WSWebServiceProxy` class serves as an invocation proxy for HTTP-RPC services. Internally, it uses an instance of `NSURLSession` to issue HTTP requests. `POST` requests are encoded as "multipart/form-data". `NSJSONSerialization` is used to decode JSON response data, and `UIImage` is used to decode image content. All other content is returned as `NSData`.

Service proxies are initialized via the `initWithSession:serverURL:` method, which takes an `NSURLSession` instance and the URL of the server as arguments. Service operations are executed by calling the `invoke:path:arguments:resultHandler:` method:

    - (NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
        arguments:(NSDictionary<NSString *, id> *)arguments
        resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

This method takes the following arguments:

* `method` - the HTTP method to execute
* `path` - the resource path
* `arguments` - a dictionary containing the request arguments as key/value pairs
* `resultHandler` - a callback that will be invoked upon completion of the method

A convenience method is also provided for executing operations that don't take any arguments:

    - (NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
        resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

Arguments may be any of the following types:

* `NSNumber`
* `NSString`
* `NSURL`
* `NSArray`

`CFBooleanRef` is also supported for boolean arguments. URL arguments represent binary content and can only be used with `POST` requests. Array arguments may be used with any request type, but array elements must be a supported simple type.

The result handler callback is called upon completion of the operation. The callback takes two arguments: a result object and an error object. If the operation completes successfully, the first argument first argument will contain the result of the operation. If the operation fails, the second argument will be populated with an instance of `NSError` describing the error that occurred.

Both variants of the `invoke` method return an instance of `NSURLSessionDataTask` representing the invocation request. This allows an application to cancel a task, if necessary.

Although requests are typically processed on a background thread, result handlers are called on the same operation queue that initially invoked the service method. This is typically the application's main queue, which allows result handlers to update the application's user interface directly, rather than posting a separate update operation to the main queue.

### Authentication
Although it is possible to use the `URLSession:task:didReceiveChallenge:completionHandler:` method of the `NSURLSessionDataDelegate` protocol to authenticate service requests, this method requires an unnecessary round trip to the server if a user's credentials are already known up front, as is often the case.

HTTP-RPC provides an additional authentication mechanism that can be specified on a per-proxy basis. The `WSAuthentication` protocol defines a single method that is used to authenticate each request submitted by a proxy instance:

    - (void)authenticateRequest:(NSMutableURLRequest *)request;

Authentication providers are associated with a proxy instance via the `authentication` property of the `WSWebServiceProxy` class. For example, the following code associates an instance of `WSBasicAuthentication` with a service proxy:

    serviceProxy.authentication = WSBasicAuthentication(username: "username", password: "password")

The `WSBasicAuthentication` class is provided by the HTTP-RPC framework. Applications may provide custom implementations of the `WSAuthentication` protocol to support other authentication schemes.

### Examples
The following code snippet demonstrates how `WSWebServiceProxy` can be used to invoke the methods of the hypothetical math service. It first creates an instance of the `WSWebServiceProxy` class backed by a default URL session and a delegate queue supporting ten concurrent operations. It then invokes the `getSum(double, double)` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `getSum(List<Double>)` method, passing the values 1, 2, and 3 as arguments:

    // Configure session
    let configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
    configuration.requestCachePolicy = NSURLRequestCachePolicy.ReloadIgnoringLocalAndRemoteCacheData

    let delegateQueue = NSOperationQueue()
    delegateQueue.maxConcurrentOperationCount = 10

    let session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

    // Initialize service proxy and invoke methods
    let serverURL = NSURL(string: "https://localhost:8443")

    let serviceProxy = WSWebServiceProxy(session: session, serverURL: serverURL!)
    
    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", path: "/math/sum", arguments: ["a": 2, "b": 4]) {(result, error) in
        // result is 6
    }

    // Get sum of all values
    serviceProxy.invoke("GET", path: "/math/sum", arguments: ["values": [1, 2, 3]]) {(result, error) in
        // result is 6
    }

## JavaScript Client
The JavaScript HTTP-RPC client enables browser-based applications to consume HTTP-RPC services. It is delivered as JavaScript file that defines a single `WebServiceProxy` class, which is discussed in more detail below. 

The source code for the JavaScript client can be downloaded [here](https://github.com/gk-brown/HTTP-RPC/releases).

### WebServiceProxy Class
The `WebServiceProxy` class serves as an invocation proxy for HTTP-RPC services. Internally, it uses an instance of `XMLHttpRequest` to communicate with the server, and uses `JSON.parse()` to convert the response to an object. `POST` requests are encoded using the "application/x-www-form-urlencoded" MIME type.

Service proxies are initialized via the `WebServiceProxy` constructor. Service operations are executed by calling the `invoke()` method on the service proxy. Request arguments can be numbers, booleans, strings, or arrays of any simple type.

The `invoke()` method takes a result handler function as the final argument. This callback is invoked upon completion of the operation. The callback takes two arguments: a result object and an error object. If the remote method completes successfully, the first argument contains the value returned by the method. If the method call fails, the second argument will contain the HTTP status code corresponding to the error that occurred.

Both methods return the `XMLHttpRequest` instance used to execute the remote call. This allows an application to cancel a request, if necessary.

### Examples
The following code snippet demonstrates how `WebServiceProxy` can be used to invoke the methods of the hypothetical math service. It first creates an instance of the `WebServiceProxy` class, and then invokes the `getSum(double, double)` method of the service, passing a value of 2 for "a" and 4 for "b". Finally, it executes the `getSum(List<Double>)` method, passing the values 1, 2, and 3 as arguments:

    // Create service proxy
    var serviceProxy = new WebServiceProxy();

    // Get sum of "a" and "b"
    serviceProxy.invoke("GET", "/math/sum", {a:4, b:2}, function(result, error) {
        // result is 6
    });

    // Get sum of all values
    serviceProxy.invoke("GET", "/math/sum", {values:[1, 2, 3, 4]}, function(result, error) {
        // result is 6
    });

# Additional Information
For additional information and examples, see the [the wiki](https://github.com/gk-brown/HTTP-RPC/wiki).
