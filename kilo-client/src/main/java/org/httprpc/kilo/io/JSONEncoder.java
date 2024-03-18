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
import java.io.Writer;
import java.net.URL;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Encodes an object hierarchy to JSON.
 */
public class JSONEncoder extends Encoder<Object> {
    private boolean compact;

    private Map<Class<?>, Function<?, Object>> mappings = new HashMap<>();

    private int depth = 0;

    /**
     * Constructs a new JSON encoder.
     */
    public JSONEncoder() {
        this(false);
    }

    /**
     * Constructs a new JSON encoder.
     *
     * @param compact
     * {@code true} if the encoded output should be compact; {@code false},
     * otherwise.
     */
    public JSONEncoder(boolean compact) {
        this.compact = compact;

        map(Date.class, Date::getTime);
        map(Enum.class, Object::toString);
        map(TemporalAccessor.class, Object::toString);
        map(TemporalAmount.class, Object::toString);
        map(UUID.class, Object::toString);
        map(URL.class, Object::toString);
    }

    /**
     * Associates a mapping function with a type.
     *
     * @param <T>
     * The source type.

     * @param type
     * The source type.
     *
     * @param transform
     * The mapping function to apply.
     */
    public <T> void map(Class<T> type, Function<T, Object> transform) {
        if (type == null || transform == null) {
            throw new IllegalArgumentException();
        }

        mappings.put(type, transform);
    }

    @Override
    public void write(Object value, Writer writer) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException();
        }

        writer = new BufferedWriter(writer);

        encode(value, writer);

        writer.flush();
    }

    @SuppressWarnings("unchecked")
    void encode(Object value, Writer writer) throws IOException {
        if (value == null) {
            writer.append(null);
        } else if (value instanceof CharSequence text) {
            writer.write("\"");

            for (int i = 0, n = text.length(); i < n; i++) {
                var c = text.charAt(i);

                if (c == '"' || c == '\\') {
                    writer.write("\\" + c);
                } else if (c == '\b') {
                    writer.write("\\b");
                } else if (c == '\f') {
                    writer.write("\\f");
                } else if (c == '\n') {
                    writer.write("\\n");
                } else if (c == '\r') {
                    writer.write("\\r");
                } else if (c == '\t') {
                    writer.write("\\t");
                } else {
                    writer.write(c);
                }
            }

            writer.write("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof Iterable<?> iterable) {
            writer.write("[");

            depth++;

            var i = 0;

            for (var element : iterable) {
                if (i > 0) {
                    writer.write(",");
                }

                if (!compact) {
                    writer.write("\n");

                    indent(writer);
                }

                encode(element, writer);

                i++;
            }

            depth--;

            if (!compact) {
                writer.write("\n");

                indent(writer);
            }

            writer.write("]");
        } else if (value instanceof Map<?, ?> map) {
            writer.write("{");

            depth++;

            var i = 0;

            for (var entry : map.entrySet()) {
                var key = entry.getKey();

                if (key == null) {
                    continue;
                }

                if (i > 0) {
                    writer.write(",");
                }

                if (!compact) {
                    writer.write("\n");

                    indent(writer);
                }

                encode(key.toString(), writer);

                writer.write(":");

                if (!compact) {
                    writer.write(" ");
                }

                encode(entry.getValue(), writer);

                i++;
            }

            depth--;

            if (!compact) {
                writer.write("\n");

                indent(writer);
            }

            writer.write("}");
        } else {
            var type = value.getClass();

            for (var entry : mappings.entrySet()) {
                if (entry.getKey().isAssignableFrom(type)) {
                    var transform = (Function<Object, Object>)entry.getValue();

                    encode(transform.apply(value), writer);

                    return;
                }
            }

            throw new IllegalArgumentException("Unsupported type.");
        }
    }

    private void indent(Writer writer) throws IOException {
        for (var i = 0; i < depth; i++) {
            writer.write("  ");
        }
    }
}
