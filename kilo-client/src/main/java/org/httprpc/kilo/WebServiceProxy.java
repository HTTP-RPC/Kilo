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
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

/**
 * Web service proxy class.
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
     * Success status values.
     */
    public enum Status {
        /**
         * Indicates that the request was successful.
         */
        OK(200),

        /**
         * Indicates that the request resulted in the creation of a new resource.
         */
        CREATED(201),

        /**
         * Indicates that the request returned no content.
         */
        NO_CONTENT(204);

        private final int code;

        Status(int code) {
            this.code = code;
        }
    }

    /**
     * Interface representing a request handler.
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
     * Interface representing a response handler.
     */
    public interface ResponseHandler<T> {
        /**
         * Decodes a response from an input stream.
         *
         * @param inputStream
         * The input stream to read from.
         *
         * @param contentType
         * The content type, or <code>null</code> if the content type is not known.
         *
         * @throws IOException
         * If an exception occurs.
         *
         * @return
         * The decoded value.
         */
        T decodeResponse(InputStream inputStream, String contentType) throws IOException;
    }

    /**
     * Interface representing an error handler.
     */
    public interface ErrorHandler {
        /**
         * Handles an error response.
         *
         * @param errorStream
         * The error stream.
         *
         * @param contentType
         * The content type, or <code>null</code> if the content type is not known.
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

    private class MonitoredInputStream extends InputStream {
        InputStream inputStream;

        MonitoredInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            var b = inputStream.read();

            if (monitorStream != null && b != -1) {
                monitorStream.write(b);
            }

            return b;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }

    private class MonitoredOutputStream extends OutputStream {
        OutputStream outputStream;

        MonitoredOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);

            if (monitorStream != null) {
                monitorStream.write(b);
            }
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
        }
    }

    private String method;
    private URL url;

    private Encoding encoding = Encoding.APPLICATION_X_WWW_FORM_URLENCODED;

    private Map<String, Object> headers = emptyMap();
    private Map<String, Object> arguments = emptyMap();

    private Object body;

    private RequestHandler requestHandler = null;
    private ErrorHandler errorHandler = null;

    private int connectTimeout = 0;
    private int readTimeout = 0;

    private Status expectedStatus = null;

    private PrintStream monitorStream = null;

    private String multipartBoundary = UUID.randomUUID().toString();

    private static final int EOF = -1;

    private static final ErrorHandler defaultErrorHandler = (errorStream, contentType, statusCode) -> {
        String message;
        if (contentType != null && contentType.startsWith("text/")) {
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
     * @param url
     * The resource URL.
     */
    public WebServiceProxy(String method, URL url) {
        if (method == null) {
            throw new IllegalArgumentException();
        }

        if (url == null) {
            throw new IllegalArgumentException();
        }

        this.method = method.toUpperCase();
        this.url = url;
    }

    /**
     * Returns the HTTP method.
     *
     * @return
     * The HTTP method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the resource URL.
     *
     * @return
     * The resource URL.
     */
    public URL getURL() {
        return url;
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
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setEncoding(Encoding encoding) {
        if (encoding == null) {
            throw new IllegalArgumentException();
        }

        this.encoding = encoding;

        return this;
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
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setHeaders(Map<String, Object> headers) {
        if (headers == null) {
            throw new IllegalArgumentException();
        }

        this.headers = headers;

        return this;
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
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setArguments(Map<String, Object> arguments) {
        if (arguments == null) {
            throw new IllegalArgumentException();
        }

        this.arguments = arguments;

        return this;
    }

    /**
     * Returns the request body.
     *
     * @return
     * A value representing the body content, or <code>null</code> if no body has been set.
     */
    public Object getBody() {
        return body;
    }

    /**
     * Sets the request body.
     *
     * @param body
     * A value representing the body content, or <code>null</code> for no body.
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setBody(Object body) {
        this.body = body;

        return this;
    }

    /**
     * Returns the request handler.
     *
     * @return
     * The request handler, or <code>null</code> if no request handler has been set.
     */
    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

    /**
     * Sets the request handler.
     *
     * @param requestHandler
     * The request handler, or <code>null</code> for the default request handler.
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;

        return this;
    }

    /**
     * Returns the error handler.
     *
     * @return
     * The error handler, or <code>null</code> if no error handler has been set.
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler.
     *
     * @param errorHandler
     * The error handler, or <code>null</code> for the default error handler.
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

        return this;
    }

    /**
     * Returns the connect timeout.
     *
     * @return
     * The connect timeout.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connect timeout.
     *
     * @param connectTimeout
     * The connect timeout.
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;

        return this;
    }

    /**
     * Returns the read timeout.
     *
     * @return
     * The read timeout.
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout.
     *
     * @param readTimeout
     * The read timeout.
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;

        return this;
    }

    /**
     * Returns the expected status.
     *
     * @return
     * The expected status.
     */
    public Status getExpectedStatus() {
        return expectedStatus;
    }

    /**
     * Sets the expected status.
     *
     * @param expectedStatus
     * The expected status.
     *
     * @return
     * The web service proxy.
     */
    public WebServiceProxy setExpectedStatus(Status expectedStatus) {
        this.expectedStatus = expectedStatus;

        return this;
    }

    /**
     * Returns the monitor stream.
     *
     * @return
     * The monitor stream, or <code>null</code> if no monitor stream is set.
     */
    public PrintStream getMonitorStream() {
        return monitorStream;
    }

    /**
     * Sets the monitor stream.
     *
     * @param monitorStream
     * The monitor stream, or <code>null</code> for no monitor.
     */
    public WebServiceProxy setMonitorStream(PrintStream monitorStream) {
        this.monitorStream = monitorStream;

        return this;
    }

    /**
     * Invokes the service operation.
     *
     * @param <T>
     * The result type.
     *
     * @return
     * The result of the operation.
     *
     * @throws IOException
     * If an exception occurs while executing the operation.
     */
    public <T> T invoke() throws IOException {
        return invoke(Object.class);
    }

    /**
     * Invokes the service operation.
     *
     * @param <T>
     * The result type.
     *
     * @param type
     * The result type.
     *
     * @return
     * The result of the operation.
     *
     * @throws IOException
     * If an exception occurs while executing the operation.
     */
    public <T> T invoke(Type type) throws IOException {
        return invoke((inputStream, contentType) -> {
            var jsonDecoder = new JSONDecoder();

            return BeanAdapter.coerce(jsonDecoder.read(inputStream), type);
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
    public <T> T invoke(ResponseHandler<? extends T> responseHandler) throws IOException {
        if (responseHandler == null) {
            throw new IllegalArgumentException();
        }

        URL url;
        RequestHandler requestHandler;
        if (method.equals("POST") && body == null && this.requestHandler == null) {
            url = this.url;

            requestHandler = new RequestHandler() {
                @Override
                public String getContentType() {
                    String contentType;
                    switch (encoding) {
                        case APPLICATION_X_WWW_FORM_URLENCODED: {
                            contentType = "application/x-www-form-urlencoded";
                            break;
                        }

                        case MULTIPART_FORM_DATA: {
                            contentType = String.format("multipart/form-data; boundary=%s", multipartBoundary);
                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }

                    return contentType;
                }

                @Override
                public void encodeRequest(OutputStream outputStream) throws IOException {
                    switch (encoding) {
                        case APPLICATION_X_WWW_FORM_URLENCODED: {
                            encodeApplicationXWWWFormURLEncodedRequest(outputStream);
                            break;
                        }

                        case MULTIPART_FORM_DATA: {
                            encodeMultipartFormDataRequest(outputStream);
                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }
                }
            };
        } else {
            var query = encodeQuery();

            if (query.length() == 0) {
                url = this.url;
            } else {
                url = new URL(this.url.getProtocol(), this.url.getHost(), this.url.getPort(), this.url.getFile() + "?" + query);
            }

            if (body != null) {
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

        if (monitorStream != null) {
            monitorStream.println(String.format("%s %s", method, url));
        }

        // Open URL connection
        var connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod(method);

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        // Set standard headers
        connection.setRequestProperty("Accept", "*/*");

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

            var contentType = requestHandler.getContentType();

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            connection.setRequestProperty("Content-Type", contentType);

            try (OutputStream outputStream = new MonitoredOutputStream(connection.getOutputStream())) {
                requestHandler.encodeRequest(outputStream);
            } finally {
                if (monitorStream != null) {
                    monitorStream.println();
                    monitorStream.flush();
                }
            }
        }

        // Read response
        var statusCode = connection.getResponseCode();

        if (monitorStream != null) {
            monitorStream.println(String.format("HTTP %d", statusCode));
        }

        var contentType = connection.getContentType();

        T result;
        if (statusCode / 100 == 2) {
            if (expectedStatus != null && expectedStatus.code != statusCode) {
                throw new WebServiceException("Unexpected status.", statusCode);
            }

            if (statusCode % 100 < 4) {
                try (InputStream inputStream = new MonitoredInputStream(connection.getInputStream())) {
                    result = responseHandler.decodeResponse(inputStream, contentType);
                } finally {
                    if (monitorStream != null) {
                        monitorStream.println();
                        monitorStream.flush();
                    }
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
                errorHandler.handleResponse((errorStream == null) ? null : new MonitoredInputStream(errorStream), contentType, statusCode);
            } finally {
                if (monitorStream != null) {
                    monitorStream.println();
                    monitorStream.flush();
                }
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
                continue;
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

                if (value instanceof URL) {
                    var path = ((URL)value).getPath();
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

    @SuppressWarnings("unchecked")
    private static Iterable<Object> getParameterValues(Object argument) {
        Iterable<Object> values;
        if (argument instanceof Iterable<?>) {
            values = (Iterable<Object>)getParameterValue(argument);
        } else {
            values = singletonList(getParameterValue(argument));
        }

        return values;
    }

    private static Object getParameterValue(Object argument) {
        if (argument instanceof Date) {
            return ((Date)argument).getTime();
        } else {
            return argument;
        }
    }

    /**
     * Creates a web service proxy representing a GET request.
     *
     * @param url
     * The resource URL.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy get(URL url) {
        return new WebServiceProxy("GET", url);
    }

    /**
     * Creates a web service proxy representing a GET request.
     *
     * @param baseURL
     * The base URL.
     *
     * @param path
     * The path to the resource, relative to the base URL.
     *
     * @param args
     * Path format specifier arguments.
     *
     * @throws MalformedURLException
     * If a URL cannot be constructed from the base URL and path.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy get(URL baseURL, String path, Object... args) throws MalformedURLException {
        return get(new URL(baseURL, String.format(path, args)));
    }

    /**
     * Creates a web service proxy representing a POST request.
     *
     * @param url
     * The resource URL.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy post(URL url) {
        return new WebServiceProxy("POST", url);
    }

    /**
     * Creates a web service proxy representing a POST request.
     *
     * @param baseURL
     * The base URL.
     *
     * @param path
     * The path to the resource, relative to the base URL.
     *
     * @param args
     * Path format specifier arguments.
     *
     * @throws MalformedURLException
     * If a URL cannot be constructed from the base URL and path.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy post(URL baseURL, String path, Object... args) throws MalformedURLException {
        return post(new URL(baseURL, String.format(path, args)));
    }

    /**
     * Creates a web service proxy representing a PUT request.
     *
     * @param url
     * The resource URL.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy put(URL url) {
        return new WebServiceProxy("PUT", url);
    }

    /**
     * Creates a web service proxy representing a PUT request.
     *
     * @param baseURL
     * The base URL.
     *
     * @param path
     * The path to the resource, relative to the base URL.
     *
     * @param args
     * Path format specifier arguments.
     *
     * @throws MalformedURLException
     * If a URL cannot be constructed from the base URL and path.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy put(URL baseURL, String path, Object... args) throws MalformedURLException {
        return put(new URL(baseURL, String.format(path, args)));
    }

    /**
     * Creates a web service proxy representing a DELETE request.
     *
     * @param url
     * The resource URL.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy delete(URL url) {
        return new WebServiceProxy("DELETE", url);
    }

    /**
     * Creates a web service proxy representing a DELETE request.
     *
     * @param baseURL
     * The base URL.
     *
     * @param path
     * The path to the resource, relative to the base URL.
     *
     * @param args
     * Path format specifier arguments.
     *
     * @throws MalformedURLException
     * If a URL cannot be constructed from the base URL and path.
     *
     * @return
     * The new web service proxy.
     */
    public static WebServiceProxy delete(URL baseURL, String path, Object... args) throws MalformedURLException {
        return delete(new URL(baseURL, String.format(path, args)));
    }
}
