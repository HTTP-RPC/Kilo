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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.httprpc.beans.BeanAdapter;

/**
 * CSV encoder.
 */
public class CSVEncoder {
    private List<String> keys;
    private char delimiter;

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The output column keys.
     */
    public CSVEncoder(List<String> keys) {
        this(keys, ',');
    }

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The output column keys.
     *
     * @param delimiter
     * The character to use as a field delimiter.
     */
    public CSVEncoder(List<String> keys, char delimiter) {
        if (keys == null) {
            throw new IllegalArgumentException();
        }

        this.keys = keys;
        this.delimiter = delimiter;
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
    public void write(Iterable<? extends Map<String, ?>> values, OutputStream outputStream) throws IOException {
        write(values, new OutputStreamWriter(outputStream, StandardCharsets.ISO_8859_1));
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
    public void write(Iterable<? extends Map<String, ?>> values, Writer writer) throws IOException {
        writer = new BufferedWriter(writer);

        int i = 0;

        for (String key : keys) {
            if (key == null) {
                continue;
            }

            if (i > 0) {
                writer.write(delimiter);
            }

            encode(key, writer);

            i++;
        }

        writer.write("\r\n");

        for (Map<String, ?> map : values) {
            i = 0;

            for (String key : keys) {
                if (key == null) {
                    continue;
                }

                if (i > 0) {
                    writer.write(delimiter);
                }

                encode(BeanAdapter.valueAt(map, key), writer);

                i++;
            }

            writer.write("\r\n");
        }

        writer.flush();
    }

    private void encode(Object value, Writer writer) throws IOException {
        if (value instanceof CharSequence) {
            writer.write('"');

            String text = value.toString();

            for (int i = 0, n = text.length(); i < n; i++) {
                char c = text.charAt(i);

                if (c == '"') {
                    writer.append("\"\"");
                } else {
                    writer.append(c);
                }
            }

            writer.write('"');
        } else if (value instanceof Enum<?>) {
            encode(((Enum<?>)value).ordinal(), writer);
        } else if (value instanceof Date) {
            encode(((Date)value).getTime(), writer);
        } else {
            writer.write((value == null) ? "" : value.toString());
        }
    }
}
