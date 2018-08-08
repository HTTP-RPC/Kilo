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
    public static class Cursor implements Iterable<Map<String, Object>> {
        private List<String> keys;

        private LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        private Cursor(List<String> keys, Reader reader) {
            this.keys = keys;
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
        return new Iterator<Map<String, Object>>() {
            @Override
            public boolean hasNext() {
                // TODO
                return false;
            }

            @Override
            public Map<String, Object> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                // TODO
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
                        private Iterator<Map<String, Object>> iterator = Cursor.this.iterator();

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
        // TODO Read keys from first line
        ArrayList<String> keys = new ArrayList<>();

        return new Cursor(keys, reader);
    }

    private static String readValue(Reader reader) {
        // TODO
        return null;
    }
}
