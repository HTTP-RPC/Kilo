# Kilo Proxy Generator
Generates web service proxy types.

## Usage
```
proxy-generator <package-name> <base-uri> <service-path>...
```

The first argument specifies the package name for the generated types. The second argument is the fully-qualified base URL of the service. The remaining arguments represent the paths to the services for which proxy types will be generated.

Output is relative to the current directory.
