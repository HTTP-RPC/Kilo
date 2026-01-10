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
import java.io.Writer;
import java.util.Date;
import java.util.Map;

/**
 * Encodes JSON content.
 */
public class JSONEncoder extends Encoder<Object> {
    private boolean compact;

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
    }

    @Override
    public void write(Object value, Writer writer) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException();
        }

        writer = new BufferedWriter(writer);

        try {
            encode(BeanAdapter.adapt(value), writer);
        } finally {
            writer.flush();
        }
    }

    private void encode(Object value, Writer writer) throws IOException {
        if (value instanceof Date date) {
            value = date.getTime();
        }

        switch (value) {
            case null -> writer.append(null);
            case CharSequence text -> encode(text, writer);
            case Float number -> {
                if (number.isNaN() || number.isInfinite()) {
                    throw new IllegalArgumentException("Invalid float value.");
                }

                encode(number, writer);
            }
            case Double number -> {
                if (number.isNaN() || number.isInfinite()) {
                    throw new IllegalArgumentException("Invalid double value.");
                }

                encode(number, writer);
            }
            case Number number -> encode(number, writer);
            case Boolean flag -> encode(flag, writer);
            case Iterable<?> iterable -> encode(iterable, writer);
            case Map<?, ?> map -> encode(map, writer);
            default -> encode(value.toString(), writer);
        }
    }

    private void encode(CharSequence text, Writer writer) throws IOException {
        writer.write("\"");

        for (int i = 0, n = text.length(); i < n; i++) {
            var c = text.charAt(i);

            if (c == '"') {
                writer.write("\\\"");
            } else if (c == '\\') {
                writer.write("\\\\");
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
            } else if (Character.isISOControl(c)) {
                writer.write(String.format("\\u%04x", (int)c));
            } else {
                writer.write(c);
            }
        }

        writer.write("\"");
    }

    private void encode(Number number, Writer writer) throws IOException {
        writer.write(number.toString());
    }

    private void encode(Boolean flag, Writer writer) throws IOException {
        writer.write(flag.toString());
    }

    private void encode(Iterable<?> iterable, Writer writer) throws IOException {
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
    }

    private void encode(Map<?, ?> map, Writer writer) throws IOException {
        writer.write("{");

        depth++;

        var i = 0;

        for (var entry : map.entrySet()) {
            var key = entry.getKey();

            if (key == null) {
                throw new IllegalArgumentException("Invalid key.");
            }

            if (key instanceof Date date) {
                key = date.getTime();
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
    }

    private void indent(Writer writer) throws IOException {
        for (var i = 0; i < depth; i++) {
            writer.write("  ");
        }
    }
}
