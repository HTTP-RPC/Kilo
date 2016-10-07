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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

/**
 * Invocation proxy for HTTP-RPC web services.
 */
public class WebServiceProxy {
    // Invocation callback
    private class InvocationCallback<V> implements Callable<V> {
        private String method;
        private String path;
        private Map<String, ?> arguments;
        private BiConsumer<V, Exception> resultHandler;

        private static final String POST_METHOD = "POST";

        private static final String AUTHORIZATION_KEY = "Authorization";
        private static final String ACCEPT_LANGUAGE_KEY = "Accept-Language";

        private static final String CONTENT_TYPE_KEY = "Content-Type";
        private static final String MULTIPART_FORM_DATA_MIME_TYPE = "multipart/form-data";
        private static final String BOUNDARY_PARAMETER_FORMAT = "; boundary=%s";

        private static final String OCTET_STREAM_MIME_TYPE = "application/octet-stream";

        private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition: form-data";
        private static final String NAME_PARAMETER_FORMAT = "; name=\"%s\"";
        private static final String FILENAME_PARAMETER_FORMAT = "; filename=\"%s\"";

        private static final String CRLF = "\r\n";

        private static final int EOF = -1;

        public InvocationCallback(String method, String path, Map<String, ?> arguments, BiConsumer<V, Exception> resultHandler) {
            this.method = method;
            this.path = path;
            this.arguments = arguments;
            this.resultHandler = resultHandler;
        }

        @Override
        public V call() throws Exception {
            final V result;
            try {
                result = invoke();
            } catch (final Exception exception) {
                if (resultHandler != null) {
                    dispatchResult(new Runnable() {
                        @Override
                        public void run() {
                            resultHandler.accept(null, exception);
                        }
                    });
                }

                throw exception;
            }

            if (resultHandler != null) {
                dispatchResult(new Runnable() {
                    @Override
                    public void run() {
                        resultHandler.accept(result, null);
                    }
                });
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        private V invoke() throws Exception {
            URL url = new URL(serverURL, path);

            // Construct query
            if (!method.equalsIgnoreCase(POST_METHOD)) {
                StringBuilder queryBuilder = new StringBuilder();

                for (Map.Entry<String, ?> argument : arguments.entrySet()) {
                    String name = argument.getKey();

                    if (name == null) {
                        continue;
                    }

                    List<?> values = getParameterValues(argument.getValue());

                    for (int i = 0, n = values.size(); i < n; i++) {
                        Object value = values.get(i);

                        if (value == null) {
                            continue;
                        }

                        if (queryBuilder.length() > 0) {
                            queryBuilder.append("&");
                        }

                        queryBuilder.append(URLEncoder.encode(name, UTF_8_ENCODING));
                        queryBuilder.append("=");
                        queryBuilder.append(URLEncoder.encode(value.toString(), UTF_8_ENCODING));
                    }
                }

                if (queryBuilder.length() > 0) {
                    url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + queryBuilder);
                }
            }

            // Open URL connection
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            connection.setRequestMethod(method);

            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);

            // Set language
            Locale locale = Locale.getDefault();
            String acceptLanguage = locale.getLanguage().toLowerCase() + "-" + locale.getCountry().toLowerCase();

            connection.setRequestProperty(ACCEPT_LANGUAGE_KEY, acceptLanguage);

            // Authenticate request
            if (authorization != null) {
                String credentials = String.format("%s:%s", authorization.getUserName(), new String(authorization.getPassword()));
                String value = String.format("Basic %s", base64Encode(credentials));

                connection.setRequestProperty(AUTHORIZATION_KEY, value);
            }

            // Write request body
            if (method.equalsIgnoreCase(POST_METHOD)) {
                connection.setDoOutput(true);

                String boundary = UUID.randomUUID().toString();
                String requestContentType = MULTIPART_FORM_DATA_MIME_TYPE + String.format(BOUNDARY_PARAMETER_FORMAT, boundary);

                connection.setRequestProperty(CONTENT_TYPE_KEY, requestContentType);

                String boundaryData = String.format("--%s%s", boundary, CRLF);

                // Write request body
                try (OutputStream outputStream = new MonitoredOutputStream(connection.getOutputStream())) {
                    OutputStreamWriter writer = new OutputStreamWriter(outputStream, Charset.forName(UTF_8_ENCODING));

                    for (Map.Entry<String, ?> argument : arguments.entrySet()) {
                        String name = argument.getKey();

                        if (name == null) {
                            continue;
                        }

                        List<?> values = getParameterValues(argument.getValue());

                        for (Object value : values) {
                            if (value == null) {
                                continue;
                            }

                            writer.append(boundaryData);

                            writer.append(CONTENT_DISPOSITION_HEADER);
                            writer.append(String.format(NAME_PARAMETER_FORMAT, name));

                            if (value instanceof URL) {
                                String path = ((URL)value).getPath();
                                String filename = path.substring(path.lastIndexOf('/') + 1);

                                writer.append(String.format(FILENAME_PARAMETER_FORMAT, filename));
                                writer.append(CRLF);

                                String attachmentContentType = URLConnection.guessContentTypeFromName(filename);

                                if (attachmentContentType == null) {
                                    attachmentContentType = OCTET_STREAM_MIME_TYPE;
                                }

                                writer.append(String.format("%s: %s%s", CONTENT_TYPE_KEY, attachmentContentType, CRLF));
                                writer.append(CRLF);

                                writer.flush();

                                try (InputStream inputStream = ((URL)value).openStream()) {
                                    int b;
                                    while ((b = inputStream.read()) != EOF) {
                                        outputStream.write(b);
                                    }
                                }
                            } else {
                                writer.append(CRLF);

                                writer.append(CRLF);
                                writer.append(value.toString());
                            }

                            writer.append(CRLF);
                        }
                    }

                    writer.append(String.format("--%s--%s", boundary, CRLF));

                    writer.flush();
                }
            }

            // Read response
            int responseCode = connection.getResponseCode();

            Object result;
            if (responseCode / 100 == 2) {
                try (InputStream inputStream = new MonitoredInputStream(connection.getInputStream())) {
                    result = decodeResponse(inputStream, connection.getContentType());
                }
            } else {
                throw new IOException(String.format("%d %s", responseCode, connection.getResponseMessage()));
            }

            return (V)result;
        }
    }

    // Monitored input stream
    private static class MonitoredInputStream extends BufferedInputStream {
        public MonitoredInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public int read() throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }

            return super.read();
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }

            return super.read(b, off, len);
        }
    }

    // Monitored output stream
    private static class MonitoredOutputStream extends BufferedOutputStream {
        public MonitoredOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        public void write(int b) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }

            super.write(b);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }

            super.write(b, off, len);
        }
    }

    private URL serverURL;
    private ExecutorService executorService;
    private int connectTimeout;
    private int readTimeout;

    private PasswordAuthentication authorization = null;

    private static final String UTF_8_ENCODING = "UTF-8";

    private static final String JSON_MIME_TYPE = "application/json";

    private static char[] lookup = new char[64];

    static {
        for (int i = 0; i < 26; i++) {
            lookup[i] = (char)('A' + i);
        }

        for (int i = 26, j = 0; i < 52; i++, j++) {
            lookup[i] = (char)('a' + j);
        }

        for (int i = 52, j = 0; i < 62; i++, j++) {
            lookup[i] = (char)('0' + j);
        }

        lookup[62] = '+';
        lookup[63] = '/';
    }

    /**
     * Creates a new web service proxy.
     *
     * @param serverURL
     * The server URL.
     *
     * @param executorService
     * The executor service that will be used to execute requests.
     */
    public WebServiceProxy(URL serverURL, ExecutorService executorService) {
        this(serverURL, executorService, 0, 0);
    }

    /**
     * Creates a new web service proxy.
     *
     * @param serverURL
     * The server URL.
     *
     * @param executorService
     * The executor service that will be used to execute requests.
     *
     * @param connectTimeout
     * The connect timeout.
     *
     * @param readTimeout
     * The read timeout.
     */
    public WebServiceProxy(URL serverURL, ExecutorService executorService, int connectTimeout, int readTimeout) {
        if (serverURL == null) {
            throw new IllegalArgumentException();
        }

        if (executorService == null) {
            throw new IllegalArgumentException();
        }

        this.serverURL = serverURL;
        this.executorService = executorService;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * Returns the server URL.
     *
     * @return
     * The server URL.
     */
    public URL getServerURL() {
        return serverURL;
    }

    /**
     * Returns the executor service that is used to execute requests.
     *
     * @return
     * The executor service that is used to execute requests.
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Returns the service proxy's authorization credentials.
     *
     * @return
     * The service proxy's authorization credentials, or <tt>null</tt> if no
     * credentials have been provided.
     */
    public PasswordAuthentication getAuthorization() {
        return authorization;
    }

    /**
     * Sets the service proxy's authorization credentials.
     *
     * @param authorization
     * The service proxy's authorization credentials, or <tt>null</tt> for no
     * credentials.
     */
    public void setAuthorization(PasswordAuthentication authorization) {
        this.authorization = authorization;
    }

    /**
     * Executes a service operation.
     *
     * @param <V> The type of the value returned by the operation.
     *
     * @param method
     * The HTTP verb associated with the request.
     *
     * @param path
     * The path associated with the request.
     *
     * @param resultHandler
     * A callback that will be invoked upon completion of the request, or
     * <tt>null</tt> for no result handler.
     *
     * @return
     * A future representing the invocation request.
     */
    @SuppressWarnings("unchecked")
    public <V> Future<V> invoke(String method, String path, BiConsumer<V, Exception> resultHandler) {
        return invoke(method, path, Collections.EMPTY_MAP, resultHandler);
    }

    /**
     * Executes a service operation.
     *
     * @param <V> The type of the value returned by the operation.
     *
     * @param method
     * The HTTP verb associated with the request.
     *
     * @param path
     * The path associated with the request.
     *
     * @param arguments
     * The request arguments.
     *
     * @param resultHandler
     * A callback that will be invoked upon completion of the request, or
     * <tt>null</tt> for no result handler.
     *
     * @return
     * A future representing the invocation request.
     */
    public <V> Future<V> invoke(String method, String path, Map<String, ?> arguments, BiConsumer<V, Exception> resultHandler) {
        if (method == null) {
            throw new IllegalArgumentException();
        }

        if (path == null) {
            throw new IllegalArgumentException();
        }

        if (arguments == null) {
            throw new IllegalArgumentException();
        }

        return executorService.submit(new InvocationCallback<>(method, path, arguments, resultHandler));
    }

    /**
     * Decodes a response value.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @param contentType
     * The MIME type of the content, or <tt>null</tt> if the content type is
     * unknown.
     *
     * @return
     * The decoded value, or <tt>null</tt> if the value could not be decoded.
     *
     * @throws IOException
     * If an exception occurs.
     */
    protected Object decodeResponse(InputStream inputStream, String contentType) throws IOException {
        Object value = null;

        if (contentType != null && contentType.startsWith(JSON_MIME_TYPE)) {
            JSONDecoder decoder = new JSONDecoder();

            value = decoder.readValue(inputStream);
        }

        return value;
    }

    /**
     * Dispatches a result value.
     *
     * @param command
     * A callback representing the result handler and the associated result or
     * exception.
     */
    protected void dispatchResult(Runnable command) {
        command.run();
    }

    /**
     * Creates a list from a variable length array of elements.
     *
     * @param elements
     * The elements from which the list will be created.
     *
     * @return
     * An immutable list containing the given elements.
     */
    @SafeVarargs
    public static List<?> listOf(Object...elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

    /**
     * Creates a map from a variable length array of map entries.
     *
     * @param <K> The type of the key.
     *
     * @param entries
     * The entries from which the map will be created.
     *
     * @return
     * An immutable map containing the given entries.
     */
    @SafeVarargs
    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) {
        HashMap<K, Object> map = new HashMap<>();

        for (Map.Entry<K, ?> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates a map entry.
     *
     * @param <K> The type of the key.
     *
     * @param key
     * The entry's key.
     *
     * @param value
     * The entry's value.
     *
     * @return
     * An immutable map entry containing the key/value pair.
     */
    public static <K> Map.Entry<K, ?> entry(K key, Object value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /**
     * Returns the value at a given path.
     *
     * @param <V>
     * The type of the value to return.
     *
     * @param root
     * The root object.
     *
     * @param path
     * The path to the value.
     *
     * @return
     * The value at the given path, or <tt>null</tt> if the value does not exist.
     */
    @SuppressWarnings("unchecked")
    public static <V> V valueAt(Map<String, ?> root, String path) {
        if (root == null) {
            throw new IllegalArgumentException();
        }

        if (path == null) {
            throw new IllegalArgumentException();
        }

        Object value = root;

        String[] components = path.split("\\.");

        for (int i = 0; i < components.length; i++) {
            String component = components[i];

            if (value instanceof Map<?, ?>) {
                value = ((Map<?, ?>)value).get(component);
            } else {
                value = null;

                break;
            }
        }

        return (V)value;
    }

    private static List<?> getParameterValues(Object argument) throws UnsupportedEncodingException {
        List<?> values;
        if (argument instanceof List<?>) {
            values = (List<?>)argument;
        } else {
            values = Collections.singletonList(argument);
        }

        return values;
    }

    private static String base64Encode(String value) {
        // TODO Use java.util.Base64 when Android fully supports Java 8
        byte[] bytes = value.getBytes();

        StringBuilder resultBuilder = new StringBuilder(4 * (bytes.length / 3 + 1));

        for (int i = 0, n = bytes.length; i < n; ) {
            byte byte0 = bytes[i++];
            byte byte1 = (i++ < n) ? bytes[i - 1] : 0;
            byte byte2 = (i++ < n) ? bytes[i - 1] : 0;

            resultBuilder.append(lookup[byte0 >> 2]);
            resultBuilder.append(lookup[((byte0 << 4) | byte1 >> 4) & 63]);
            resultBuilder.append(lookup[((byte1 << 2) | byte2 >> 6) & 63]);
            resultBuilder.append(lookup[byte2 & 63]);

            if (i > n) {
                for (int m = resultBuilder.length(), j = m - (i - n); j < m; j++) {
                    resultBuilder.setCharAt(j, '=');
                }
            }
        }

        return resultBuilder.toString();
    }
}

class JSONDecoder {
    private int c = EOF;
    private LinkedList<Object> collections = new LinkedList<>();

    private static final String UTF_8_ENCODING = "UTF-8";

    private static final String TRUE_KEYWORD = "true";
    private static final String FALSE_KEYWORD = "false";
    private static final String NULL_KEYWORD = "null";

    private static final int EOF = -1;

    public Object readValue(InputStream inputStream) throws IOException {
        return readValue(new InputStreamReader(inputStream, Charset.forName(UTF_8_ENCODING)));
    }

    @SuppressWarnings("unchecked")
    public Object readValue(Reader reader) throws IOException {
        c = reader.read();

        Object value = null;

        while (c != EOF) {
            String key = null;

            if (c == ']') {
                value = collections.pop();

                if (!(value instanceof List<?>)) {
                    throw new IOException("Unexpected closing bracket.");
                }

                c = reader.read();
            } else if (c == '}') {
                value = collections.pop();

                if (!(value instanceof Map<?, ?>)) {
                    throw new IOException("Unexpected closing brace.");
                }

                c = reader.read();
            } else if (c == ',') {
                c = reader.read();
            } else {
                Object collection = collections.peek();

                // If the current collection is a map, read the key
                if (collection instanceof Map<?, ?>) {
                    key = readString(reader);

                    skipWhitespace(reader);

                    if (c != ':') {
                        throw new IOException("Missing key/value delimiter.");
                    }

                    c = reader.read();

                    skipWhitespace(reader);
                }

                // Read the value
                if (c == '"') {
                    value = readString(reader);
                } else if (c == '+' || c == '-' || Character.isDigit(c)) {
                    value = readNumber(reader);
                } else if (c == 't') {
                    if (!readKeyword(reader, TRUE_KEYWORD)) {
                        throw new IOException();
                    }

                    value = Boolean.TRUE;
                } else if (c == 'f') {
                    if (!readKeyword(reader, FALSE_KEYWORD)) {
                        throw new IOException();
                    }

                    value = Boolean.FALSE;
                } else if (c == 'n') {
                    if (!readKeyword(reader, NULL_KEYWORD)) {
                        throw new IOException();
                    }

                    value = null;
                } else if (c == '[') {
                    value = new ArrayList<>();

                    collections.push(value);

                    c = reader.read();
                } else if (c == '{') {
                    value = new HashMap<String, Object>();

                    collections.push(value);

                    c = reader.read();
                } else {
                    throw new IOException("Unexpected character in input stream.");
                }

                // Add the value to the current collection
                if (collection != null) {
                    if (key != null) {
                        ((Map<String, Object>)collection).put(key, value);
                    } else {
                        ((List<Object>)collection).add(value);
                    }

                    if (!(value instanceof List<?> || value instanceof Map<?, ?>)) {
                        skipWhitespace(reader);

                        if (c != ']' && c != '}' && c != ',') {
                            throw new IOException("Undelimited or unterminated collection.");
                        }
                    }
                }
            }

            skipWhitespace(reader);
        }

        return value;
    }

    private void skipWhitespace(Reader reader) throws IOException {
        while (c != EOF && Character.isWhitespace(c)) {
            c = reader.read();
        }
    }

    private String readString(Reader reader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        // Move to the next character after the opening quotes
        c = reader.read();

        while (c != EOF && c != '"') {
            if (Character.isISOControl(c)) {
                throw new IOException("Illegal character in input stream.");
            }

            if (c == '\\') {
                c = reader.read();

                if (c == 'b') {
                    c = '\b';
                } else if (c == 'f') {
                    c = '\f';
                } else if (c == 'r') {
                    c = '\r';
                } else if (c == 'n') {
                    c = '\n';
                } else if (c == 't') {
                    c = '\t';
                } else if (c == 'u') {
                    StringBuilder unicodeValueBuilder = new StringBuilder();

                    while (c != EOF && unicodeValueBuilder.length() < 4) {
                        c = reader.read();
                        unicodeValueBuilder.append((char)c);
                    }

                    if (c == EOF) {
                        throw new IOException("Invalid Unicode escape sequence.");
                    }

                    String unicodeValue = unicodeValueBuilder.toString();

                    c = (char)Integer.parseInt(unicodeValue, 16);
                } else if (c != '"' && c != '\\' && c != '/') {
                    throw new IOException("Unsupported escape sequence in input stream.");
                }
            }

            stringBuilder.append((char)c);

            c = reader.read();
        }

        if (c != '"') {
            throw new IOException("Unterminated string in input stream.");
        }

        // Move to the next character after the closing quotes
        c = reader.read();

        return stringBuilder.toString();
    }

    private Number readNumber(Reader reader) throws IOException {
        Number value = null;

        boolean negative = false;
        boolean decimal = false;

        StringBuilder numberBuilder = new StringBuilder();

        if (c == '+' || c == '-') {
            negative = (c == '-');

            c = reader.read();
        }

        while (c != EOF && (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '-')) {
            numberBuilder.append((char)c);
            decimal |= (c == '.');

            c = reader.read();
        }

        if (decimal) {
            value = Double.valueOf(numberBuilder.toString()) * (negative ? -1.0 : 1.0);
        } else {
            value = Long.valueOf(numberBuilder.toString()) * (negative ? -1 : 1);
        }

        return new NumberAdapter(value);
    }

    private boolean readKeyword(Reader reader, String keyword) throws IOException {
        int n = keyword.length();
        int i = 0;

        while (c != EOF && i < n) {
            if (keyword.charAt(i) != c) {
                break;
            }

            c = reader.read();
            i++;
        }

        return (i == n);
    }
}

// Number adapter
class NumberAdapter extends Number {
    private static final long serialVersionUID = 0;

    private Number number;

    public NumberAdapter(Number number) {
        this.number = number;
    }

    @Override
    public int intValue() {
        return number.intValue();
    }

    @Override
    public long longValue() {
        return number.longValue();
    }

    @Override
    public float floatValue() {
        return number.floatValue();
    }

    @Override
    public double doubleValue() {
        return number.doubleValue();
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(doubleValue());

        return (int)(bits ^ (bits >>> 32));
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof Number && equals((Number)object));
    }

    private boolean equals(Number number) {
        return (Double.doubleToLongBits(doubleValue()) == Double.doubleToLongBits(number.doubleValue()));
    }
}

