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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * CSV encoder.
 */
public class CSVEncoder {
    private List<String> keys;

    /**
     * Creates a new CSV encoder.
     *
     * @param keys
     * The column keys.
     */
    public CSVEncoder(List<String> keys) {
        if (keys == null) {
            throw new IllegalArgumentException();
        }

        this.keys = keys;
    }

    /**
     * Writes a sequence of values to an output stream.
     *
     * @param values
     * The values to encode.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValues(Iterable<? extends Map<String, ?>> values, OutputStream outputStream) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, Charset.forName("ISO-8859-1"));
        writeValues(values, writer);

        writer.flush();
    }

    /**
     * Writes a sequence of values to a character stream.
     *
     * @param values
     * The values to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValues(Iterable<? extends Map<String, ?>> values, Writer writer) throws IOException {
        int i = 0;

        for (String key : keys) {
            if (key == null) {
                continue;
            }

            if (i > 0) {
                writer.append(',');
            }

            writeValue(key, writer);

            i++;
        }

        writer.append("\r\n");

        for (Map<String, ?> map : values) {
            i = 0;

            for (String key : keys) {
                if (key == null) {
                    continue;
                }

                if (i > 0) {
                    writer.append(',');
                }

                Object value = valueAt(map, key);

                if (value != null) {
                    writeValue(value, writer);
                }

                i++;
            }

            writer.append("\r\n");
        }
    }

    private void writeValue(Object value, Writer writer) throws IOException {
        if (value instanceof CharSequence) {
            writer.write('"');
            writer.write(value.toString().replace("\"", "\"\""));
            writer.write('"');
        } else if (value instanceof Enum<?>) {
            writeValue(((Enum<?>)value).ordinal(), writer);
        } else if (value instanceof Date) {
            writeValue(((Date)value).getTime(), writer);
        } else {
            writer.append(value.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> V valueAt(Map<String, ?> root, String path) {
        Object value = root;

        String[] components = path.split("\\.");

        for (int i = 0; i < components.length; i++) {
            String component = components[i];

            if (value instanceof Map<?, ?>) {
                value = ((Map<?, ?>)value).get(component);
            } else {
                value = null;

                break;
            }
        }

        return (V)value;
    }
}
