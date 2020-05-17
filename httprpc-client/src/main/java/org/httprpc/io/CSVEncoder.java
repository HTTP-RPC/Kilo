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

import org.httprpc.beans.BeanAdapter;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * CSV encoder.
 */
public class CSVEncoder extends Encoder<Iterable<? extends Map<String, ?>>> {
    /**
     * Class representing a column.
     */
    public static final class Column {
        private String key;
        private String label;

        /**
         * Constructs a new column.
         *
         * @param key
         * The column key.
         */
        public Column(String key) {
            this(key, key);
        }

        /**
         * Constructs a new column.
         *
         * @param key
         * The column key.
         *
         * @param label
         * The column label.
         */
        public Column(String key, String label) {
            this.key = key;
            this.label = label;
        }

        /**
         * Returns the column key.
         *
         * @return
         * The column key.
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the column label.
         *
         * @return
         * The column label.
         */
        public String getLabel() {
            return label;
        }
    }

    private List<Column> columns;
    private char delimiter;

    private static final char DEFAULT_DELIMITER = ',';

    /**
     * Constructs a new CSV encoder.
     *
     * @param columns
     * The output columns.
     */
    public CSVEncoder(List<Column> columns) {
        this(columns, DEFAULT_DELIMITER);
    }

    /**
     * Constructs a new CSV encoder.
     *
     * @param columns
     * The output columns.
     *
     * @param delimiter
     * The character to use as a field delimiter.
     */
    public CSVEncoder(List<Column> columns, char delimiter) {
        super(StandardCharsets.ISO_8859_1);

        if (columns == null) {
            throw new IllegalArgumentException();
        }

        this.columns = columns;
        this.delimiter = delimiter;
    }

    @Override
    public void write(Iterable<? extends Map<String, ?>> values, Writer writer) throws IOException {
        writer = new BufferedWriter(writer);

        int i = 0;

        for (Column column : columns) {
            if (column == null) {
                continue;
            }

            if (i > 0) {
                writer.write(delimiter);
            }

            encode(column.getLabel(), writer);

            i++;
        }

        writer.write("\r\n");

        for (Map<String, ?> map : values) {
            i = 0;

            for (Column column : columns) {
                if (column == null) {
                    continue;
                }

                if (i > 0) {
                    writer.write(delimiter);
                }

                encode(BeanAdapter.valueAt(map, column.getKey()), writer);

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
