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

import org.httprpc.kilo.beans.BeanAdapter;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Decodes JSON content.
 */
public class JSONDecoder extends Decoder<Object> {
    private Type type;
    private Type elementType;

    private int c = EOF;

    private Deque<Object> containers = new LinkedList<>();

    private StringBuilder valueBuilder = new StringBuilder();

    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NULL = "null";

    /**
     * Constructs a new JSON decoder.
     */
    public JSONDecoder() {
        this(Object.class);
    }

    /**
     * Constructs a new JSON decoder.
     *
     * @param type
     * The result type.
     */
    public JSONDecoder(Type type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        this.type = type;

        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() == List.class) {
            elementType = parameterizedType.getActualTypeArguments()[0];
        } else {
            elementType = null;
        }
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

                if (elementType != null && containers.size() == 1 && containers.peek() instanceof List<?> list) {
                    ((List<Object>)list).set(list.size() - 1, BeanAdapter.toGenericType(value, elementType));
                }

                c = reader.read();
            } else if (c == ',') {
                c = reader.read();
            } else {
                var container = containers.peek();

                // If the current container is a map, read the key
                String key;
                if (container instanceof Map) {
                    if (c != '"') {
                        throw new IOException("Invalid key.");
                    }

                    key = readString(reader);

                    skipWhitespace(reader);

                    if (c != ':') {
                        throw new IOException("Missing colon.");
                    }

                    c = reader.read();

                    skipWhitespace(reader);
                } else {
                    key = null;
                }

                // Read the value
                if (c == '"') {
                    value = readString(reader);
                } else if (c == '-' || Character.isDigit(c)) {
                    value = readNumber(reader);
                } else if (c == TRUE.charAt(0)) {
                    readLiteral(reader, TRUE);

                    value = Boolean.TRUE;
                } else if (c == FALSE.charAt(0)) {
                    readLiteral(reader, FALSE);

                    value = Boolean.FALSE;
                } else if (c == NULL.charAt(0)) {
                    readLiteral(reader, NULL);

                    value = null;
                } else if (c == '[') {
                    value = new ArrayList<>();

                    containers.push(value);

                    c = reader.read();
                } else if (c == '{') {
                    value = new LinkedHashMap<>();

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
                        if (elementType != null && containers.size() == 1) {
                            value = BeanAdapter.toGenericType(value, elementType);
                        }

                        ((List<Object>)container).add(value);
                    }
                }
            }

            skipWhitespace(reader);
        }

        if (!containers.isEmpty()) {
            throw new IOException("Unterminated container.");
        }

        if (elementType != null) {
            if (!(value instanceof List<?>)) {
                throw new IOException("Value is not a list.");
            }

            return value;
        }

        return BeanAdapter.toGenericType(value, type);
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

    private void readLiteral(Reader reader, String literal) throws IOException {
        var n = literal.length();
        var i = 0;

        while (c != EOF && i < n) {
            if (literal.charAt(i) != c) {
                break;
            }

            c = reader.read();

            i++;
        }

        if (i < n) {
            throw new IOException("Invalid literal.");
        }
    }
}