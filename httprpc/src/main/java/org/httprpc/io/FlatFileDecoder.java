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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Flat file decoder.
 */
public class FlatFileDecoder {
    /**
     * Class representing a field.
     */
    public static class Field {
        private String key;
        private int length;

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
            if (key == null) {
                throw new IllegalArgumentException();
            }

            if (length < 0) {
                throw new IllegalArgumentException();
            }

            this.key = key;
            this.length = length;
        }

        /**
         * Returns the field key.
         *
         * @return
         * The field key.
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the field length.
         *
         * @return
         * The field length.
         */
        public int getLength() {
            return length;
        }
    }

    // Cursor
    private static class Cursor implements Iterable<Map<String, String>> {
        public Cursor(Reader reader, List<Field> fields) throws IOException {
            // TODO
        }

        @Override
        public Iterator<Map<String, String>> iterator() {
            // TODO
            return null;
        }
    }

    private List<Field> fields;

    /**
     * Constructs a new flat file decoder.
     *
     * @param fields
     * The input fields.
     */
    public FlatFileDecoder(List<Field> fields) {
        if (fields == null) {
            throw new IllegalArgumentException();
        }

        this.fields = fields;
    }

    /**
     * Reads a sequence of values from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * A cursor over the values in the input stream.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public Iterable<Map<String, String>> read(InputStream inputStream) throws IOException {
        return read(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));
    }

    /**
     * Reads a sequence of values from a character stream.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * A cursor over the values in the character stream.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public Iterable<Map<String, String>> read(Reader reader) throws IOException {
        return new Cursor(new BufferedReader(reader), fields);
    }
}
