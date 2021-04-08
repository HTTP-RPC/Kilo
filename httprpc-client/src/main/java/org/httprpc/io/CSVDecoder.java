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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * CSV decoder.
 */
public class CSVDecoder extends Decoder<Iterable<Map<String, String>>> {
    private static class Cursor implements Iterable<Map<String, String>> {
        Reader reader;
        char delimiter;

        StringBuilder valueBuilder = new StringBuilder();

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();

        Iterator<Map<String, String>> iterator = new Iterator<Map<String, String>>() {
            Boolean hasNext = null;

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

                Map<String, String> row = new LinkedHashMap<>();

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

        Cursor(Reader reader, char delimiter) throws IOException {
            this.reader = reader;
            this.delimiter = delimiter;

            readValues(reader, keys, delimiter);
        }

        void readValues(Reader reader, List<String> values, char delimiter) throws IOException {
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
    }

    private boolean cursor;
    private char delimiter;

    /**
     * Constructs a new CSV decoder.
     */
    public CSVDecoder() {
        this(false);
    }

    /**
     * Constructs a new CSV decoder.
     *
     * @param cursor
     * <code>true</code> if the results should be returned as a scrolling cursor;
     * <code>false</code>, otherwise.
     */
    public CSVDecoder(boolean cursor) {
        this(cursor, ',');
    }

    /**
     * Constructs a new CSV decoder.
     *
     * @param cursor
     * <code>true</code> if the results should be returned as a scrolling cursor;
     * <code>false</code>, otherwise.
     *
     * @param delimiter
     * The character to use as a field delimiter.
     */
    public CSVDecoder(boolean cursor, char delimiter) {
        super(StandardCharsets.ISO_8859_1);

        this.cursor = cursor;
        this.delimiter = delimiter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends Iterable<Map<String, String>>> U read(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        Cursor cursor = new Cursor(new BufferedReader(reader), delimiter);

        if (this.cursor) {
            return (U)cursor;
        } else {
            return (U)StreamSupport.stream(cursor.spliterator(), false).collect(Collectors.toList());
        }
    }
}
