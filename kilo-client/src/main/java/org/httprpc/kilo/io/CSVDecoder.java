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
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Decodes CSV content.
 */
public class CSVDecoder extends Decoder<List<String>> {
    private class RowIterator implements Iterator<List<String>> {
        Reader reader;

        List<String> next = null;

        RowIterator(Reader reader) {
            this.reader = new BufferedReader(reader);
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                try {
                    next = readRow(reader);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }

            return next != null && !next.isEmpty();
        }

        @Override
        public List<String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            try {
                return next;
            } finally {
                next = null;
            }
        }
    }

    private int rowSize = 0;

    private StringBuilder valueBuilder = new StringBuilder();

    @Override
    public List<String> read(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        return readRow(reader);
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
    public Iterable<List<String>> readAll(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }

        return readAll(new InputStreamReader(inputStream, getCharset()));
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
    public Iterable<List<String>> readAll(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        return () -> new RowIterator(reader);
    }

    private List<String> readRow(Reader reader) throws IOException {
        var row = new ArrayList<String>(rowSize);

        var c = reader.read();

        var quoted = false;

        while (c != EOF) {
            if (c == '"') {
                c = reader.read();

                if (!quoted || c != '"') {
                    quoted = !quoted;

                    continue;
                }
            }

            if (!quoted) {
                if (c == ',') {
                    row.add(valueBuilder.toString());

                    valueBuilder.setLength(0);

                    c = reader.read();

                    continue;
                }

                if (c == '\r' || c == '\n') {
                    break;
                }
            }

            valueBuilder.append((char)c);

            c = reader.read();
        }

        if (!valueBuilder.isEmpty()) {
            row.add(valueBuilder.toString());

            valueBuilder.setLength(0);
        }

        if (quoted) {
            throw new IOException("Unterminated string.");
        }

        if (c == '\r') {
            c = reader.read();

            if (c != '\n') {
                throw new IOException("Unterminated row.");
            }
        }

        rowSize = Math.max(row.size(), rowSize);

        return row;
    }
}
