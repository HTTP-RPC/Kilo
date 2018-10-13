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

package org.httprpc.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * JSON decoder.
 */
public class JSONDecoder {
    private int c = EOF;

    private LinkedList<Object> collections = new LinkedList<>();

    private StringBuilder valueBuilder = new StringBuilder();

    private static final String TRUE_KEYWORD = "true";
    private static final String FALSE_KEYWORD = "false";
    private static final String NULL_KEYWORD = "null";

    private static final int EOF = -1;

    /**
     * Reads a value from an input stream.
     *
     * @param <T>
     * The type of the value to return.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return The decoded value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public <T> T readValue(InputStream inputStream) throws IOException {
        return readValue(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    }

    /**
     * Reads a value from a character stream.
     *
     * @param <T>
     * The type of the value to return.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return The decoded value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader reader) throws IOException {
        reader = new BufferedReader(reader);

        Object value = null;

        c = reader.read();

        skipWhitespace(reader);

        while (c != EOF) {
            if (c == ']' || c == '}') {
                value = collections.pop();

                c = reader.read();
            } else if (c == ',') {
                c = reader.read();
            } else {
                Object collection = collections.peek();

                // If the current collection is a map, read the key
                String key;
                if (collection instanceof Map<?, ?>) {
                    if (c != '"') {
                        throw new IOException("Invalid key.");
                    }

                    key = decodeString(reader);

                    skipWhitespace(reader);

                    if (c != ':') {
                        throw new IOException("Missing key/value delimiter.");
                    }

                    c = reader.read();

                    skipWhitespace(reader);
                } else {
                    key = null;
                }

                // Read the value
                if (c == '"') {
                    value = decodeString(reader);
                } else if (c == '+' || c == '-' || Character.isDigit(c)) {
                    value = decodeNumber(reader);
                } else if (c == 't') {
                    if (!decodeKeyword(reader, TRUE_KEYWORD)) {
                        throw new IOException();
                    }

                    value = Boolean.TRUE;
                } else if (c == 'f') {
                    if (!decodeKeyword(reader, FALSE_KEYWORD)) {
                        throw new IOException();
                    }

                    value = Boolean.FALSE;
                } else if (c == 'n') {
                    if (!decodeKeyword(reader, NULL_KEYWORD)) {
                        throw new IOException();
                    }

                    value = null;
                } else if (c == '[') {
                    value = new ArrayList<>();

                    collections.push(value);

                    c = reader.read();
                } else if (c == '{') {
                    value = new LinkedHashMap<String, Object>();

                    collections.push(value);

                    c = reader.read();
                } else {
                    throw new IOException("Unexpected character.");
                }

                // Add the value to the current collection
                if (collection != null) {
                    if (key != null) {
                        ((Map<String, Object>)collection).put(key, value);
                    } else {
                        ((List<Object>)collection).add(value);
                    }
                }
            }

            skipWhitespace(reader);
        }

        return (T)value;
    }

    private void skipWhitespace(Reader reader) throws IOException {
        while (c != EOF && Character.isWhitespace(c)) {
            c = reader.read();
        }
    }

    private String decodeString(Reader reader) throws IOException {
        valueBuilder.setLength(0);

        // Move to the next character after the opening quotes
        c = reader.read();

        while (c != EOF && c != '"') {
            if (Character.isISOControl(c)) {
                throw new IOException("Illegal character.");
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
                    StringBuilder characterBuilder = new StringBuilder();

                    while (c != EOF && characterBuilder.length() < 4) {
                        c = reader.read();

                        characterBuilder.append((char)c);
                    }

                    if (c == EOF) {
                        throw new IOException("Invalid Unicode escape sequence.");
                    }

                    String unicodeValue = characterBuilder.toString();

                    c = (char)Integer.parseInt(unicodeValue, 16);
                } else if (c != '"' && c != '\\' && c != '/') {
                    throw new IOException("Unsupported escape sequence.");
                }
            }

            valueBuilder.append((char)c);

            c = reader.read();
        }

        if (c != '"') {
            throw new IOException("Unterminated string.");
        }

        // Move to the next character after the closing quotes
        c = reader.read();

        return valueBuilder.toString();
    }

    private Number decodeNumber(Reader reader) throws IOException {
        valueBuilder.setLength(0);

        boolean decimal = false;

        while (c != EOF && (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '-')) {
            valueBuilder.append((char)c);

            decimal |= (c == '.');

            c = reader.read();
        }

        Number number;
        if (decimal) {
            number = Double.valueOf(valueBuilder.toString());
        } else {
            number = Long.valueOf(valueBuilder.toString());
        }

        return number;
    }

    private boolean decodeKeyword(Reader reader, String keyword) throws IOException {
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