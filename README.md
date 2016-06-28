HTTP-RPC is a framework for simplifying development of REST-based applications. It allows developers to publish and interact with HTTP-based web services using a convenient, RPC-like interface while preserving fundamental REST concepts such as statelessness and uniform resource access.

The project currently includes support for implementing REST services in Java and consuming services in Java, Objective-C/Swift, or JavaScript. The server library provides a lightweight alternative to other, larger Java-based REST frameworks, and the consistent cross-platform client API makes it easy to interact with services regardless of target device or operating system. 

# Overview
HTTP-RPC services are accessed by applying an HTTP verb such as GET or POST to a target resource. The target is specified by a path representing the name of the resource, and is generally expressed as a noun such as _/calendar_ or _/contacts_.

Arguments are passed either via the query string or in the request body, like an HTML form. Results are typically returned as JSON, although operations that do not return a value are also supported.

## GET
The `GET` method is used to retrive information from the server. For example, the following request might be used to obtain data about a calendar event:

    GET /calendar?eventID=101

This request might retrieve the sum of two numbers, whose values are specified by the `a` and `b` arguments:

    GET /sum?a=2&b=4

Alternatively, the values could be specified as a list rather than as two fixed variables:

    GET /sum?values=1&values=2&values=3

## POST
The `POST` method is typically used to add new information to the server. For example, the following request might be used to create a new calendar event:

    POST /calendar

As with HTML forms, if the `POST` arguments contain only text values, they can be encoded using the "application/x-www-form-urlencoded" MIME type:

    title=Planning+Meeting&start=2016-06-28T14:00&end=2016-06-28T15:00

If the arguments contain binary data such as a JPEG or PNG image, the "multipart/form-data" encoding can be used.

## PUT
The `PUT` method updates existing information on the server. For example, the following request might be used to modify the end date of a calendar event:

    PUT /calendar?eventID=102&end=2016-06-28T15:30

## DELETE
The `DELETE` method removes information from the server. For example, this request might be used to delete a calendar event:

    DELETE /calendar?eventID=102

## Response Codes
Although the HTTP specification defines a large number of possible response codes, only a few are applicable to HTTP-RPC services:

* _200 OK_ - The request succeeded, and the response contains a JSON value representing the result
* _204 No Content_ - The request succeeded, but did not produce a result
* _404 Not Found_ - The requested resource does not exist
* _405 Method Not Allowed_ - The requested resource exists, but does not support the requested HTTP method
* _500 Internal Server Error_ - An error occurred while executing the method

# Implementations
TODO

## Java Server
TODO

## Java Client
TODO

## Objective-C/Swift Client
TODO

## JavaScript Client
TODO

