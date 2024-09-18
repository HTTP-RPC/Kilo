/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TextDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

/**
 * Client-side invocation proxy for web services.
 */
public class WebServiceProxy {
    /**
     * Encoding options for POST requests.
     */
    public enum Encoding {
        /**
         * The "application/x-www-form-urlencoded" encoding.
         */
        APPLICATION_X_WWW_FORM_URLENCODED,

        /**
         * The "multipart/form-data" encoding.
         */
        MULTIPART_FORM_DATA
    }

    /**
     * Represents a request handler.
     */
    public interface RequestHandler {
        /**
         * Returns the content type produced by the handler.
         *
         * @return
         * The content type produced by the handler.
         */
        String getContentType();

        /**
         * Encodes a request to an output stream.
         *
         * @param outputStream
         * The output stream to write to.
         *
         * @throws IOException
         * If an exception occurs.
         */
        void encodeRequest(OutputStream outputStream) throws IOException;
    }

    /**
     * Represents an error handler.
     */
    public interface ErrorHandler {
        /**
         * Handles an error response.
         *
         * @param errorStream
         * The error stream.
         *
         * @param contentType
         * The content type, or {@code null} if the content type is not known.
         *
         * @param statusCode
         * The status code.
         *
         * @throws IOException
         * Representing the error that occurred, or if an exception occurs while
         * handling the error.
         */
        void handleResponse(InputStream errorStream, String contentType, int statusCode) throws IOException;
    }

    /**
     * Represents a response handler.
     */
    public interface ResponseHandler<T> {
        /**
         * Decodes a response from an input stream.
         *
         * @param inputStream
         * The input stream to read from.
         *
         * @param contentType
         * The content type, or {@code null} if the content type is not known.
         *
         * @return
         * The decoded value.
         *
         * @throws IOException
         * If an exception occurs.
         */
        T decodeResponse(InputStream inputStream, String contentType) throws IOException;
    }

    private static class TypedInvocationHandler implements InvocationHandler {
        URI baseURI;
        Map<String, Object> headers;

        TypedInvocationHandler(URI baseURI, Map<String, Object> headers) {
            this.baseURI = baseURI;
            this.headers = headers;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                try {
                    return method.invoke(this, arguments);
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    throw new RuntimeException(exception);
                }
            } else if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
            } else {
                var requestMethod = method.getAnnotation(RequestMethod.class);

                if (requestMethod == null) {
                    throw new UnsupportedOperationException("Request method is not defined.");
                }

                var exceptionTypes = method.getExceptionTypes();

                if (exceptionTypes.length != 1 || !exceptionTypes[0].isAssignableFrom(IOException.class)) {
                    throw new UnsupportedOperationException("Missing or invalid exception declaration.");
                }

                var argumentList = coalesce(map(arguments, Arrays::asList), listOf());

                var pathBuilder = new StringBuilder();
                var keyCount = 0;

                var resourcePath = method.getAnnotation(ResourcePath.class);

                if (resourcePath != null) {
                    var components = resourcePath.value().split("/");

                    for (var i = 0; i < components.length; i++) {
                        if (i > 0) {
                            pathBuilder.append("/");
                        }

                        var component = components[i];

                        if (component.isEmpty()) {
                            throw new UnsupportedOperationException("Invalid resource path.");
                        }

                        if (component.equals("?")) {
                            var parameterValue = getParameterValue(argumentList.get(keyCount));

                            if (parameterValue == null) {
                                throw new IllegalArgumentException("Path variable is required.");
                            }

                            component = URLEncoder.encode(parameterValue.toString(), StandardCharsets.UTF_8);

                            keyCount++;
                        }

                        pathBuilder.append(component);
                    }
                }

                var webServiceProxy = new WebServiceProxy(requestMethod.value(), baseURI.resolve(pathBuilder.toString()));

                webServiceProxy.setHeaders(headers);

                var empty = switch (webServiceProxy.method) {
                    case "POST", "PUT" -> {
                        var formData = method.getAnnotation(FormData.class);

                        if (formData == null) {
                            yield argumentList.size() == keyCount;
                        } else {
                            if (formData.multipart()) {
                                webServiceProxy.setEncoding(Encoding.MULTIPART_FORM_DATA);
                            } else {
                                webServiceProxy.setEncoding(Encoding.APPLICATION_X_WWW_FORM_URLENCODED);
                            }

                            yield true;
                        }
                    }
                    default -> true;
                };

                var parameters = method.getParameters();

                var n = parameters.length;

                if (!empty) {
                    n--;
                }

                var argumentMap = new LinkedHashMap<String, Object>();

                for (var i = keyCount; i < n; i++) {
                    var parameter = parameters[i];

                    var value = argumentList.get(i);

                    if (parameter.getAnnotation(Required.class) != null && value == null) {
                        throw new IllegalArgumentException("Required argument is not defined.");
                    }

                    var name = coalesce(map(parameter.getAnnotation(Name.class), Name::value), parameter.getName());

                    argumentMap.put(name, value);
                }

                webServiceProxy.setArguments(argumentMap);

                if (n < parameters.length) {
                    var body = argumentList.get(n);

                    if (body == null && parameters[n].getType() != Void.class) {
                        throw new IllegalArgumentException("Body is required.");
                    }

                    webServiceProxy.setBody(body);
                }

                return BeanAdapter.toGenericType(webServiceProxy.invoke(), method.getGenericReturnType());
            }
        }
    }

    private String method;
    private URI uri;

    private Encoding encoding = null;

    private Map<String, Object> headers = mapOf();
    private Map<String, Object> arguments = mapOf();

    private Object body = null;

    private RequestHandler requestHandler = null;
    private ErrorHandler errorHandler = null;

    private int connectTimeout = 15000;
    private int readTimeout = 60000;
    private int chunkSize = 4096;

    private String multipartBoundary = UUID.randomUUID().toString();

    private int statusCode = -1;

    private static final int EOF = -1;

    private static final ErrorHandler defaultErrorHandler = (errorStream, contentType, statusCode) -> {
        String message;
        if (errorStream != null && contentType != null && contentType.toLowerCase().startsWith("text/plain")) {
            var textDecoder = new TextDecoder();

            message = textDecoder.read(errorStream);
        } else {
            message = String.format("HTTP %d", statusCode);
        }

        throw new WebServiceException(message, statusCode);
    };

    /**
     * Constructs a new web service proxy.
     *
     * @param method
     * The HTTP method.
     *
     * @param uri
     * The resource URI.
     */
    public WebServiceProxy(String method, URI uri) {
        if (method == null || uri == null) {
            throw new IllegalArgumentException();
        }

        this.method = method.toUpperCase();
        this.uri = uri;
    }

    /**
     * Constructs a new web service proxy.
     *
     * @param method
     * The HTTP method.
     *
     * @param baseURI
     * The base URI.
     *
     * @param path
     * The path to the resource, relative to the base URL.
     *
     * @param arguments
     * Path format specifier arguments.
     */
    public WebServiceProxy(String method, URI baseURI, String path, Object... arguments) {
        this(method, baseURI.resolve(String.format(path, arguments)));
    }

    /**
     * Returns the encoding used for POST requests.
     *
     * @return
     * The encoding used for POST requests.
     */
    public Encoding getEncoding() {
        return encoding;
    }

    /**
     * Sets the encoding used for POST requests.
     *
     * @param encoding
     * The encoding used for POST requests.
     */
    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the header map.
     *
     * @return
     * The header map.
     */
    public Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * Sets the header map.
     *
     * @param headers
     * The header map.
     */
    public void setHeaders(Map<String, Object> headers) {
        if (headers == null) {
            throw new IllegalArgumentException();
        }

        this.headers = headers;
    }

    /**
     * Returns the argument map.
     *
     * @return
     * The argument map.
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Sets the argument map.
     *
     * @param arguments
     * The argument map.
     */
    public void setArguments(Map<String, Object> arguments) {
        if (arguments == null) {
            throw new IllegalArgumentException();
        }

        this.arguments = arguments;
    }

    /**
     * Returns the request body.
     *
     * @return
     * A value representing the body content, or {@code null} if no body has
     * been set.
     */
    public Object getBody() {
        return body;
    }

    /**
     * Sets the request body.
     *
     * @param body
     * A value representing the body content, or {@code null} for no body.
     */
    public void setBody(Object body) {
        this.body = body;
    }

    /**
     * Returns the request handler.
     *
     * @return
     * The request handler, or {@code null} if no request handler has been set.
     */
    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    /**
     * Sets the request handler.
     *
     * @param requestHandler
     * The request handler, or {@code null} for the default request handler.
     */
    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    /**
     * Returns the error handler.
     *
     * @return
     * The error handler, or {@code null} if no error handler has been set.
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler.
     *
     * @param errorHandler
     * The error handler, or {@code null} for the default error handler.
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Returns the connect timeout.
     *
     * @return
     * The connect timeout, in milliseconds.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connect timeout.
     *
     * @param connectTimeout
     * The connect timeout, in milliseconds, or 0 for no timeout.
     */
    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout < 0) {
            throw new IllegalArgumentException();
        }

        this.connectTimeout = connectTimeout;
    }

    /**
     * Returns the read timeout.
     *
     * @return
     * The read timeout, in milliseconds.
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout.
     *
     * @param readTimeout
     * The read timeout, in milliseconds, or 0 for no timeout.
     */
    public void setReadTimeout(int readTimeout) {
        if (readTimeout < 0) {
            throw new IllegalArgumentException();
        }

        this.readTimeout = readTimeout;
    }

    /**
     * Returns the chunk size.
     *
     * @return
     * The chunk size.
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Sets the chunk size.
     *
     * @param chunkSize
     * The chunk size, or 0 to disable chunked streaming.
     */
    public void setChunkSize(int chunkSize) {
        if (chunkSize < 0) {
            throw new IllegalArgumentException();
        }

        this.chunkSize = chunkSize;
    }

    /**
     * Invokes the service operation.
     *
     * @return
     * The result of the operation.
     *
     * @throws IOException
     * If an exception occurs while executing the operation.
     */
    public Object invoke() throws IOException {
        return invoke((inputStream, contentType) -> {
            var jsonDecoder = new JSONDecoder();

            return jsonDecoder.read(inputStream);
        });
    }

    /**
     * Invokes the service operation.
     *
     * @param <T>
     * The result type.
     *
     * @param responseHandler
     * The response handler.
     *
     * @return
     * The result of the operation.
     *
     * @throws IOException
     * If an exception occurs while executing the operation.
     */
    public <T> T invoke(ResponseHandler<T> responseHandler) throws IOException {
        if (responseHandler == null) {
            throw new IllegalArgumentException();
        }

        var uri = this.uri;

        RequestHandler requestHandler;
        if (method.equals("POST") && encoding != null) {
            if (body != null || this.requestHandler != null) {
                throw new IllegalStateException("Encoding already specified.");
            }

            requestHandler = new RequestHandler() {
                @Override
                public String getContentType() {
                    return switch (encoding) {
                        case APPLICATION_X_WWW_FORM_URLENCODED -> "application/x-www-form-urlencoded";
                        case MULTIPART_FORM_DATA -> String.format("multipart/form-data; boundary=%s", multipartBoundary);
                    };
                }

                @Override
                public void encodeRequest(OutputStream outputStream) throws IOException {
                    switch (encoding) {
                        case APPLICATION_X_WWW_FORM_URLENCODED -> encodeApplicationXWWWFormURLEncodedRequest(outputStream);
                        case MULTIPART_FORM_DATA -> encodeMultipartFormDataRequest(outputStream);
                    }
                }
            };
        } else {
            var query = encodeQuery();

            if (!query.isEmpty()) {
                try {
                    uri = new URI(String.format("%s?%s", uri, query));
                } catch (URISyntaxException exception) {
                    throw new MalformedURLException(exception.getMessage());
                }
            }

            if (body != null) {
                if (this.requestHandler != null) {
                    throw new IllegalStateException("Body already specified.");
                }

                requestHandler = new RequestHandler() {
                    @Override
                    public String getContentType() {
                        return "application/json";
                    }

                    @Override
                    public void encodeRequest(OutputStream outputStream) throws IOException {
                        var jsonEncoder = new JSONEncoder();

                        jsonEncoder.write(BeanAdapter.adapt(body), outputStream);
                    }
                };
            } else {
                requestHandler = this.requestHandler;
            }
        }

        // Open URL connection
        var connection = (HttpURLConnection)uri.toURL().openConnection();

        connection.setRequestMethod(method);

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        var locale = Locale.getDefault();

        connection.setRequestProperty("Accept-Language", String.format("%s-%s",
            locale.getLanguage().toLowerCase(),
            locale.getCountry().toLowerCase()));

        // Apply custom headers
        for (Map.Entry<String, ?> entry : headers.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (key == null || value == null) {
                continue;
            }

            connection.setRequestProperty(key, value.toString());
        }

        // Write request body
        if (requestHandler != null) {
            connection.setDoOutput(true);

            if (chunkSize > 0) {
                connection.setChunkedStreamingMode(chunkSize);
            }

            var contentType = requestHandler.getContentType();

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            connection.setRequestProperty("Content-Type", contentType);

            try (var outputStream = connection.getOutputStream()) {
                requestHandler.encodeRequest(outputStream);
            }
        }

        // Read response
        statusCode = connection.getResponseCode();

        var contentType = connection.getContentType();

        T result;
        if (statusCode / 100 == 2) {
            if (statusCode % 100 < 4) {
                try (var inputStream = connection.getInputStream()) {
                    result = responseHandler.decodeResponse(inputStream, contentType);
                }
            } else {
                result = null;
            }
        } else {
            var errorHandler = this.errorHandler;

            if (errorHandler == null) {
                errorHandler = defaultErrorHandler;
            }

            try (var errorStream = connection.getErrorStream()) {
                errorHandler.handleResponse(errorStream, contentType, statusCode);
            }

            return null;
        }

        return result;
    }

    private String encodeQuery() {
        var queryBuilder = new StringBuilder(256);

        var i = 0;

        for (Map.Entry<String, ?> entry : arguments.entrySet()) {
            var key = entry.getKey();

            if (key == null) {
                throw new IllegalStateException();
            }

            for (var value : getParameterValues(entry.getValue())) {
                if (value == null) {
                    continue;
                }

                if (i > 0) {
                    queryBuilder.append("&");
                }

                queryBuilder.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
                queryBuilder.append("=");
                queryBuilder.append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));

                i++;
            }
        }

        return queryBuilder.toString();
    }

    private void encodeApplicationXWWWFormURLEncodedRequest(OutputStream outputStream) throws IOException {
        var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        writer.append(encodeQuery());

        writer.flush();
    }

    private void encodeMultipartFormDataRequest(OutputStream outputStream) throws IOException {
        var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        for (Map.Entry<String, ?> entry : arguments.entrySet()) {
            var name = entry.getKey();

            if (name == null) {
                continue;
            }

            for (var value : getParameterValues(entry.getValue())) {
                if (value == null) {
                    continue;
                }

                writer.append(String.format("--%s\r\n", multipartBoundary));
                writer.append(String.format("Content-Disposition: form-data; name=\"%s\"", name));

                if (value instanceof URL url) {
                    var path = url.getPath();
                    var filename = path.substring(path.lastIndexOf('/') + 1);

                    writer.append(String.format("; filename=\"%s\"\r\n", filename));
                    writer.append("Content-Type: application/octet-stream\r\n\r\n");

                    writer.flush();

                    try (var inputStream = ((URL)value).openStream()) {
                        int b;
                        while ((b = inputStream.read()) != EOF) {
                            outputStream.write(b);
                        }
                    }
                } else {
                    writer.append("\r\n\r\n");
                    writer.append(value.toString());
                }

                writer.append("\r\n");
            }
        }

        writer.append(String.format("--%s--\r\n", multipartBoundary));

        writer.flush();
    }

    private static List<Object> getParameterValues(Object argument) {
        if (argument != null && argument.getClass().isArray()) {
            var length = Array.getLength(argument);

            var list = new ArrayList<>(length);

            for (var i = 0; i < length; i++) {
                list.add(getParameterValue(Array.get(argument, i)));
            }

            return list;
        } else if (argument instanceof Collection<?> collection) {
            return collection.stream().map(WebServiceProxy::getParameterValue).toList();
        } else {
            return listOf(getParameterValue(argument));
        }
    }

    private static Object getParameterValue(Object argument) {
        if (argument instanceof Date date) {
            return date.getTime();
        } else {
            return argument;
        }
    }

    /**
     * Returns the status code associated with the most recent invocation.
     *
     * @return
     * The status code associated with the most recent invocation.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Creates a typed proxy for web service invocation.
     *
     * @param <T>
     * The proxy type.
     *
     * @param type
     * The proxy type.
     *
     * @param baseURI
     * The base URI.
     *
     * @return
     * The typed service proxy.
     */
    public static <T> T of(Class<T> type, URI baseURI) {
        return of(type, baseURI, mapOf());
    }

    /**
     * Creates a typed proxy for web service invocation.
     *
     * @param <T>
     * The proxy type.
     *
     * @param type
     * The proxy type.
     *
     * @param baseURI
     * The base URI.
     *
     * @param headers
     * The header map.
     *
     * @return
     * The typed web service proxy.
     */
    public static <T> T of(Class<T> type, URI baseURI, Map<String, Object> headers) {
        if (type == null || baseURI == null || headers == null) {
            throw new IllegalArgumentException();
        }

        if (!type.isInterface()) {
            throw new IllegalArgumentException("Type is not an interface.");
        }

        var servicePath = type.getAnnotation(ServicePath.class);

        if (servicePath != null) {
            baseURI = baseURI.resolve(String.format("%s/", servicePath.value()));
        }

        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, new TypedInvocationHandler(baseURI, headers)));
    }
}
