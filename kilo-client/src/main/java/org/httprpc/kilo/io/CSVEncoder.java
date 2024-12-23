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
import java.text.Format;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

/**
 * Encodes CSV content.
 */
public class CSVEncoder extends Encoder<Iterable<?>> {
    private List<String> keys;

    private ResourceBundle resourceBundle = null;

    private Map<String, Format> formats = new HashMap<>();

    private static final char DELIMITER = ',';

    /**
     * Constructs a new CSV encoder.
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
     * Returns the resource bundle.
     *
     * @return
     * The resource bundle, or {@code null} if a resource bundle has not been
     * set.
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Sets the resource bundle.
     *
     * @param resourceBundle
     * The resource bundle, or {@code null} for no resource bundle.
     */
    public void setResourceBundle(ResourceBundle resourceBundle) {
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
    public void write(Iterable<?> rows, Writer writer) throws IOException {
        if (rows == null || writer == null) {
            throw new IllegalArgumentException();
        }

        writer = new BufferedWriter(writer);

        try {
            encode(rows, writer);
        } finally {
            writer.flush();
        }
    }

    private void encode(Iterable<?> rows, Writer writer) throws IOException {
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
                heading = resourceBundle.getObject(key).toString();
            }

            encode(heading, writer);

            i++;
        }

        writer.write("\r\n");

        for (var row : rows) {
            var map = coalesce(cast(row, Map.class), new BeanAdapter(row));

            i = 0;

            for (var key : keys) {
                if (i > 0) {
                    writer.write(DELIMITER);
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
    }

    private void encode(Object value, Writer writer) throws IOException {
        if (value == null) {
            return;
        }

        if (value instanceof CharSequence text) {
            encode(text, writer);
        } else if (value instanceof Date date) {
            encode(date, writer);
        } else {
            writer.write(value.toString());
        }
    }

    private void encode(CharSequence text, Writer writer) throws IOException {
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
    }

    private void encode(Date date, Writer writer) throws IOException {
        writer.write(String.valueOf(date.getTime()));
    }
}
