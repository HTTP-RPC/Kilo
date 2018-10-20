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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

/**
 * JSON encoder.
 */
public class JSONEncoder {
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
     * <tt>true</tt> if the encoded output should be compact; <tt>false</tt>,
     * otherwise.
     */
    public JSONEncoder(boolean compact) {
        this.compact = compact;
    }

    /**
     * Writes a value to an output stream.
     *
     * @param value
     * The value to encode.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, OutputStream outputStream) throws IOException {
        writeValue(value, new OutputStreamWriter(outputStream, Charset.forName("UTF-8")));
    }

    /**
     * Writes a value to a character stream.
     *
     * @param value
     * The value to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, Writer writer) throws IOException {
        writer = new BufferedWriter(writer);

        encodeValue(value, writer);

        writer.flush();
    }

    private void encodeValue(Object value, Writer writer) throws IOException {
        if (value instanceof CharSequence) {
            CharSequence text = (CharSequence)value;

            writer.append("\"");

            for (int i = 0, n = text.length(); i < n; i++) {
                char c = text.charAt(i);

                if (c == '"' || c == '\\') {
                    writer.append("\\" + c);
                } else if (c == '\b') {
                    writer.append("\\b");
                } else if (c == '\f') {
                    writer.append("\\f");
                } else if (c == '\n') {
                    writer.append("\\n");
                } else if (c == '\r') {
                    writer.append("\\r");
                } else if (c == '\t') {
                    writer.append("\\t");
                } else {
                    writer.append(c);
                }
            }

            writer.append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            writer.append(value.toString());
        } else if (value instanceof Enum<?>) {
            encodeValue(((Enum<?>)value).ordinal(), writer);
        } else if (value instanceof Date) {
            encodeValue(((Date)value).getTime(), writer);
        } else if (value instanceof LocalDate || value instanceof LocalTime || value instanceof LocalDateTime) {
            encodeValue(value.toString(), writer);
        } else if (value instanceof Iterable<?>) {
            writer.append("[");

            depth++;

            int i = 0;

            for (Object element : (Iterable<?>)value) {
                if (i > 0) {
                    writer.append(",");
                }

                if (!compact) {
                    writer.append("\n");

                    indent(writer);
                }

                encodeValue(element, writer);

                i++;
            }

            depth--;

            if (!compact) {
                writer.append("\n");

                indent(writer);
            }

            writer.append("]");
        } else if (value instanceof Map<?, ?>) {
            writer.append("{");

            depth++;

            int i = 0;

            for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
                Object key = entry.getKey();

                if (key == null) {
                    continue;
                }

                if (i > 0) {
                    writer.append(",");
                }

                if (!compact) {
                    writer.append("\n");

                    indent(writer);
                }

                encodeValue(key.toString(), writer);

                writer.append(": ");

                encodeValue(entry.getValue(), writer);

                i++;
            }

            depth--;

            if (!compact) {
                writer.append("\n");

                indent(writer);
            }

            writer.append("}");
        } else {
            writer.append(null);
        }
    }

    private void indent(Writer writer) throws IOException {
        for (int i = 0; i < depth; i++) {
            writer.append("  ");
        }
    }
}
