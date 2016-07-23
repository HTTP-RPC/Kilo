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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * JSON decoder.
 */
public class JSONDecoder implements Decoder {
    private int c = EOF;
    private LinkedList<Object> collections = new LinkedList<>();

    private static final String UTF_8_ENCODING = "UTF-8";

    private static final String TRUE_KEYWORD = "true";
    private static final String FALSE_KEYWORD = "false";
    private static final String NULL_KEYWORD = "null";

    private static final int EOF = -1;

    @Override
    public <V> V readValue(InputStream inputStream) throws IOException {
        return readValue(new BufferedReader(new InputStreamReader(inputStream, Charset.forName(UTF_8_ENCODING))));
    }

    /**
     * Reads a value from a character stream.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * The decoded value.
     *
     * @throws IOException
     * If an exception occurs.
     *
     * @param <V>
     * The type of value read by the decoder.
     */
    @SuppressWarnings("unchecked")
    public <V> V readValue(Reader reader) throws IOException {
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
