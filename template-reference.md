# Templates
Templates are documents that describe an output format such as HTML. They allow the ultimate representation of a data structure (or "data dictionary") to be specified independently of the data itself, promoting a clear separation of responsibility. 

Template documents include "markers" that are replaced with values from the data dictionary when the template is processed:

* {{_variable_}} - injects a [named value](#variables) into the output
* {{#_section_}}...{{/_section_}} - defines a [repeating section](#repeating-sections)
* {{?_section_}}...{{/_section_}} - defines a [conditional section](#conditional-sections)
* {{^_section_}}...{{/_section_}} - defines an [inverted section](#inverted-sections)
* {{$_resource_}} - injects a [localized value](#resources) into the output
* {{>_include_}} - [imports](#includes) content from another template
* {{!_comment_}} - provides non-rendered [informational](#comments) content

Each is discussed in more detail below.

## Variables
Variable markers inject a named value from the data dictionary into the output. For example:

```json
{
  "a": "A",
  "b": {
    "c": "C"
  },
  "d": [
    1, 
    2, 
    3
  ]
}
```

```html
<!-- A -->
<p>{{a}}</p>
```

Nested values can be referred to by path:

```html
<!-- C -->
<p>{{b/c}}</p>
```

Within a repeating or conditional section, the "." character can be used to refer to the current element:

```html
<!-- 1, 2, 3 -->
{{#d}}
<p>{{.}}</p>
{{/d}}
```

### Modifiers
Modifiers are used to transform a variable's representation before it is written to the output stream. They are specified as shown below and are invoked in declaration order. An optional argument may be included to provide additional information to a modifier:

```
{{variable:modifier1:modifier2:modifier3=argument:...}}
```

All templates support the `format` modifier, which can be used to apply a [format string](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Formatter.html#syntax) or one of the following named formats:

  * `currency`
  * `percent`
  * `shortDate`
  * `mediumDate`
  * `longDate`
  * `fullDate`
  * `isoDate`
  * `shortTime`
  * `mediumTime`
  * `longTime`
  * `fullTime`
  * `isoTime`
  * `shortDateTime`
  * `mediumDateTime`
  * `longDateTime`
  * `fullDateTime`
  * `isoDateTime`

For example:

```
{{date:format=shortDate}}
```

Applications may also define their own custom modifiers.

## Repeating Sections
Repeating section markers define a section of content that is repeated once for every element in a sequence of values. For example:

```json
{
  "a": "A",
  "b": [
    {
      "c": 1
    },
    {
      "c": 2
    },
    {
      "c": 3
    }
  ]
}
```

```html
<!-- 1, 2, 3 -->
{{#b}}
<p>{{c}}</p>
{{/b}}
```

Values are inherited from the parent context:

```html
<!-- A1, A2, A3 -->
{{#b}}
<p>{{a}}{{c}}</p>
{{/b}}
```

## Conditional Sections
Conditional section markers define a section of content that is only rendered if the named value exists in the data dictionary. Additionally, for iterable, map, and string content, the value must not be empty. For numeric data, the value must not be 0. For booleans, the value must be `true`.

For example:

```json
{
  "a": "A",
  "b": {
    "c": 1,
    "d": 2,
    "e": 3
  }
}
```

```html
<!-- 1, 2, 3 -->
{{?b}}
<p>{{c}}, {{d}}, {{e}}</p>
{{/b}}
```

As with repeating sections, parent values are inherited:

```html
<!-- A1, A2, A3 -->
{{?b}}
<p>{{a}}{{c}}, {{a}}{{d}}, {{a}}{{e}}</p>
{{/b}}
```

## Inverted Sections
Inverted section markers define a section of content that is only rendered if the named value does not exist in the data dictionary. Additionally, for iterable, map, and string content, the value must be empty. For numeric data, the value must be 0. For booleans, the value must be `false`.

For example:

```json
{
  "a": [
  ]
}
```

```html
<!-- not found -->
{{^a}}
<p>not found</p>
{{/a}}
```

## Resources
Resource markers inject a localized value into the output. For example:

```json
{
  "a": 123
}
```

```properties
a = Å
```

```html
<!-- 123 Å -->
{{a}} {{$a}}
```

## Includes
Include markers import content defined by another template. For example:

```json
{
  "a": 123
}
```

```html
<!DOCTYPE html>
<html>
<body>
{{>include.html}}
</body>
</html>
```

```html
<!-- 123 -->
<p>{{a}}</p>
```

Self-referencing includes are supported and can be used to facilitate recursion.

## Comments
Comment markers provide informational text about a template's content. They are not included in the final output:

```json
{
  "a": 123
}
```

```html
<!-- 123 -->
<p>{{a}} {{!456}}</p>
```
