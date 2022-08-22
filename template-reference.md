# Templates
Templates are documents that describe an output format such as HTML. They allow the ultimate representation of a data structure (or "data dictionary") to be specified independently of the data itself, promoting a clear separation of responsibility. 

Template documents include "markers" that are replaced with values provided by the data dictionary when the template is processed:

* {{_variable_}} - injects a value from the data dictionary into the output
* {{?_section_}}...{{/_section_}} - defines a conditional section
* {{#_section_}}...{{/_section_}} - defines a repeating section
* {{^_section_}}...{{/_section_}} - defines an inverted section
* {{>_include_}} - imports content from another template
* {{!_comment_}} - provides non-rendered informational content

Each of these marker types is discussed in more detail below.

## Variables
Variable markers inject a named value from the data dictionary into the output. For example:

```html
<p>Count: {{count}}</p>
<p>Sum: {{sum}}</p>
<p>Average: {{average}}</p> 
```

Variable names represent keys into the data dictionary. When the template is processed, the markers are replaced with the corresponding values from the dictionary. When traversing sections, dictionary values are recursively inherited. If a value is not provided by the current dictionary or any of its parents, it is excluded from the generated output. 

Nested values can be referred to by path; e.g. "name/first". A literal slash or backslash character can be used in a variable or section name by escaping it with a leading backslash.

The reserved "~" and "." variable names represent key and value references, respectively, and are discussed in more detail below.

### Modifiers
Modifiers are used to transform a variable's representation before it is written to the output stream; for example, to apply an escape sequence.

Modifiers are specified as shown below. They are invoked in order from left to right. An optional argument value may be included to provide additional information to the modifier:

```
{{variable:modifier1:modifier2:modifier3=argument:...}}
```

All templates support the following set of standard modifiers:

* `format` - applies a [format string](https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax)
* `url` - applies URL encoding to a value
* `json` - applies JSON encoding to a value
* `csv` - applies CSV encoding to a value
* `xml` - applies XML encoding to a value
* `html` - applies HTML encoding to a value

For example, the following marker applies a format string to a value and then URL-encodes the result:

```
{{value:format=0x%04x:url}}
```

Applications may also define their own custom modifiers.

#### Locale-Specific Formatting
In addition to `printf()`-style formatting, the `format` modifier also supports the following arguments for locale-specific formatting of numbers and dates/times:

  * `currency` - applies a currency format
  * `percent` - applies a percentage format
  * `shortDate` - applies a short date format
  * `mediumDate` - applies a medium date format
  * `longDate` - applies a long date format
  * `fullDate` - applies a full date format
  * `isoDate` - applies an ISO date format
  * `shortTime` - applies a short time format
  * `mediumTime` - applies a medium time format
  * `longTime` - applies a long time format
  * `fullTime` - applies a full time format
  * `isoTime` - applies an ISO time format
  * `shortDateTime` - applies a short date/time format
  * `mediumDateTime` - applies a medium date/time format
  * `longDateTime` - applies a long date/time format
  * `fullDateTime` - applies a full date/time format
  * `isoDateTime` - applies an ISO date/time format

For example, this marker transforms a date value into a medium-length, localized date string:

```
{{date:format=mediumDate}}
```

Date/time values may be represented by one of the following:

* a numeric value representing epoch time in milliseconds
* an instance of `java.util.Date` 
* an instance of `java.util.time.TemporalAccessor`

## Conditional Sections
Conditional section markers define a section of content that is only rendered if the named value exists in the data dictionary. When the value exists, it is used as the data dictionary for the section.

For example, given the following data dictionary:

```json
{
  "name": {
    "first": "John",
    "last": "Smith"
  }
}
```

the content of the "name" section in this template would be included in the generated output, but the content of the "age" section would not. The section markers are enclosed in comments so they will be ignored by syntax-aware text editors, and will simply resolve to empty comment blocks when the template is processed:

```
<!-- {{?name}} -->
<p>Name: {{last}}, {{first}}</p>
<!-- {{/name}} -->

<!-- {{?age}} -->
<p>Age: {{.}}</p>
<!-- {{/age}} -->
```

## Repeating Sections
Repeating section markers define a section of content that is repeated once for every element in a sequence of values. The marker name must refer to an instance of either `java.lang.Iterable` or `java.util.Map` in the data dictionary. The elements of the sequence provide the data dictionaries for successive iterations through the section.

For example, a data dictionary that contains information about homes for sale might look like this:

```json
{
  "properties": [
    {
      "streetAddress": "27 Crescent St.",
      "listPrice": 925000,
      "numberOfBedrooms": 4,
      "numberOfBathrooms": 3
    },
    {
      "streetAddress": "390 North Elm St.",
      "listPrice": 7650000,
      "numberOfBedrooms": 3,
      "numberOfBathrooms": 1.5
    },
    ...
  ]
}
```

A template to transform these results into HTML is shown below:

```html
<table>
<!-- {{#properties}} -->
<tr>
    <td>{{streetAddress}}</td> 
    <td>{{listPrice:format=currency}}</td> 
    <td>{{numberOfBedrooms}}</td> 
    <td>{{numberOfBathrooms}}</td>
</tr>
<!-- {{/properties}} -->
</table>
```

### Separators
Section markers may specify an optional separator string that will be automatically injected between the section's elements. The separator text is enclosed in square brackets immediately following the section name. 

For example, the elements of the "names" section specified below will be separated by a comma in the generated output:

```
{{#names[,]}}
...
{{/names}}
```

### Key and Value References
In most cases, variable names are used to refer to properties of the `Map` instance representing the current data dictionary. However, when traversing the contents of a `Map` sequence, the reserved "~" variable can be used to refer to the key associated with the current element. 

Additionally, when traversing any type of sequence (whether `Iterable` or `Map`), if the current element is not a `Map` (for example, a `Number`, `String`, or `Iterable`), the reserved "." variable can be used to refer to the value of the element itself.

For example, the following data dictionary associates a set of number names with their corresponding numeric values:

```json
{
  "numbers": { 
    "one": 1,
    "two": 2,
    "three": 3
  }
}
```

This template could be used to generate a comma-separated list of name/value pairs from the data dictionary:

```
{{#numbers[,]}}{{~}}:{{.}}{{/numbers}}
``` 

## Inverted Sections
Inverted section markers define a section of content that is only rendered if the named value does not exist in the data dictionary. For example, given the following data dictionary:

```json
{
  "addresses": [
  ]
}
```

this template would produce the text "no addresses": 

```
{{^addresses}}no addresses{{/addresses}}
``` 

## Includes
Include markers import content defined by another template. They can be used to create reusable content modules, such as document headers or footers. 

For example, the following template, _hello.txt_, includes another document named _world.txt_: 

```
Hello, {{>world.txt}}!
```
    
When _hello.txt_ is processed, the include marker will be replaced with the contents of _world.txt_. For example, if _world.txt_ contains the text "World", the result of processing _hello.txt_ would be the following:

```
Hello, World!
```

Includes inherit their context from the parent document, so they can refer to elements in the parent's data dictionary. This allows includes to be parameterized. Self-referencing includes can also be used to facilitate recursion.

## Comments
Comment markers provide informational text about a template's content. They are not included in the final output. 

For example, when the following template is processed, only the content between the `<p>` tags will be included:

```
{{! Some placeholder text }}
<p>Lorem ipsum dolor sit amet.</p>
```
