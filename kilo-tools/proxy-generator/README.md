# Kilo Proxy Generator
Generates web service proxy types.

## Usage
```
proxy-generator <base-uri> <package-name> <service-path:proxy-type-name>...
```

The first argument is the fully-qualified base URL of the service (i.e. the web application's context path). The second argument specifies the package name for the generated types. The remaining arguments are key/value pairs representing the service path and proxy type name, respectively.  

Output is relative to the current directory.
