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
import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 * Flat file encoder.
 */
public class FlatFileEncoder {
    /**
     * Class representing a column in a flat file.
     */
    public static class Column {
        private String key;
        private String format;

        /**
         * Constructs a new column.
         *
         * @param key
         * The column key.
         *
         * @param format
         * The column format.
         */
        public Column(String key, String format) {
            if (key == null) {
                throw new IllegalArgumentException();
            }

            if (format == null) {
                throw new IllegalArgumentException();
            }

            this.key = key;
            this.format = format;
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
         * Returns the column format.
         *
         * @return
         * The column format.
         */
        public String getFormat() {
            return format;
        }
    }

    private List<Column> columns;

    /**
     * Constructs a new flat file encoder.
     *
     * @param columns
     * The output columns.
     */
    public FlatFileEncoder(List<Column> columns) {
        if (columns == null) {
            throw new IllegalArgumentException();
        }

        this.columns = columns;
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

        for (Column column : columns) {
            if (column == null) {
                continue;
            }

            if (i > 0) {
                writer.append(',');
            }

            writeValue(column, column.getKey(), writer);

            i++;
        }

        for (Map<String, ?> map : values) {
            i = 0;

            for (Column column : columns) {
                if (column == null) {
                    continue;
                }

                if (i > 0) {
                    writer.append(',');
                }

                Object value = map.get(column.getKey());

                if (value != null) {
                    writeValue(column, value, writer);
                }

                i++;
            }
        }
    }

    private void writeValue(Column column, Object value, Writer writer) throws IOException {
        if (value instanceof CharSequence) {
            // TODO
        } else if (value instanceof Enum<?>) {
            // TODO
        } else if (value instanceof Date) {
            // TODO
        } else {
            // TODO
        }
    }
}
