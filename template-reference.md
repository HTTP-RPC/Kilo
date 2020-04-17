# Templates
Templates are documents that describe an output format such as HTML. They allow the ultimate representation of a data structure (or "data dictionary") to be specified independently of the data itself, promoting a clear separation of responsibility. 

Template documents include "markers" that are replaced with values provided by the data dictionary when the template is processed:

* {{_variable_}} - injects a variable from the data dictionary into the output
* {{#_section_}}...{{/_section_}} - defines a repeating section of content
* {{>_include_}} - imports content from another template
* {{!_comment_}} - provides informational text about a template's content

Each of these marker types is discussed in more detail below.

## Variable Markers
Variable markers inject a value from the data dictionary into the output. For example:

```html
<p>Count: {{count}}</p>
<p>Sum: {{sum}}</p>
<p>Average: {{average}}</p> 
```

Variable names represent keys into the data dictionary. When the template is processed, the markers are replaced with the corresponding values from the dictionary.

Nested values can be referred to using path notation; e.g. "name.first". Missing (i.e. `null`) values are replaced with the empty string in the generated output. 

Variable names beginning with "@" represent resource references, and are replaced with the corresponding values from the resource bundle when the template is processed. 

Variable names beginning with "$" represent context references, and are replaced with the corresponding values from the template context when the template is processed. 

### Modifiers
Modifiers are used to transform a variable's representation before it is written to the output stream; for example, to apply an escape sequence.

Modifiers are specified as shown below. They are invoked in order from left to right. An optional argument value may be included to provide additional information to the modifier:

```
{{variable:modifier1:modifier2:modifier3=argument:...}}
```

All templates support the following set of standard modifiers:

* `format` - applies a [format string](https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax)
* `^url` - applies URL encoding to a value
* `^json` - applies JSON encoding to a value
* `^csv` - applies CSV encoding to a value
* `^xml` - applies XML encoding to a value
* `^html` - applies HTML encoding to a value

For example, the following marker applies a format string to a value and then URL-encodes the result:

```
{{value:format=0x%04x:^url}}
```

Applications may also define their own custom modifiers.

#### Locale-Specific Formatting
In addition to `printf()`-style formatting, the `format` modifier also supports the following arguments for locale-specific formatting of numbers and dates:

  * `currency` - applies a currency format
  * `percent` - applies a percentage format
  * `shortDate` - applies a short date format
  * `mediumDate` - applies a medium date format
  * `longDate` - applies a long date format
  * `fullDate` - applies a full date format
  * `shortTime` - applies a short time format
  * `mediumTime` - applies a medium time format
  * `longTime` - applies a long time format
  * `fullTime` - applies a full time format
  * `shortDateTime` - applies a short date/time format
  * `mediumDateTime` - applies a medium date/time format
  * `longDateTime` - applies a long date/time format
  * `fullDateTime` - applies a full date/time format

For example, this marker transforms a date value into a localized medium-length date string:

```
{{date:format=mediumDate}}
```

Date/time values may be represented by one of the following:

* a `long` value representing epoch time in milliseconds
* an instance of `java.util.Date` 
* an instance of `java.util.time.TemporalAccessor`

## Section Markers
Section markers define a repeating section of content. The marker name must refer to an iterable value in the data dictionary (for example, an instance of `java.util.List`). 

Content between the markers is repeated once for each element in the list. The elements provide the data dictionaries for each successive iteration through the section. If the iterable value is missing (i.e. `null`) or empty, the section's content is excluded from the output.

For example, a data dictionary that contains information about homes for sale might look like this:

```json
{
  "properties": [
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
}
```

A template to transform these results into HTML is shown below. The section markers are enclosed in HTML comments so they will be ignored by syntax-aware text editors, and will simply resolve to empty comment blocks when the template is processed:

```html
<table>
<!-- {{#properties}} -->
<tr>
    <td>{{streetAddress:^html}}</td> 
    <td>{{listPrice:format=currency:^html}}</td> 
    <td>{{numberOfBedrooms}}</td> 
    <td>{{numberOfBathrooms}}</td>
</tr>
<!-- {{/properties}} -->
</table>
```

A dot character (".") can be used to represent the current element in a sequence. This can be useful when rendering scalar values. 

A dot can also be used to represent the sequence itself; for example, when a sequence is the root object:

```
{{#.}}
    ...
{{/.}}
```

### Separators
Section markers may specify an optional separator string that will be automatically injected between the section's elements. The separator text is enclosed in square brackets immediately following the section name. 

For example, the elements of the "addresses" section specified below will be separated by a comma in the generated output:

```
{{#addresses[,]}}
...
{{/addresses}}
```

## Includes
Include markers import content defined by another template. They can be used to create reusable content modules; for example, document headers and footers. 

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
