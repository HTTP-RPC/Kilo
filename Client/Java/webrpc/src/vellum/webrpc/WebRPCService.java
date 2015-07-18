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

package vellum.webrpc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.activation.MimeType;

/**
 * Invocation proxy for web RPC services.
 */
public class WebRPCService {
    // Invocation callback
    private class InvocationCallback implements Callable<Object> {
        // Monitored input stream
        private class MonitoredInputStream extends InputStream {
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
        private class MonitoredOutputStream extends OutputStream {
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

        private String methodName;
        private Map<String, Object> arguments;
        private ResultHandler resultHandler;

        private int c = EOF;
        private LinkedList<Object> collections = new LinkedList<>();

        private static final int EOF = -1;
        private static final int PAGE_SIZE = 1<<10;

        private static final String TRUE_KEYWORD = "true";
        private static final String FALSE_KEYWORD = "false";
        private static final String NULL_KEYWORD = "null";

        private static final String UTF_8_ENCODING = "UTF-8";

        public InvocationCallback(String methodName, Map<String, Object> arguments, ResultHandler resultHandler) {
            this.methodName = methodName;
            this.arguments = arguments;
            this.resultHandler = resultHandler;
        }

        @Override
        public Object call() throws Exception {
            Object result;
            try {
                // Open URL connection
                URL methodURL = new URL(baseURL, methodName);

                HttpURLConnection connection = (HttpURLConnection)methodURL.openConnection();

                connection.setRequestMethod("POST");

                connection.setDoInput(true);
                connection.setDoOutput(true);

                // Create request body
                StringBuilder parameters = new StringBuilder();

                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    if (parameters.length() > 0) {
                        parameters.append("&");
                    }

                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value != null) {
                        if (value instanceof Object[]) {
                            Object[] values = (Object[])value;

                            for (int i = 0, n = values.length; i < n; i++) {
                                Object element = values[i];

                                if (element != null) {
                                    if (parameters.length() > 0) {
                                        parameters.append("&");
                                    }

                                    parameters.append(key + "=" + URLEncoder.encode(element.toString(), UTF_8_ENCODING));
                                }
                            }
                        } else {
                            parameters.append(key + "=" + URLEncoder.encode(value.toString(), UTF_8_ENCODING));
                        }
                    }
                }

                // Write request body
                try (OutputStream outputStream = new MonitoredOutputStream(connection.getOutputStream())) {
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                        writer.write(parameters.toString());
                    }
                }

                // Read response
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream inputStream = new MonitoredInputStream(connection.getInputStream())) {
                        Charset charset = null;

                        String contentType = connection.getContentType();

                        if (contentType != null) {
                            MimeType mimeType = new MimeType(contentType);
                            String charsetName = mimeType.getParameters().get("charset");

                            if (charsetName != null) {
                                charset = Charset.forName(charsetName);
                            }
                        }

                        if (charset == null) {
                            charset = Charset.forName(UTF_8_ENCODING);
                        }

                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
                            result = readValue(reader);
                        }
                    }
                } else {
                    throw new IOException(connection.getResponseMessage());
                }
            } catch (Exception exception) {
                dispatcher.dispatchException(exception, resultHandler);

                throw exception;
            }

            dispatcher.dispatchResult(result, resultHandler);

            return result;
        }

        @SuppressWarnings("unchecked")
        private Object readValue(Reader reader) throws IOException {
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
                    } else if (c != '\\'
                        && c != '/'
                        && c != '"') {
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

    private URL baseURL;
    private ExecutorService executorService;
    private Dispatcher dispatcher;

    /**
     * Creates a new RPC service.
     *
     * @param baseURL
     * The base URL of the service.
     *
     * @param executorService
     * The executor service that will be used to execute requests.
     *
     * @param dispatcher
     * The dispatcher that will be used to dispatch results.
     */
    public WebRPCService(URL baseURL, ExecutorService executorService, Dispatcher dispatcher) {
        if (baseURL == null) {
            throw new IllegalArgumentException();
        }

        if (executorService == null) {
            throw new IllegalArgumentException();
        }

        if (dispatcher == null) {
            throw new IllegalArgumentException();
        }

        this.baseURL = baseURL;
        this.executorService = executorService;
        this.dispatcher = dispatcher;
    }

    /**
     * Returns the service's base URL.
     */
    public URL getBaseURL() {
        return baseURL;
    }

    /**
     * Returns the executor service that is used to execute requests.
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Returns the dispatcher that is used to dispatch results.
     */
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * Invokes a web RPC service method.
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
    public Future<Object> invoke(String methodName, ResultHandler resultHandler) {
        return invoke(methodName, Collections.EMPTY_MAP, resultHandler);
    }

    /**
     * Invokes a web RPC service method.
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
    public Future<Object> invoke(String methodName, Map<String, Object> arguments, ResultHandler resultHandler) {
        if (methodName == null) {
            throw new IllegalArgumentException();
        }

        if (arguments == null) {
            throw new IllegalArgumentException();
        }

        if (resultHandler == null) {
            throw new IllegalArgumentException();
        }

        return executorService.submit(new InvocationCallback(methodName, arguments, resultHandler));
    }
}
