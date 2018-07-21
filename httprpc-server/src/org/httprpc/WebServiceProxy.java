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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.httprpc.beans.BeanAdapter;

/**
 * Web service proxy class.
 */
public class WebServiceProxy {
    /**
     * Enumeration representing a request encoding.
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
         * Encodes a request to an output stream.
         *
         * @param outputStream
         * The output stream to write to.
         *
         * @throws IOException
         * If an exception occurs.
         */
        public void encodeRequest(OutputStream outputStream) throws IOException;
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
         * The content type.
         *
         * @throws IOException
         * If an exception occurs.
         *
         * @return
         * The decoded value.
         */
        public T decodeResponse(InputStream inputStream, String contentType) throws IOException;
    }

    private String method;
    private URL url;

    private Encoding encoding = Encoding.APPLICATION_X_WWW_FORM_URLENCODED;

    private LinkedHashMap<String, String> headers = new LinkedHashMap<>();
    private LinkedHashMap<String, String> arguments = new LinkedHashMap<>();

    private int connectTimeout = 0;
    private int readTimeout = 0;

    private String multipartBoundary = UUID.randomUUID().toString();

    private static final String UTF_8 = "UTF-8";
    private static final String CRLF = "\r\n";

    /**
     * Creates a new web service proxy.
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
        this.encoding = encoding;
    }

    /**
     * Returns the header map.
     *
     * @return
     * The header map.
     */
    public LinkedHashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the argument map.
     *
     * @return
     * The argument map.
     */
    public LinkedHashMap<String, String> getArguments() {
        return arguments;
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
        return invoke(null);
    }

    /**
     * Invokes the service method.
     *
     * @param <T>
     * The result type.
     *
     * @param resultType
     * The result type, or <tt>null</tt> for the default type.
     *
     * @return
     * The result of the operation.
     *
     * @throws IOException
     * If an exception occurs while executing the operation.
     */
    public <T> T invoke(Class<T> resultType) throws IOException {
        return invoke(new RequestHandler() {
            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                // TODO
            }
        }, resultType);
    }

    /**
     * Invokes the service method.
     *
     * @param <T>
     * The result type.
     *
     * @param requestHandler
     * The request handler.
     *
     * @param resultType
     * The result type, or <tt>null</tt> for the default type.
     *
     * @return
     * The result of the operation.
     *
     * @throws IOException
     * If an exception occurs while executing the operation.
     */
    @SuppressWarnings("unchecked")
    public <T> T invoke(RequestHandler requestHandler, Class<T> resultType) throws IOException {
        return invoke(requestHandler, new ResponseHandler<T>() {
            @Override
            public T decodeResponse(InputStream inputStream, String contentType) throws IOException {
                T result;
                if (contentType.startsWith("application/json")) {
                    JSONDecoder decoder = new JSONDecoder();

                    Object value = decoder.readValue(inputStream);

                    if (resultType == null) {
                        result = (T)value;
                    } else {
                        result = BeanAdapter.adapt(value, resultType);
                    }
                } else {
                    result = null;
                }

                return result;
            }
        });
    }

    /**
     * Invokes the service method.
     *
     * @param <T>
     * The result type.
     *
     * @param requestHandler
     * The request handler.
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
    public <T> T invoke(RequestHandler requestHandler, ResponseHandler<T> responseHandler) throws IOException {
        if (requestHandler == null) {
            throw new IllegalArgumentException();
        }

        if (responseHandler == null) {
            throw new IllegalArgumentException();
        }

        // TODO Execute the request
        return null;
    }
}
