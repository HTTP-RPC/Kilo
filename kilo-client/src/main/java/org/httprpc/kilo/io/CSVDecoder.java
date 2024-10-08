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
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Decodes CSV content.
 */
public class CSVDecoder extends Decoder<List<Map<String, String>>> {
    private char delimiter = ',';

    /**
     * Returns the delimiter character.
     *
     * @return
     * The delimiter character.
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the delimiter character.
     *
     * @param delimiter
     * The delimiter character.
     */
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public List<Map<String, String>> read(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        reader = new BufferedReader(reader);

        var keys = readValues(reader);

        if (keys.isEmpty()) {
            throw new IOException("Missing header record.");
        }

        var records = new ArrayList<Map<String, String>>();

        List<String> values;
        while (!(values = readValues(reader)).isEmpty()) {
            var record = new LinkedHashMap<String, String>();

            for (int i = 0, n = Math.min(keys.size(), values.size()); i < n; i++) {
                var key = keys.get(i);

                if (key.isEmpty()) {
                    continue;
                }

                var value = values.get(i);

                if (value.isEmpty()) {
                    continue;
                }

                record.put(key, value);
            }

            records.add(record);
        }

        return records;
    }

    private List<String> readValues(Reader reader) throws IOException {
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
