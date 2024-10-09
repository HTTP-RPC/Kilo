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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Encodes CSV content.
 */
public class CSVEncoder extends Encoder<Iterable<? extends Map<String, ?>>> {
    private List<String> keys;
    private ResourceBundle resourceBundle;

    private Map<String, Format> formats = new HashMap<>();

    private static final char DELIMITER = ',';

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The column keys.
     */
    public CSVEncoder(List<String> keys) {
        this(keys, null);
    }

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The column keys.
     *
     * @param resourceBundle
     * The resource bundle, or {@code null} for no resource bundle.
     */
    public CSVEncoder(List<String> keys, ResourceBundle resourceBundle) {
        if (keys == null) {
            throw new IllegalArgumentException();
        }

        this.keys = keys;
        this.resourceBundle = resourceBundle;
    }

    /**
     * Associates a format with a column.
     *
     * @param key
     * The column key.
     *
     * @param format
     * The format to apply.
     */
    public void format(String key, Format format) {
        if (key == null || format == null) {
            throw new IllegalArgumentException();
        }

        formats.put(key, format);
    }

    @Override
    public void write(Iterable<? extends Map<String, ?>> records, Writer writer) throws IOException {
        if (records == null || writer == null) {
            throw new IllegalArgumentException();
        }

        writer = new BufferedWriter(writer);

        try {
            var i = 0;

            for (var key : keys) {
                if (key == null) {
                    throw new IllegalStateException();
                }

                if (i > 0) {
                    writer.write(DELIMITER);
                }

                String heading;
                if (resourceBundle == null) {
                    heading = key;
                } else {
                    try {
                        heading = resourceBundle.getObject(key).toString();
                    } catch (MissingResourceException exception) {
                        heading = key;
                    }
                }

                encode(heading, writer);

                i++;
            }

            writer.write("\r\n");

            for (var record : records) {
                i = 0;

                for (var key : keys) {
                    if (i > 0) {
                        writer.write(DELIMITER);
                    }

                    var value = record.get(key);

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
        } finally {
            writer.flush();
        }
    }

    private void encode(Object value, Writer writer) throws IOException {
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
