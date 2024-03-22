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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Decodes a sequence of map values from CSV.
 */
public class CSVDecoder extends Decoder<List<Map<String, String>>> {
    private static class Cursor implements Iterable<Map<String, String>> {
        List<String> keys;
        char delimiter;
        Reader reader;

        List<String> values = null;

        Cursor(List<String> keys, char delimiter, Reader reader) {
            this.keys = keys;
            this.delimiter = delimiter;
            this.reader = reader;
        }

        @Override
        public Iterator<Map<String, String>> iterator() {
            return new Iterator<>() {
                Boolean hasNext = null;

                @Override
                public boolean hasNext() {
                    if (hasNext == null) {
                        try {
                            values = readValues(reader, delimiter);
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
                        var key = keys.get(i);

                        if (key.isEmpty()) {
                            continue;
                        }

                        var value = values.get(i);

                        if (value.isEmpty()) {
                            continue;
                        }

                        row.put(key, value);
                    }

                    hasNext = null;

                    return row;
                }
            };
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
        this.delimiter = delimiter;
    }

    @Override
    public List<Map<String, String>> read(Reader reader) throws IOException {
        return StreamSupport.stream(iterate(reader).spliterator(), false).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Decodes a sequence of map values.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * A cursor over the contents of the input stream.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public Iterable<Map<String, String>> iterate(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }

        return read(new InputStreamReader(inputStream, getCharset()));
    }

    /**
     * Decodes a sequence of map values.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * A cursor over the contents of the character stream.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public Iterable<Map<String, String>> iterate(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        reader = new BufferedReader(reader);

        var keys = readValues(reader, delimiter);

        if (keys.isEmpty()) {
            throw new IOException("Missing header row.");
        }

        return new Cursor(keys, delimiter, reader);
    }

    private static List<String> readValues(Reader reader, char delimiter) throws IOException {
        List<String> values = new LinkedList<>();

        var valueBuilder = new StringBuilder();

        var c = reader.read();

        while (c != '\r' && c != '\n' && c != EOF) {
            valueBuilder.setLength(0);

            var quoted = false;

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

        return values;
    }
}
