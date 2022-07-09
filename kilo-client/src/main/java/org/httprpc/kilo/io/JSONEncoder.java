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
import java.nio.charset.StandardCharsets;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JSON encoder.
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
     * <code>true</code> if the encoded output should be compact;
     * <code>false</code>, otherwise.
     */
    public JSONEncoder(boolean compact) {
        super(StandardCharsets.UTF_8);

        this.compact = compact;
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

    private void encode(Object value, Writer writer) throws IOException {
        if (value instanceof CharSequence) {
            CharSequence text = (CharSequence)value;

            writer.write("\"");

            for (int i = 0, n = text.length(); i < n; i++) {
                char c = text.charAt(i);

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
        } else if (value instanceof Enum<?>) {
            encode(value.toString(), writer);
        } else if (value instanceof Date) {
            encode(((Date)value).getTime(), writer);
        } else if (value instanceof TemporalAccessor || value instanceof TemporalAmount || value instanceof UUID || value instanceof URL) {
            encode(value.toString(), writer);
        } else if (value instanceof Iterable<?>) {
            writer.write("[");

            depth++;

            int i = 0;

            for (Object element : (Iterable<?>)value) {
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
        } else if (value instanceof Map<?, ?>) {
            writer.write("{");

            depth++;

            int i = 0;

            for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
                Object key = entry.getKey();

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
            writer.append(null);
        }
    }

    private void indent(Writer writer) throws IOException {
        for (int i = 0; i < depth; i++) {
            writer.write("  ");
        }
    }
}
