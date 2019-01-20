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
import java.util.List;
import java.util.Map;

import org.httprpc.beans.BeanAdapter;

/**
 * Flat file encoder.
 */
public class FlatFileEncoder {
    /**
     * Class representing a field.
     */
    public static class Field extends FlatFileDecoder.Field {
        private String format;

        /**
         * Constructs a new field.
         *
         * @param key
         * The field key.
         *
         * @param length
         * The field length.
         */
        public Field(String key, int length) {
            this(key, length, "%s");
        }

        /**
         * Constructs a new field.
         *
         * @param key
         * The field key.
         *
         * @param length
         * The field length.
         *
         * @param format
         * The field format.
         */
        public Field(String key, int length, String format) {
            super(key, length);

            if (format == null) {
                throw new IllegalArgumentException();
            }

            this.format = format;
        }

        /**
         * Returns the field format.
         *
         * @return
         * The field format.
         */
        public String getFormat() {
            return format;
        }
    }

    private List<Field> fields;
    private String terminator;

    /**
     * Constructs a new flat file encoder.
     *
     * @param fields
     * The output fields.
     */
    public FlatFileEncoder(List<Field> fields) {
        this(fields, "\r\n");
    }

    /**
     * Constructs a new flat file encoder.
     *
     * @param fields
     * The output fields.
     *
     * @param terminator
     * The line terminator.
     */
    public FlatFileEncoder(List<Field> fields, String terminator) {
        if (fields == null) {
            throw new IllegalArgumentException();
        }

        if (terminator == null) {
            throw new IllegalArgumentException();
        }

        this.fields = fields;
        this.terminator = terminator;
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
        write(values, new OutputStreamWriter(outputStream, Charset.forName("ISO-8859-1")));
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

        for (Map<String, ?> map : values) {
            for (Field field : fields) {
                if (field == null) {
                    continue;
                }

                Object value = BeanAdapter.valueAt(map, field.getKey());

                String text = (value == null) ? "" : String.format(field.getFormat(), value);

                int length = text.length();

                for (int i = 0, n = field.getLength(); i < n; i++) {
                    writer.append(i < length ? text.charAt(i) : ' ');
                }
            }

            writer.write(terminator);
        }

        writer.flush();
    }
}
