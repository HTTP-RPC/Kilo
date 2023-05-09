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

package org.httprpc.kilo.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JSON decoder.
 */
public class JSONDecoder extends Decoder<Object> {
    private boolean sorted;

    private int c = EOF;

    private Deque<Object> containers = new LinkedList<>();

    private StringBuilder valueBuilder = new StringBuilder();

    private static final String TRUE_KEYWORD = "true";
    private static final String FALSE_KEYWORD = "false";
    private static final String NULL_KEYWORD = "null";

    /**
     * Constructs a new JSON decoder.
     */
    public JSONDecoder() {
        this(false);
    }

    /**
     * Constructs a new JSON decoder.
     *
     * @param sorted
     * {@code true} if the decoded output should be sorted by key;
     * {@code false}, otherwise.
     */
    public JSONDecoder(boolean sorted) {
        this.sorted = sorted;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object read(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        reader = new BufferedReader(reader);

        Object value = null;

        c = reader.read();

        skipWhitespace(reader);

        while (c != EOF) {
            if (c == ']' || c == '}') {
                value = containers.pop();

                c = reader.read();
            } else if (c == ',') {
                c = reader.read();
            } else {
                var container = containers.peek();

                // If the current container is a map, read the key
                String key;
                if (container instanceof Map<?, ?>) {
                    if (c != '"') {
                        throw new IOException("Invalid key.");
                    }

                    key = readString(reader);

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
                    value = readString(reader);
                } else if (c == '+' || c == '-' || Character.isDigit(c)) {
                    value = readNumber(reader);
                } else if (c == TRUE_KEYWORD.charAt(0)) {
                    if (!readKeyword(reader, TRUE_KEYWORD)) {
                        throw new IOException();
                    }

                    value = Boolean.TRUE;
                } else if (c == FALSE_KEYWORD.charAt(0)) {
                    if (!readKeyword(reader, FALSE_KEYWORD)) {
                        throw new IOException();
                    }

                    value = Boolean.FALSE;
                } else if (c == NULL_KEYWORD.charAt(0)) {
                    if (!readKeyword(reader, NULL_KEYWORD)) {
                        throw new IOException();
                    }

                    value = null;
                } else if (c == '[') {
                    value = new ArrayList<>();

                    containers.push(value);

                    c = reader.read();
                } else if (c == '{') {
                    if (sorted) {
                        value = new TreeMap<>();
                    } else {
                        value = new LinkedHashMap<>();
                    }

                    containers.push(value);

                    c = reader.read();
                } else {
                    throw new IOException(String.format("Unexpected character (0x%04X).", c));
                }

                // Add the value to the current container
                if (container != null) {
                    if (key != null) {
                        ((Map<String, Object>)container).put(key, value);
                    } else {
                        ((List<Object>)container).add(value);
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
        valueBuilder.setLength(0);

        // Move to the next character after the opening quotes
        c = reader.read();

        while (c != EOF && c != '"') {
            if (Character.isISOControl(c)) {
                throw new IOException("Illegal character.");
            }

            if (c == '\\') {
                c = reader.read();

                if (c != '"' && c != '\\' && c != '/') {
                    if (c == 'b') {
                        c = '\b';
                    } else if (c == 'f') {
                        c = '\f';
                    } else if (c == 'n') {
                        c = '\n';
                    } else if (c == 'r') {
                        c = '\r';
                    } else if (c == 't') {
                        c = '\t';
                    } else if (c == 'u') {
                        var characterBuilder = new StringBuilder();

                        while (c != EOF && characterBuilder.length() < 4) {
                            c = reader.read();

                            characterBuilder.append((char)c);
                        }

                        if (c == EOF) {
                            throw new IOException("Incomplete Unicode escape sequence.");
                        }

                        var unicodeValue = characterBuilder.toString();

                        c = (char)Integer.parseInt(unicodeValue, 16);
                    } else if (c == EOF) {
                        throw new IOException("Unterminated escape sequence.");
                    } else {
                        throw new IOException("Invalid escape sequence.");
                    }
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

    private Number readNumber(Reader reader) throws IOException {
        valueBuilder.setLength(0);

        var decimal = false;

        while (c != EOF && (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '-')) {
            valueBuilder.append((char)c);

            decimal |= (c == '.');

            c = reader.read();
        }

        Number number;
        if (decimal) {
            number = Double.parseDouble(valueBuilder.toString());
        } else {
            var value = Long.parseLong(valueBuilder.toString());

            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                number = value;
            } else {
                number = (int)value;
            }
        }

        return number;
    }

    private boolean readKeyword(Reader reader, String keyword) throws IOException {
        var n = keyword.length();
        var i = 0;

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