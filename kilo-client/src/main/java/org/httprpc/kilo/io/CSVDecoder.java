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
import java.util.NoSuchElementException;

/**
 * Decodes CSV content.
 */
public class CSVDecoder extends Decoder<Iterable<String>> {
    private class ValueIterator implements Iterator<String> {
        Reader reader;

        boolean endOfLine = false;

        ValueIterator(Reader reader) {
            this.reader = reader;
        }

        @Override
        public boolean hasNext() {
            return c != EOF && !endOfLine;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            valueBuilder.setLength(0);

            try {
                if (c == ',') {
                    c = reader.read();
                }

                while (c != EOF && c != '\r' && c != '\n') {
                    if (c == ',') {
                        break;
                    }

                    valueBuilder.append((char)c);

                    c = reader.read();
                }

                if (c == '\r') {
                    endOfLine = true;

                    c = reader.read();
                }

                if (c == '\n') {
                    endOfLine = true;

                    c = reader.read();
                }
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }

            return valueBuilder.toString();
        }
    }

    private class RowIterator implements Iterator<Iterable<String>> {
        Reader reader;

        RowIterator(Reader reader) {
            this.reader = reader;

            try {
                c = reader.read();
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        @Override
        public boolean hasNext() {
            return c != EOF;
        }

        @Override
        public Iterable<String> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return () -> new ValueIterator(reader);
        }
    }

    private int c = EOF;

    private StringBuilder valueBuilder = new StringBuilder();

    @Override
    public Iterable<String> read(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        c = reader.read();

        return () -> new ValueIterator(reader);
    }

    /**
     * Reads multiple values from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * The decoded values.
     */
    public Iterable<Iterable<String>> readAll(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }

        return readAll(new InputStreamReader(inputStream, getCharset()));
    }

    /**
     * Reads multiple values from a character stream.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * The decoded values.
     */
    public Iterable<Iterable<String>> readAll(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        return () -> new RowIterator(new BufferedReader(reader));
    }
}
