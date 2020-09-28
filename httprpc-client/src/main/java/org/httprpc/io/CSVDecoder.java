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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * CSV decoder.
 */
@SuppressWarnings("unchecked")
public class CSVDecoder extends Decoder {
    /**
     * CSV cursor.
     */
    public static class Cursor implements Iterable<Map<String, String>> {
        private Reader reader;
        private char delimiter;

        private StringBuilder valueBuilder = new StringBuilder();

        private ArrayList<String> keys = new ArrayList<>();
        private ArrayList<String> values = new ArrayList<>();

        private Iterator<Map<String, String>> iterator = new Iterator<Map<String, String>>() {
            private Boolean hasNext = null;

            @Override
            public boolean hasNext() {
                if (hasNext == null) {
                    try {
                        values.clear();

                        readValues(reader, values, delimiter);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }

                    hasNext = !values.isEmpty();
                }

                return hasNext;
            }

            @Override
            public Map<String, String> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                LinkedHashMap<String, String> row = new LinkedHashMap<>();

                for (int i = 0, n = Math.min(keys.size(), values.size()); i < n; i++) {
                    String key = keys.get(i);

                    if (key.isEmpty()) {
                        continue;
                    }

                    String value = values.get(i);

                    if (value.isEmpty()) {
                        continue;
                    }

                    row.put(key, value);
                }

                hasNext = null;

                return row;
            }
        };

        private static final int EOF = -1;

        private Cursor(Reader reader, char delimiter) throws IOException {
            this.reader = reader;
            this.delimiter = delimiter;

            readValues(reader, keys, delimiter);
        }

        private void readValues(Reader reader, ArrayList<String> values, char delimiter) throws IOException {
            int c = reader.read();

            while (c != '\r' && c != '\n' && c != EOF) {
                valueBuilder.setLength(0);

                boolean quoted = false;

                if (c == '"') {
                    quoted = true;

                    c = reader.read();
                }

                while ((quoted || (c != delimiter && c != '\r' && c != '\n')) && c != EOF) {
                    valueBuilder.append((char)c);

                    c = reader.read();

                    if (c == '"') {
                        c = reader.read();

                        if (c != '"') {
                            quoted = false;
                        }
                    }
                }

                if (quoted) {
                    throw new IOException("Unterminated quoted value.");
                }

                values.add(valueBuilder.toString());

                if (c == delimiter) {
                    c = reader.read();
                }
            }

            if (c == '\r') {
                c = reader.read();

                if (c != '\n') {
                    throw new IOException("Improperly terminated record.");
                }
            }
        }

        @Override
        public Iterator<Map<String, String>> iterator() {
            return iterator;
        }

        /**
         * Returns a stream over the results.
         *
         * @return
         * A stream over the results.
         */
        public Stream<Map<String, String>> stream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    private char delimiter;

    /**
     * Constructs a new CSV decoder.
     */
    public CSVDecoder() {
        this(',');
    }

    /**
     * Constructs a new CSV decoder.
     *
     * @param delimiter
     * The character to use as a field delimiter.
     */
    public CSVDecoder(char delimiter) {
        super(StandardCharsets.ISO_8859_1);

        this.delimiter = delimiter;
    }

    @Override
    public Cursor read(InputStream inputStream) throws IOException {
        return super.read(inputStream);
    }

    @Override
    public Cursor read(Reader reader) throws IOException {
        return new Cursor(new BufferedReader(reader), delimiter);
    }
}
