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
import java.util.List;

/**
 * Decodes CSV content.
 */
public class CSVDecoder extends Decoder<List<String>> {
    private StringBuilder valueBuilder = new StringBuilder();

    @Override
    public List<String> read(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        return decode(reader);
    }

    private List<String> decode(Reader reader) throws IOException {
        var c = reader.read();

        if (c == EOF) {
            return null;
        }

        var row = new ArrayList<String>();

        while (c != '\r' && c != '\n' && c != EOF) {
            valueBuilder.setLength(0);

            var quoted = false;

            if (c == '"') {
                quoted = true;

                c = reader.read();
            }

            while ((quoted || (c != ',' && c != '\r' && c != '\n')) && c != EOF) {
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

            row.add(valueBuilder.toString());

            if (c == ',') {
                c = reader.read();
            }
        }

        if (c == '\r') {
            c = reader.read();

            if (c != '\n') {
                throw new IOException("Improperly terminated row.");
            }
        }

        return row;
    }

    /**
     * Decodes multiple rows.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * The decoded rows.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public List<List<String>> readAll(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }

        return readAll(new InputStreamReader(inputStream, getCharset()));
    }

    /**
     * Decodes multiple rows.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * The decoded rows.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public List<List<String>> readAll(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        reader = new BufferedReader(reader);

        var rows = new ArrayList<List<String>>();

        List<String> row;
        while ((row = decode(reader)) != null) {
            rows.add(row);
        }

        return rows;
    }
}
