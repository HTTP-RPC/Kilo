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

package org.httprpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.httprpc.beans.BeanAdapter;

/**
 * CSV decoder.
 */
public class CSVDecoder {
    /**
     * CSV cursor.
     */
    public static class Cursor implements Iterable<Map<String, String>> {
        private List<String> keys;
        private Reader reader;

        private ArrayList<String> values = new ArrayList<>();
        private LinkedHashMap<String, String> map = new LinkedHashMap<>();

        private Cursor(List<String> keys, Reader reader) {
            this.keys = keys;
            this.reader = reader;
        }

        @Override
        public Iterator<Map<String, String>> iterator() {
            return new Iterator<Map<String, String>>() {
                private Boolean hasNext = null;

                @Override
                public boolean hasNext() {
                    if (hasNext == null) {
                        try {
                            readRecord(reader, values);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }

                        hasNext = !values.isEmpty();
                    }

                    return hasNext.booleanValue();
                }

                @Override
                public Map<String, String> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    map.clear();

                    for (int i = 0, n = Math.min(keys.size(), values.size()); i < n; i++) {
                        map.put(keys.get(i), values.get(i));
                    }

                    hasNext = null;

                    return map;
                }
            };
        }

        /**
         * Adapts the cursor for typed access.
         *
         * @param <T>
         * The element type.
         *
         * @param elementType
         * The element type.
         *
         * @return
         * An iterable sequence of the given type.
         */
        public <T> Iterable<T> adapt(Class<T> elementType) {
            return new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        private Iterator<Map<String, String>> iterator = Cursor.this.iterator();

                        private T proxy = BeanAdapter.adapt(map, elementType);

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public T next() {
                            iterator.next();

                            return proxy;
                        }
                    };
                }
            };
        }
    }

    private static StringBuilder fieldBuilder = new StringBuilder();

    private static final int EOF = -1;

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
    public Cursor readValues(InputStream inputStream) throws IOException {
        return readValues(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));
    }

    /**
     * Reads a sequence of values from a character stream.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * A cursor over the values in the input stream.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public Cursor readValues(Reader reader) throws IOException {
        ArrayList<String> keys = new ArrayList<>();

        readRecord(reader, keys);

        return new Cursor(keys, reader);
    }

    private static void readRecord(Reader reader, ArrayList<String> fields) throws IOException {
        fields.clear();

        int c = reader.read();

        while (c != '\r' && c != EOF) {
            fieldBuilder.setLength(0);

            boolean quoted = false;

            if (c == '"') {
                quoted = true;

                c = reader.read();
            }

            while ((quoted || (c != ',' && c != '\r')) && c != EOF) {
                fieldBuilder.append((char)c);

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

            fields.add(fieldBuilder.toString());

            if (c == ',') {
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
}
