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
import java.text.Format;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.mapOf;

/**
 * Encodes a sequence of map values to CSV.
 */
public class CSVEncoder extends Encoder<Iterable<? extends Map<String, ?>>> {
    private List<String> keys;
    private char delimiter;

    private Map<String, String> labels = mapOf();
    private Map<String, Format> formats = mapOf();

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The column keys.
     */
    public CSVEncoder(List<String> keys) {
        this(keys, ',');
    }

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The column keys.
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
     * Returns the column labels.
     *
     * @return
     * The column labels.
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Sets the column labels.
     *
     * @param labels
     * The column labels.
     */
    public void setLabels(Map<String, String> labels) {
        if (labels == null) {
            throw new IllegalArgumentException();
        }

        this.labels = labels;
    }

    /**
     * Returns the column formats.
     *
     * @return
     * The column formats.
     */
    public Map<String, Format> getFormats() {
        return formats;
    }

    /**
     * Sets the column formats.
     *
     * @param formats
     * The column formats.
     */
    public void setFormats(Map<String, Format> formats) {
        if (formats == null) {
            throw new IllegalArgumentException();
        }

        this.formats = formats;
    }

    @Override
    public void write(Iterable<? extends Map<String, ?>> values, Writer writer) throws IOException {
        if (values == null || writer == null) {
            throw new IllegalArgumentException();
        }

        writer = new BufferedWriter(writer);

        var i = 0;

        for (var key : keys) {
            if (key == null) {
                continue;
            }

            if (i > 0) {
                writer.write(delimiter);
            }

            var label = labels.get(key);

            if (label == null) {
                label = key;
            }

            encode(label, writer);

            i++;
        }

        writer.write("\r\n");

        for (var map : values) {
            i = 0;

            for (var key : keys) {
                if (key == null) {
                    continue;
                }

                if (i > 0) {
                    writer.write(delimiter);
                }

                var value = map.get(key);

                if (value != null) {
                    var format = formats.get(key);

                    if (format != null) {
                        value = format.format(value);
                    }
                }

                encode(value, writer);

                i++;
            }

            writer.write("\r\n");
        }

        writer.flush();
    }

    void encode(Object value, Writer writer) throws IOException {
        if (value == null) {
            writer.write("");
        } else if (value instanceof CharSequence text) {
            writer.write('"');

            for (int i = 0, n = text.length(); i < n; i++) {
                var c = text.charAt(i);

                if (c == '"') {
                    writer.append("\"\"");
                } else {
                    writer.append(c);
                }
            }

            writer.write('"');
        } else if (value instanceof Date date) {
            writer.write(String.valueOf(date.getTime()));
        } else {
            writer.write(value.toString());
        }
    }
}
