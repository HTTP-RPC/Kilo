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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Decodes CSV content.
 */
public class CSVDecoder extends Decoder<List<Map<String, String>>> {
    private class RowIterator implements Iterator<Map<String, String>> {
        Reader reader;

        RowIterator(Reader reader) {
            this.reader = reader;

            try {
                c = reader.read();

                while (c != EOF) {
                    keys.add(readValue(reader));

                    if (c == '\n') {
                        c = reader.read();

                        break;
                    }

                    c = reader.read();
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public boolean hasNext() {
            return c != EOF;
        }

        @Override
        public Map<String, String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            var next = new LinkedHashMap<String, String>();

            try {
                var n = keys.size();

                var i = 0;

                while (c != EOF) {
                    var value = readValue(reader);

                    if (i < n) {
                        next.put(keys.get(i), value);
                    }

                    if (c == '\n') {
                        c = reader.read();

                        break;
                    }

                    i++;

                    c = reader.read();
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            return next;
        }
    }

    private List<String> keys = new LinkedList<>();

    private int c = EOF;

    private StringBuilder valueBuilder = new StringBuilder();

    @Override
    public List<Map<String, String>> read(Reader reader) throws IOException {
        return listOf(iterate(reader));
    }

    /**
     * Reads multiple rows from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * The decoded rows.
     */
    public Iterable<Map<String, String>> iterate(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }

        return iterate(new InputStreamReader(inputStream, getCharset()));
    }

    /**
     * Reads multiple rows from a character stream.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * The decoded rows.
     */
    public Iterable<Map<String, String>> iterate(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        return () -> new RowIterator(new BufferedReader(reader));
    }

    private String readValue(Reader reader) throws IOException {
        valueBuilder.setLength(0);

        var quoted = false;

        while (c != EOF) {
            if (c == '"') {
                c = reader.read();

                if (!quoted || c != '"') {
                    quoted = !quoted;

                    continue;
                }
            }

            if ((c == ',' || c == '\r' || c == '\n') && !quoted) {
                break;
            }

            valueBuilder.append((char)c);

            c = reader.read();
        }

        if (quoted) {
            throw new IOException("Unterminated string.");
        }

        if (c == '\r') {
            c = reader.read();

            if (c != '\n') {
                throw new IOException("Improperly terminated row.");
            }
        }

        return valueBuilder.toString();
    }
}
