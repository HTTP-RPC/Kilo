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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * JSON encoder.
 */
public class JSONEncoder implements Encoder {
    private static final String UTF_8_ENCODING = "UTF-8";

    private static final String JSON_MIME_TYPE = "application/json";

    private int depth = 0;

    @Override
    public String getContentType() {
        return String.format("%s;charset=%s", JSON_MIME_TYPE, UTF_8_ENCODING);
    }

    @Override
    public void writeValue(Object value, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, Charset.forName(UTF_8_ENCODING));
        writeValue(value, writer);

        writer.flush();
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
        if (value == null) {
            writer.append(null);
        } else if (value instanceof CharSequence) {
            CharSequence string = (CharSequence)value;

            writer.append("\"");

            for (int i = 0, n = string.length(); i < n; i++) {
                char c = string.charAt(i);

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
            writer.append(String.valueOf(value));
        } else if (value instanceof List<?>) {
            List<?> list = (List<?>)value;

            try {
                writer.append("[");

                depth++;

                int i = 0;

                for (Object element : list) {
                    if (i > 0) {
                        writer.append(",");
                    }

                    writer.append("\n");

                    indent(writer);

                    writeValue(element, writer);

                    i++;
                }

                depth--;

                writer.append("\n");

                indent(writer);

                writer.append("]");
            } finally {
                if (list instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable)list).close();
                    } catch (Exception exception) {
                        throw new IOException(exception);
                    }
                }
            }
        } else if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>)value;

            try {
                writer.append("{");

                depth++;

                int i = 0;

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (i > 0) {
                        writer.append(",");
                    }

                    writer.append("\n");

                    Object key = entry.getKey();

                    if (!(key instanceof String)) {
                        throw new IOException("Invalid key type.");
                    }

                    indent(writer);

                    writer.append("\"" + key + "\": ");

                    writeValue(entry.getValue(), writer);

                    i++;
                }

                depth--;

                writer.append("\n");

                indent(writer);

                writer.append("}");
            } finally {
                if (map instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable)map).close();
                    } catch (Exception exception) {
                        throw new IOException(exception);
                    }
                }
            }
        } else {
            throw new IOException("Invalid value type.");
        }
    }

    private void indent(Writer writer) throws IOException {
        for (int i = 0; i < depth; i++) {
            writer.append("  ");
        }
    }
}
