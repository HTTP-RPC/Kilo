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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Invocation proxy for HTTP-RPC web services.
 */
public class WebServiceProxy {
    // Invocation callback
    private class InvocationCallback<V> implements Callable<V> {
        private URL methodURL;
        private Map<String, ?> arguments;
        private Map<String, List<URL>> attachments;
        private ResultHandler<V> resultHandler;

        private int c = EOF;
        private LinkedList<Object> collections = new LinkedList<>();

        private static final int EOF = -1;

        private static final String ACCEPT_LANGUAGE_KEY = "Accept-Language";

        private static final String CONTENT_TYPE_KEY = "Content-Type";

        private static final String WWW_FORM_URL_ENCODED_MIME_TYPE = "application/x-www-form-urlencoded";

        private static final String MULTIPART_FORM_DATA_MIME_TYPE = "multipart/form-data";
        private static final String BOUNDARY_PARAMETER_FORMAT = "; boundary=%s";

        private static final String OCTET_STREAM_MIME_TYPE = "application/octet-stream";

        private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition: form-data";
        private static final String NAME_PARAMETER_FORMAT = "; name=\"%s\"";
        private static final String FILENAME_PARAMETER_FORMAT = "; filename=\"%s\"";

        private static final String CRLF = "\r\n";

        private static final String TRUE_KEYWORD = "true";
        private static final String FALSE_KEYWORD = "false";
        private static final String NULL_KEYWORD = "null";

        private static final String CHARSET_KEY = "charset";

        public InvocationCallback(URL methodURL, Map<String, ?> arguments, Map<String, List<URL>> attachments, ResultHandler<V> resultHandler) {
            this.methodURL = methodURL;
            this.arguments = arguments;
            this.attachments = attachments;
            this.resultHandler = resultHandler;
        }

        @Override
        public V call() throws Exception {
            final V result;
            try {
                // Open URL connection
                HttpURLConnection connection = (HttpURLConnection)methodURL.openConnection();

                connection.setRequestMethod("POST");

                connection.setDoInput(true);
                connection.setDoOutput(true);

                // Set language
                Locale locale = Locale.getDefault();
                String acceptLanguage = locale.getLanguage().toLowerCase() + "-" + locale.getCountry().toLowerCase();

                connection.setRequestProperty(ACCEPT_LANGUAGE_KEY, acceptLanguage);

                // Authenticate request
                if (authentication != null) {
                    authentication.authenticate(connection);
                }

                if (attachments.size() == 0) {
                    connection.setRequestProperty(CONTENT_TYPE_KEY, WWW_FORM_URL_ENCODED_MIME_TYPE);

                    // Construct parameter list
                    StringBuilder parameters = new StringBuilder();

                    for (Map.Entry<String, ?> argument : arguments.entrySet()) {
                        String name = argument.getKey();

                        if (name == null) {
                            continue;
                        }

                        List<?> values = getParameterValues(argument.getValue());

                        for (int i = 0, n = values.size(); i < n; i++) {
                            Object element = values.get(i);

                            if (element == null) {
                                continue;
                            }

                            if (parameters.length() > 0) {
                                parameters.append("&");
                            }

                            String value = getParameterValue(element);

                            parameters.append(URLEncoder.encode(name, UTF_8_ENCODING));
                            parameters.append("=");
                            parameters.append(URLEncoder.encode(value, UTF_8_ENCODING));
                        }
                    }

                    // Write request body
                    try (OutputStream outputStream = new MonitoredOutputStream(connection.getOutputStream())) {
                        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                            writer.write(parameters.toString());
                        }
                    }
                } else {
                    String boundary = UUID.randomUUID().toString();
                    String requestContentType = MULTIPART_FORM_DATA_MIME_TYPE + String.format(BOUNDARY_PARAMETER_FORMAT, boundary);

                    connection.setRequestProperty(CONTENT_TYPE_KEY, requestContentType);

                    String boundaryData = String.format("--%s%s", boundary, CRLF);

                    // Write request body
                    try (OutputStream outputStream = new MonitoredOutputStream(connection.getOutputStream())) {
                        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                            for (Map.Entry<String, ?> argument : arguments.entrySet()) {
                                String name = argument.getKey();

                                if (name == null) {
                                    continue;
                                }

                                List<?> values = getParameterValues(argument.getValue());

                                for (Object element : values) {
                                    if (element == null) {
                                        continue;
                                    }

                                    String value = getParameterValue(element);

                                    writer.append(boundaryData);

                                    writer.append(CONTENT_DISPOSITION_HEADER);
                                    writer.append(String.format(NAME_PARAMETER_FORMAT, name));

                                    writer.append(CRLF);
                                    writer.append(CRLF);
                                    writer.append(value);
                                    writer.append(CRLF);
                                }
                            }

                            for (Map.Entry<String, List<URL>> attachment : attachments.entrySet()) {
                                String name = attachment.getKey();
                                List<URL> urls = attachment.getValue();

                                if (urls == null) {
                                    continue;
                                }

                                for (URL url : urls) {
                                    if (url == null) {
                                        continue;
                                    }

                                    writer.append(boundaryData);

                                    writer.append(CONTENT_DISPOSITION_HEADER);
                                    writer.append(String.format(NAME_PARAMETER_FORMAT, name));

                                    String path = url.getPath();
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

                                    try (InputStream inputStream = url.openStream()) {
                                        int b;
                                        while ((b = inputStream.read()) != EOF) {
                                            outputStream.write(b);
                                        }
                                    }

                                    writer.append(CRLF);
                                }
                            }

                            writer.append(String.format("--%s--%s", boundary, CRLF));
                        }
                    }
                }

                // Read response
                int status = connection.getResponseCode();

                if (status == HttpURLConnection.HTTP_OK) {
                    String charsetName = getCharsetName(connection.getContentType());

                    if (charsetName == null) {
                        charsetName = UTF_8_ENCODING;
                    }

                    try (InputStream inputStream = new MonitoredInputStream(connection.getInputStream())) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charsetName))) {
                            result = readValue(reader);
                        }
                    }
                } else {
                    throw new IOException(String.format("%d %s", status, connection.getResponseMessage()));
                }
            } catch (final Exception exception) {
                resultDispatcher.execute(new Runnable() {
                    @Override
                    public void run() {
                        resultHandler.execute(null, exception);
                    }
                });

                throw exception;
            }

            resultDispatcher.execute(new Runnable() {
                @Override
                public void run() {
                    resultHandler.execute(result, null);
                }
            });

            return result;
        }

        private String getCharsetName(String contentType) {
            String charsetName = null;

            if (contentType != null) {
                int i = contentType.indexOf(CHARSET_KEY);

                if (i != -1) {
                    i += CHARSET_KEY.length();

                    int n = contentType.length();

                    if (i < n && contentType.charAt(i) == '=') {
                        int j = contentType.indexOf(";", ++i);

                        if (j == -1) {
                            j = n;
                        }

                        if (j > i) {
                            charsetName = contentType.substring(i, j);
                        }
                    }
                }
            }

            return charsetName;
        }

        @SuppressWarnings("unchecked")
        private V readValue(Reader reader) throws IOException {
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

            return (V)value;
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

            StringBuilder numberBuilder = new StringBuilder();
            boolean negative = false;
            boolean integer = true;

            if (c == '+' || c == '-') {
                negative = (c == '-');

                c = reader.read();
            }

            while (c != EOF && (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '-')) {
                numberBuilder.append((char)c);
                integer &= !(c == '.');

                c = reader.read();
            }

            if (integer) {
                value = Long.valueOf(numberBuilder.toString()) * (negative ? -1 : 1);
            } else {
                value = Double.valueOf(numberBuilder.toString()) * (negative ? -1.0 : 1.0);
            }

            return value;
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

    // Monitored input stream
    private static class MonitoredInputStream extends InputStream {
        private InputStream inputStream;

        private int count = 0;

        public MonitoredInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            int b = inputStream.read();

            if (b != -1 && ++count % PAGE_SIZE == 0 && Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }

            return b;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }

    // Monitored output stream
    private static class MonitoredOutputStream extends OutputStream {
        private OutputStream outputStream;

        private int count = 0;

        public MonitoredOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);

            if (++count % PAGE_SIZE == 0 && Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException();
            }
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
        }
    }

    private URL baseURL;
    private ExecutorService executorService;

    private Authentication authentication = null;

    private static Executor resultDispatcher = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    private static final int PAGE_SIZE = 1024;

    private static final String UTF_8_ENCODING = "UTF-8";

    /**
     * Creates a new HTTP-RPC service proxy.
     *
     * @param baseURL
     * The base URL of the service.
     *
     * @param executorService
     * The executor service that will be used to execute requests.
     */
    public WebServiceProxy(URL baseURL, ExecutorService executorService) {
        if (baseURL == null) {
            throw new IllegalArgumentException();
        }

        if (executorService == null) {
            throw new IllegalArgumentException();
        }

        this.baseURL = baseURL;
        this.executorService = executorService;
    }

    /**
     * Returns the service's base URL.
     *
     * @return
     * The service's base URL.
     */
    public URL getBaseURL() {
        return baseURL;
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
     * Returns the service proxy's authentication provider.
     *
     * @return
     * The service proxy's authentication provider.
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * Sets the service proxy's authentication provider.
     *
     * @param authentication
     * The service proxy's authentication provider, or <tt>null</tt> for no
     * authentication provider.
     */
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * Invokes an HTTP-RPC service method.
     *
     * @param <V> The type of the value returned by the method.
     *
     * @param methodName
     * The name of the method to invoke.
     *
     * @param resultHandler
     * A callback that will be invoked upon completion of the method.
     *
     * @return
     * A future representing the invocation request.
     */
    @SuppressWarnings("unchecked")
    public <V> Future<V> invoke(String methodName, ResultHandler<V> resultHandler) {
        return invoke(methodName, Collections.EMPTY_MAP, resultHandler);
    }

    /**
     * Invokes an HTTP-RPC service method.
     *
     * @param <V> The type of the value returned by the method.
     *
     * @param methodName
     * The name of the method to invoke.
     *
     * @param arguments
     * The method arguments.
     *
     * @param resultHandler
     * A callback that will be invoked upon completion of the method.
     *
     * @return
     * A future representing the invocation request.
     */
    @SuppressWarnings("unchecked")
    public <V> Future<V> invoke(String methodName, Map<String, ?> arguments, ResultHandler<V> resultHandler) {
        return invoke(methodName, arguments, Collections.EMPTY_MAP, resultHandler);
    }

    /**
     * Invokes an HTTP-RPC service method.
     *
     * @param <V> The type of the value returned by the method.
     *
     * @param methodName
     * The name of the method to invoke.
     *
     * @param arguments
     * The method arguments.
     *
     * @param attachments
     * The method attachments.
     *
     * @param resultHandler
     * A callback that will be invoked upon completion of the method.
     *
     * @return
     * A future representing the invocation request.
     */
    public <V> Future<V> invoke(String methodName, Map<String, ?> arguments, Map<String, List<URL>> attachments, ResultHandler<V> resultHandler) {
        if (methodName == null) {
            throw new IllegalArgumentException();
        }

        if (arguments == null) {
            throw new IllegalArgumentException();
        }

        if (attachments == null) {
            throw new IllegalArgumentException();
        }

        if (resultHandler == null) {
            throw new IllegalArgumentException();
        }

        URL methodURL;
        try {
            methodURL = new URL(baseURL, methodName);
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException(exception);
        }

        return executorService.submit(new InvocationCallback<>(methodURL, arguments, attachments, resultHandler));
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
        LinkedHashMap<K, Object> map = new LinkedHashMap<>();

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
     * Returns the result dispatcher.
     *
     * @return
     * The result dispatcher.
     */
    public static Executor getResultDispatcher() {
        return resultDispatcher;
    }

    /**
     * Sets the result dispatcher.
     *
     * @param resultDispatcher
     * The result dispatcher.
     */
    public static void setResultDispatcher(Executor resultDispatcher) {
        if (resultDispatcher == null) {
            throw new IllegalArgumentException();
        }

        WebServiceProxy.resultDispatcher = resultDispatcher;
    }

    private static List<?> getParameterValues(Object argument) throws UnsupportedEncodingException {
        List<?> values;
        if (argument instanceof List<?>) {
            values = (List<?>)argument;
        } else if (argument instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>)argument;

            ArrayList<Object> entries = new ArrayList<>(map.size());

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();

                if (key == null) {
                    continue;
                }

                Object value = entry.getValue();

                if (value == null) {
                    continue;
                }

                entries.add(String.format("%s:%s",
                    URLEncoder.encode(key.toString(), UTF_8_ENCODING),
                    URLEncoder.encode(getParameterValue(value), UTF_8_ENCODING)));
            }

            values = entries;
        } else {
            values = Collections.singletonList(argument);
        }

        return values;
    }

    private static String getParameterValue(Object element) {
        if (!(element instanceof String || element instanceof Number || element instanceof Boolean)) {
            throw new IllegalArgumentException("Invalid collection element.");
        }

        return element.toString();
    }
}
