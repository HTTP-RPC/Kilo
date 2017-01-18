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
import java.io.Writer;
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

/**
 * Web service invocation proxy.
 */
public class WebServiceProxy {
    private URL serverURL;
    private ExecutorService executorService;

    private int connectTimeout = 0;
    private int readTimeout = 0;

    private String encoding = MULTIPART_FORM_DATA;

    private PasswordAuthentication authorization = null;

    private String multipartBoundary = UUID.randomUUID().toString();

    /**
     * URL-encoded form data encoding.
     */
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    /**
     * Multi-part form data encoding.
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    /**
     * JSON encoding.
     */
    public static final String APPLICATION_JSON = "application/json";

    private static final String UTF_8 = "UTF-8";
    private static final String CRLF = "\r\n";

    private static final int EOF = -1;

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
        if (serverURL == null) {
            throw new IllegalArgumentException();
        }

        if (executorService == null) {
            throw new IllegalArgumentException();
        }

        this.serverURL = serverURL;
        this.executorService = executorService;
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
     * Returns the encoding used to submit service requests.
     *
     * @return
     * The encoding used to submit service requests
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Sets the encoding used to submit service requests.
     *
     * @param encoding
     * The encoding used to submit service requests.
     */
    public void setEncoding(String encoding) {
        if (encoding == null) {
            throw new IllegalArgumentException();
        }

        this.encoding = encoding.toLowerCase();
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
    public <V> Future<V> invoke(String method, String path, ResultHandler<V> resultHandler) {
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
    @SuppressWarnings("unchecked")
    public <V> Future<V> invoke(String method, String path, Map<String, ?> arguments, ResultHandler<V> resultHandler) {
        if (method == null) {
            throw new IllegalArgumentException();
        }

        if (path == null) {
            throw new IllegalArgumentException();
        }

        if (arguments == null) {
            throw new IllegalArgumentException();
        }

        // Capture state
        int connectTimeout = this.connectTimeout;
        int readTimeout = this.readTimeout;

        String encoding = this.encoding;

        PasswordAuthentication authorization = this.authorization;

        // TODO Use a lambda expression when Android issue 211386 is resolved:
        // https://code.google.com/p/android/issues/detail?id=211386
        return executorService.submit(new Callable<V>() {
            @Override
            public V call() throws IOException {
                Object result;
                try {
                    URL url = new URL(serverURL, path);

                    // Construct query
                    boolean upload = (method.equalsIgnoreCase("POST")
                        || (method.equalsIgnoreCase("PUT") && encoding.equals(APPLICATION_JSON)));

                    if (!upload) {
                        String query = encodeQuery(arguments);

                        if (query.length() > 0) {
                            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
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

                    connection.setRequestProperty("Accept", String.format("%s, image/*, text/*", APPLICATION_JSON));
                    connection.setRequestProperty("Accept-Language", acceptLanguage);

                    // Authenticate request
                    if (authorization != null) {
                        String credentials = String.format("%s:%s", authorization.getUserName(), new String(authorization.getPassword()));
                        String value = String.format("Basic %s", base64Encode(credentials));

                        connection.setRequestProperty("Authorization", value);
                    }

                    // Write request body
                    if (upload) {
                        connection.setDoOutput(true);

                        String contentType;
                        if (encoding.equals(MULTIPART_FORM_DATA)) {
                            contentType = String.format("%s; boundary=%s", encoding, multipartBoundary);
                        } else {
                            contentType = encoding;
                        }

                        connection.setRequestProperty("Content-Type", String.format("%s;charset=%s", contentType, UTF_8));

                        try (OutputStream outputStream = new MonitoredOutputStream(connection.getOutputStream())) {
                            if (encoding.equals(MULTIPART_FORM_DATA)) {
                                encodeMultipartFormDataRequest(arguments, outputStream, multipartBoundary);
                            } else if (encoding.equals(APPLICATION_X_WWW_FORM_URLENCODED)) {
                                encodeApplicationXWWWFormURLEncodedRequest(arguments, outputStream);
                            } else if (encoding.equals(APPLICATION_JSON)) {
                                JSONEncoder encoder = new JSONEncoder();

                                encoder.writeValue(arguments, outputStream);
                            } else {
                                throw new UnsupportedOperationException("Unsupported request encoding.");
                            }
                        }
                    }

                    // Read response
                    int responseCode = connection.getResponseCode();

                    if (responseCode / 100 == 2) {
                        if (responseCode % 100 == 0) {
                            String contentType = connection.getContentType();

                            if (contentType == null) {
                                contentType = APPLICATION_JSON;
                            }

                            try (InputStream inputStream = new MonitoredInputStream(connection.getInputStream())) {
                                MIMEType mimeType = MIMEType.valueOf(contentType);

                                String type = mimeType.getType();
                                String subtype = mimeType.getSubtype();

                                if (type.equals("application") && subtype.equals("json")) {
                                    JSONDecoder decoder = new JSONDecoder();

                                    result = decoder.readValue(inputStream);
                                } else if (type.equals("image")) {
                                    result = decodeImageResponse(inputStream, subtype);
                                } else if (type.equals("text")) {
                                    String charsetName = mimeType.getParameter("charset");

                                    if (charsetName == null) {
                                        charsetName = UTF_8;
                                    }

                                    result = decodeTextResponse(inputStream, subtype, Charset.forName(charsetName));
                                } else {
                                    throw new UnsupportedOperationException("Unsupported response encoding.");
                                }
                            }
                        } else {
                            result = null;
                        }
                    } else {
                        throw new WebServiceException(connection.getResponseMessage(), responseCode);
                    }
                } catch (Exception exception) {
                    if (resultHandler != null) {
                        // TODO Android issue 211386
                        dispatchResult(new Runnable() {
                            @Override
                            public void run() {
                                resultHandler.execute(null, exception);
                            }
                        });
                    }

                    throw exception;
                }

                if (resultHandler != null) {
                    // TODO Android issue 211386
                    dispatchResult(new Runnable() {
                        @Override
                        public void run() {
                            resultHandler.execute((V)result, null);
                        }
                    });
                }

                return (V)result;
            }
        });
    }

    private static String encodeQuery(Map<String, ?> arguments) throws UnsupportedEncodingException {
        StringBuilder queryBuilder = new StringBuilder();

        int i = 0;

        for (Map.Entry<String, ?> argument : arguments.entrySet()) {
            String name = argument.getKey();

            if (name == null) {
                continue;
            }

            for (Object value : getParameterValues(argument.getValue())) {
                if (value == null) {
                    continue;
                }

                if (i > 0) {
                    queryBuilder.append("&");
                }

                queryBuilder.append(URLEncoder.encode(name, UTF_8));
                queryBuilder.append("=");
                queryBuilder.append(URLEncoder.encode(value.toString(), UTF_8));

                i++;
            }
        }

        return queryBuilder.toString();
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

    private static void encodeMultipartFormDataRequest(Map<String, ?> arguments, OutputStream outputStream, String boundary) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, Charset.forName(UTF_8));

        for (Map.Entry<String, ?> argument : arguments.entrySet()) {
            String name = argument.getKey();

            if (name == null) {
                continue;
            }

            for (Object value : getParameterValues(argument.getValue())) {
                if (value == null) {
                    continue;
                }

                writer.append(String.format("--%s%s", boundary, CRLF));
                writer.append(String.format("Content-Disposition: form-data; name=\"%s\"", name));

                if (value instanceof URL) {
                    String path = ((URL)value).getPath();
                    String filename = path.substring(path.lastIndexOf('/') + 1);

                    writer.append(String.format("; filename=\"%s\"", filename));
                    writer.append(CRLF);

                    String attachmentContentType = URLConnection.guessContentTypeFromName(filename);

                    if (attachmentContentType == null) {
                        attachmentContentType = "application/octet-stream";
                    }

                    writer.append(String.format("%s: %s%s", "Content-Type", attachmentContentType, CRLF));
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

    private static void encodeApplicationXWWWFormURLEncodedRequest(Map<String, ?> arguments, OutputStream outputStream) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, Charset.forName(UTF_8));

        int i = 0;

        for (Map.Entry<String, ?> argument : arguments.entrySet()) {
            String name = argument.getKey();

            if (name == null) {
                continue;
            }

            for (Object value : getParameterValues(argument.getValue())) {
                if (value == null) {
                    continue;
                }

                if (i > 0) {
                    writer.append("&");
                }

                writer.append(URLEncoder.encode(name, UTF_8));
                writer.append("=");
                writer.append(URLEncoder.encode(value.toString(), UTF_8));

                i++;
            }
        }

        writer.flush();
    }

    private static Iterable<?> getParameterValues(Object argument) throws UnsupportedEncodingException {
        Iterable<?> values;
        if (argument instanceof Iterable<?>) {
            values = (Iterable<?>)argument;
        } else {
            values = Collections.singletonList(argument);
        }

        return values;
    }

    /**
     * Decodes an image response. The default implementation throws
     * {@link UnsupportedOperationException}.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @param imageType
     * The image subtype.
     *
     * @return
     * The decoded image content.
     *
     * @throws IOException
     * If an exception occurs.
     */
    protected Object decodeImageResponse(InputStream inputStream, String imageType) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Decodes a text response. The default implementation returns a
     * {@link String}.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @param textType
     * The text subtype.
     *
     * @param charset
     * The character set used to encode the text.
     *
     * @return
     * The decoded text content.
     *
     * @throws IOException
     * If an exception occurs.
     */
    protected Object decodeTextResponse(InputStream inputStream, String textType, Charset charset) throws IOException {
        StringBuilder textBuilder = new StringBuilder(1024);

        try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
            int c;
            while ((c = reader.read()) != EOF) {
                textBuilder.append((char)c);
            }
        }

        return textBuilder.toString();
    }

    /**
     * Dispatches a result. The default implementation executes the callback
     * immediately on the current thread.
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
}

// MIME type
class MIMEType {
    private String type;
    private String subtype;

    private HashMap<String, String> parameters = new HashMap<>();

    private MIMEType(String type, String subtype) {
        this.type = type;
        this.subtype = subtype;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public static MIMEType valueOf(String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        int n = value.length();
        int i = 0;

        // Type
        StringBuilder typeBuilder = new StringBuilder();

        while (i < n) {
            char c = value.charAt(i++);

            if (c == '/') {
                break;
            }

            typeBuilder.append(Character.toLowerCase(c));
        }

        // Subtype
        StringBuilder subtypeBuilder = new StringBuilder();

        while (i < n) {
            char c = value.charAt(i++);

            if (c == ';') {
                break;
            }

            subtypeBuilder.append(Character.toLowerCase(c));
        }

        // Parameters
        MIMEType mimeType = new MIMEType(typeBuilder.toString(), subtypeBuilder.toString());

        while (i < n) {
            StringBuilder keyBuilder = new StringBuilder();

            while (i < n) {
                char c = value.charAt(i++);

                if (c == '=') {
                    break;
                }

                keyBuilder.append(Character.toLowerCase(c));
            }

            StringBuilder valueBuilder = new StringBuilder();

            while (i < n) {
                char c = value.charAt(i++);

                if (c == ';') {
                    break;
                }

                valueBuilder.append(Character.toLowerCase(c));
            }

            mimeType.parameters.put(keyBuilder.toString().trim(), valueBuilder.toString().trim());
        }

        return mimeType;
    }
}

// Monitored input stream
class MonitoredInputStream extends BufferedInputStream {
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
class MonitoredOutputStream extends BufferedOutputStream {
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

// JSON decoder
class JSONDecoder {
    // Number adapter
    private static class NumberAdapter extends Number {
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
            long l1 = Double.doubleToLongBits(doubleValue());
            long l2 = Double.doubleToLongBits(number.doubleValue());
            return (l1 == l2);
        }

        @Override
        public String toString() {
            return number.toString();
        }
    }

    private int c = EOF;

    private LinkedList<Object> collections = new LinkedList<>();

    private static final String TRUE_KEYWORD = "true";
    private static final String FALSE_KEYWORD = "false";
    private static final String NULL_KEYWORD = "null";

    private static final int EOF = -1;

    public Object readValue(InputStream inputStream) throws IOException {
        return readValue(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    }

    @SuppressWarnings("unchecked")
    public Object readValue(Reader reader) throws IOException {
        c = reader.read();

        Object value = null;

        skipWhitespace(reader);

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
        boolean decimal = false;

        StringBuilder numberBuilder = new StringBuilder();

        while (c != EOF && (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '-')) {
            numberBuilder.append((char)c);
            decimal |= (c == '.');

            c = reader.read();
        }

        Number value;
        if (decimal) {
            value = Double.valueOf(numberBuilder.toString());
        } else {
            value = Long.valueOf(numberBuilder.toString());
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

// JSON encoder
class JSONEncoder {
    private int depth = 0;

    public void writeValue(Object value, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
        writeValue(value, writer);

        writer.flush();
    }

    public void writeValue(Object value, Writer writer) throws IOException {
        if (value == null) {
            writer.append(null);
        } else if (value instanceof CharSequence) {
            CharSequence text = (CharSequence)value;

            writer.append("\"");

            for (int i = 0, n = text.length(); i < n; i++) {
                char c = text.charAt(i);

                if (c == '"' || c == '\\') {
                    writer.append("\\" + c);
                } else if (c == '\b') {
                    writer.append("\\b");
                } else if (c == '\f') {
                    writer.append("\\f");
                } else if (c == '\n') {
                    writer.append("\\n");
                } else if (c == '\r') {
                    writer.append("\\r");
                } else if (c == '\t') {
                    writer.append("\\t");
                } else {
                    writer.append(c);
                }
            }

            writer.append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            writer.append(String.valueOf(value));
        } else if (value instanceof Iterable<?>) {
            writer.append("[");

            depth++;

            int i = 0;

            for (Object element : (Iterable<?>)value) {
                if (i > 0) {
                    writer.append(",");
                }

                writer.append("\n");

                indent(writer);

                writeValue(element, writer);

                i++;
            }

            depth--;

            writer.append("\n");

            indent(writer);

            writer.append("]");
        } else if (value instanceof Map<?, ?>) {
            writer.append("{");

            depth++;

            int i = 0;

            for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
                if (i > 0) {
                    writer.append(",");
                }

                writer.append("\n");

                Object key = entry.getKey();

                if (key == null) {
                    continue;
                }

                indent(writer);

                writeValue(key.toString(), writer);

                writer.append(": ");

                writeValue(entry.getValue(), writer);

                i++;
            }

            depth--;

            writer.append("\n");

            indent(writer);

            writer.append("}");
        } else {
            throw new IOException("Unsupported value type.");
        }
    }

    private void indent(Writer writer) throws IOException {
        for (int i = 0; i < depth; i++) {
            writer.append("  ");
        }
    }
}
