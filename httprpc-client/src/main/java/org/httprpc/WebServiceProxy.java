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

package org.httprpc;

import org.httprpc.beans.BeanAdapter;
import org.httprpc.io.JSONDecoder;
import org.httprpc.io.JSONEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        APPLICATION_X_WWW_FORM_URLENCODED,
        MULTIPART_FORM_DATA
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
        default String getContentType() {
            return "application/octet-stream";
        }

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
         * @param headers
         * The response headers.
         *
         * @throws IOException
         * If an exception occurs.
         *
         * @return
         * The decoded value.
         */
        T decodeResponse(InputStream inputStream, String contentType, Map<String, String> headers) throws IOException;
    }

    // Typed invocation handler
    private static class TypedInvocationHandler implements InvocationHandler {
        URL baseURL;
        Class<?> type;
        Function<ResourcePath, Map<String, ?>> keyMapFactory;
        BiFunction<String, URL, WebServiceProxy> webServiceProxyFactory;

        TypedInvocationHandler(URL baseURL, Class<?> type, Function<ResourcePath, Map<String, ?>> keyMapFactory, BiFunction<String, URL, WebServiceProxy> webServiceProxyFactory) {
            this.baseURL = baseURL;
            this.type = type;
            this.keyMapFactory = keyMapFactory;
            this.webServiceProxyFactory = webServiceProxyFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            Class<?> declaringClass = method.getDeclaringClass();

            if (declaringClass == Object.class) {
                return method.invoke(this, arguments);
            } else {
                RequestMethod requestMethod = method.getAnnotation(RequestMethod.class);

                if (requestMethod == null) {
                    throw new UnsupportedOperationException();
                }

                ResourcePath resourcePath = method.getAnnotation(ResourcePath.class);

                URL url;
                if (resourcePath != null) {
                    Map<String, ?> keys = keyMapFactory.apply(resourcePath);

                    String[] components = resourcePath.value().split("/");

                    for (int i = 0; i < components.length; i++) {
                        String component = components[i];

                        if (component.length() == 0) {
                            continue;
                        }

                        if (component.startsWith(ResourcePath.PATH_VARIABLE_PREFIX)) {
                            int k = ResourcePath.PATH_VARIABLE_PREFIX.length();

                            if (component.length() == k || component.charAt(k++) != ':') {
                                throw new IllegalStateException("Invalid path variable.");
                            }

                            if (keys == null) {
                                throw new IllegalStateException("No keys provided.");
                            }

                            Object value = getParameterValue(keys.get(component.substring(k)));

                            if (value != null) {
                                components[i] = URLEncoder.encode(value.toString(), UTF_8);
                            } else {
                                components[i] = "";
                            }
                        }
                    }

                    url = new URL(baseURL, String.join("/", components));
                } else {
                    url = baseURL;
                }

                WebServiceProxy webServiceProxy = webServiceProxyFactory.apply(requestMethod.value(), url);

                Parameter[] parameters = method.getParameters();

                Map<String, Object> argumentMap = new LinkedHashMap<>();

                for (int i = 0; i < parameters.length; i++) {
                    argumentMap.put(parameters[i].getName(), arguments[i]);
                }

                webServiceProxy.setArguments(argumentMap);

                return BeanAdapter.adapt(webServiceProxy.invoke(), method.getGenericReturnType());
            }
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Proxy) {
                object = Proxy.getInvocationHandler(object);
            }

            if (!(object instanceof TypedInvocationHandler)) {
                return false;
            }

            return (type == ((TypedInvocationHandler)object).type);
        }

        @Override
        public String toString() {
            return type.toString();
        }
    }

    private String method;
    private URL url;

    private Encoding encoding = Encoding.APPLICATION_X_WWW_FORM_URLENCODED;

    private Map<String, ?> headers = emptyMap();
    private Map<String, ?> arguments = emptyMap();

    private Object content;

    private RequestHandler requestHandler = null;

    private int connectTimeout = 0;
    private int readTimeout = 0;

    private String multipartBoundary = UUID.randomUUID().toString();

    private static final String UTF_8 = "UTF-8";

    private static final int EOF = -1;

    /**
     * Constructs a new web service proxy.
     *
     * @param method
     * The service method.
     *
     * @param url
     * The service URL.
     */
    public WebServiceProxy(String method, URL url) {
        if (method == null) {
            throw new IllegalArgumentException();
        }

        if (url == null) {
            throw new IllegalArgumentException();
        }

        this.method = method;
        this.url = url;
    }

    /**
     * Returns the service method.
     *
     * @return
     * The service method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the service URL.
     *
     * @return
     * The service URL.
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
     */
    public void setEncoding(Encoding encoding) {
        if (encoding == null) {
            throw new IllegalArgumentException();
        }

        this.encoding = encoding;
    }

    /**
     * Returns the header map.
     *
     * @return
     * The header map.
     */
    public Map<String, ?> getHeaders() {
        return headers;
    }

    /**
     * Sets the header map.
     *
     * @param headers
     * The header map.
     */
    public void setHeaders(Map<String, ?> headers) {
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
    public Map<String, ?> getArguments() {
        return arguments;
    }

    /**
     * Sets the argument map.
     *
     * @param arguments
     * The argument map.
     */
    public void setArguments(Map<String, ?> arguments) {
        if (arguments == null) {
            throw new IllegalArgumentException();
        }

        this.arguments = arguments;
    }

    /**
     * Returns the request content.
     *
     * @return
     * A value representing the request content, or <code>null</code> for no content.
     */
    public Object getContent() {
        return content;
    }

    /**
     * Returns the request content.
     *
     * @param content
     * A value representing the request content, or <code>null</code> if no content has been set.
     */
    public void setContent(Object content) {
        this.content = content;
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
     * The request handler, or <code>null</code> for no request handler.
     */
    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
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
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
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
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Invokes the service method.
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
        return invoke((inputStream, contentType, headers) -> {
            JSONDecoder jsonDecoder = new JSONDecoder();

            return jsonDecoder.read(inputStream);
        });
    }

    /**
     * Invokes the service method.
     *
     * @param <T>
     * The result type.
     *
     * @param responseHandler
     * The response handler, or <code>null</code> for no response handler.
     *
     * @return
     * The result of the operation.
     *
     * @throws IOException
     * If an exception occurs while executing the operation.
     */
    public <T> T invoke(ResponseHandler<T> responseHandler) throws IOException {
        URL url;
        RequestHandler requestHandler;
        if (content != null && this.requestHandler == null) {
            url = this.url;

            requestHandler = new RequestHandler() {
                @Override
                public String getContentType() {
                    return "application/json";
                }

                @Override
                public void encodeRequest(OutputStream outputStream) throws IOException {
                    JSONEncoder jsonEncoder = new JSONEncoder();

                    jsonEncoder.write(content, outputStream);
                }
            };
        } else if (method.equalsIgnoreCase("POST") && this.requestHandler == null) {
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
            String query = encodeQuery();

            if (query.length() == 0) {
                url = this.url;
            } else {
                url = new URL(this.url.getProtocol(), this.url.getHost(), this.url.getPort(), this.url.getFile() + "?" + query);
            }

            requestHandler = this.requestHandler;
        }

        // Open URL connection
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setRequestMethod(method);

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        // Set standard headers
        connection.setRequestProperty("Accept", "*/*");

        Locale locale = Locale.getDefault();

        connection.setRequestProperty("Accept-Language", String.format("%s-%s",
            locale.getLanguage().toLowerCase(),
            locale.getCountry().toLowerCase()));

        // Apply custom headers
        for (Map.Entry<String, ?> entry : headers.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key == null || value == null) {
                continue;
            }

            connection.setRequestProperty(key, value.toString());
        }

        // Write request body
        if (requestHandler != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", requestHandler.getContentType());

            try (OutputStream outputStream = connection.getOutputStream()) {
                requestHandler.encodeRequest(outputStream);
            }
        }

        // Read response
        int responseCode = connection.getResponseCode();

        String contentType = connection.getContentType();

        T result;
        if (responseCode / 100 == 2) {
            if (responseCode % 100 < 4 && responseHandler != null) {
                Map<String, String> headers = new HashMap<>();

                for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
                    String key = entry.getKey();

                    if (key != null) {
                        List<String> values = entry.getValue();

                        if (!values.isEmpty()) {
                            headers.put(entry.getKey(), values.get(0));
                        }
                    }
                }

                try (InputStream inputStream = connection.getInputStream()) {
                    result = responseHandler.decodeResponse(inputStream, contentType, headers);
                }
            } else {
                result = null;
            }
        } else {
            String message;
            if (contentType != null && contentType.startsWith("text/plain")) {
                StringBuilder messageBuilder = new StringBuilder(1024);

                try (InputStream inputStream = connection.getErrorStream();
                    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    int c;
                    while ((c = reader.read()) != EOF) {
                        messageBuilder.append((char)c);
                    }
                }

                message = messageBuilder.toString();
            } else {
                String responseMessage = connection.getResponseMessage();

                message = (responseMessage == null || responseMessage.isEmpty()) ? String.valueOf(responseCode) : responseMessage;
            }

            throw new WebServiceException(message, responseCode);
        }

        return result;
    }

    private String encodeQuery() throws UnsupportedEncodingException {
        StringBuilder queryBuilder = new StringBuilder(256);

        int i = 0;

        for (Map.Entry<String, ?> entry : arguments.entrySet()) {
            String key = entry.getKey();

            if (key == null) {
                continue;
            }

            for (Object value : getParameterValues(entry.getValue())) {
                if (value == null) {
                    continue;
                }

                if (i > 0) {
                    queryBuilder.append("&");
                }

                queryBuilder.append(URLEncoder.encode(key, UTF_8));
                queryBuilder.append("=");
                queryBuilder.append(URLEncoder.encode(value.toString(), UTF_8));

                i++;
            }
        }

        return queryBuilder.toString();
    }

    private void encodeApplicationXWWWFormURLEncodedRequest(OutputStream outputStream) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        writer.append(encodeQuery());

        writer.flush();
    }

    private void encodeMultipartFormDataRequest(OutputStream outputStream) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        for (Map.Entry<String, ?> entry : arguments.entrySet()) {
            String name = entry.getKey();

            if (name == null) {
                continue;
            }

            for (Object value : getParameterValues(entry.getValue())) {
                if (value == null) {
                    continue;
                }

                writer.append(String.format("--%s\r\n", multipartBoundary));
                writer.append(String.format("Content-Disposition: form-data; name=\"%s\"", name));

                if (value instanceof URL) {
                    String path = ((URL)value).getPath();
                    String filename = path.substring(path.lastIndexOf('/') + 1);

                    writer.append(String.format("; filename=\"%s\"\r\n", filename));
                    writer.append("Content-Type: application/octet-stream\r\n\r\n");

                    writer.flush();

                    try (InputStream inputStream = ((URL)value).openStream()) {
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

    private static Iterable<?> getParameterValues(Object argument) {
        Iterable<?> values;
        if (argument instanceof Iterable<?>) {
            values = (Iterable<?>)getParameterValue(argument);
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
     * Creates a type-safe web service proxy adapter.
     *
     * @param <T>
     * The target type.
     *
     * @param baseURL
     * The base URL of the web service.
     *
     * @param type
     * The type of the adapter.
     *
     * @return
     * An instance of the given type that adapts the target service.
     */
    public static <T> T adapt(URL baseURL, Class<T> type) {
        return adapt(baseURL, type, (resourcePath) -> null);
    }

    /**
     * Creates a type-safe web service proxy adapter.
     *
     * @param <T>
     * The target type.
     *
     * @param baseURL
     * The base URL of the web service.
     *
     * @param type
     * The type of the adapter.
     *
     * @param keyMapFactory
     * A function that produces a key map, or <code>null</code> if no keys are required.
     *
     * @return
     * An instance of the given type that adapts the target service.
     */
    public static <T> T adapt(URL baseURL, Class<T> type, Function<ResourcePath, Map<String, ?>> keyMapFactory) {
        return adapt(baseURL, type, keyMapFactory, (method, url) -> {
            WebServiceProxy webServiceProxy = new WebServiceProxy(method, url);

            webServiceProxy.setEncoding(Encoding.MULTIPART_FORM_DATA);

            return webServiceProxy;
        });
    }

    /**
     * Creates a type-safe web service proxy adapter.
     *
     * @param <T>
     * The target type.
     *
     * @param baseURL
     * The base URL of the web service.
     *
     * @param type
     * The type of the adapter.
     *
     * @param keyMapFactory
     * A function that produces a key map, or <code>null</code> if no keys are required.
     *
     * @param webServiceProxyFactory
     * A function that produces a web service proxy instance.
     *
     * @return
     * An instance of the given type that adapts the target service.
     */
    public static <T> T adapt(URL baseURL, Class<T> type, Function<ResourcePath, Map<String, ?>> keyMapFactory, BiFunction<String, URL, WebServiceProxy> webServiceProxyFactory) {
        if (baseURL == null) {
            throw new IllegalArgumentException();
        }

        if (type == null) {
            throw new IllegalArgumentException();
        }

        if (keyMapFactory == null) {
            throw new IllegalArgumentException();
        }

        if (webServiceProxyFactory == null) {
            throw new IllegalArgumentException();
        }

        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
            new Class<?>[] {type},
            new TypedInvocationHandler(baseURL, type, keyMapFactory, webServiceProxyFactory)));
    }
}
