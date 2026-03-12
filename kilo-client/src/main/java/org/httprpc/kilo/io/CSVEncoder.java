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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Encodes CSV content.
 */
public class CSVEncoder extends Encoder<Iterable<?>> {
    private Map<Class<?>, Function<Object, String>> formatters = new HashMap<>();

    /**
     * Associates a formatter with a type.
     *
     * @param <T>
     * The type to format.
     *
     * @param type
     * The type to format. The type must be final.
     *
     * @param formatter
     * The formatter to apply to instances of the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> void format(Class<T> type, Function<? super T, String> formatter) {
        if (type == null || formatter == null) {
            throw new IllegalArgumentException();
        }

        if ((type.getModifiers() & Modifier.FINAL) == 0) {
            throw new IllegalArgumentException();
        }

        formatters.put(type, (Function<Object, String>)formatter);
    }

    @Override
    public void write(Iterable<?> row, Writer writer) throws IOException {
        if (row == null || writer == null) {
            throw new IllegalArgumentException();
        }

        encode(row, writer);
    }

    /**
     * Encodes multiple rows to an output stream.
     *
     * @param rows
     * The rows to encode.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeAll(Iterable<? extends Iterable<?>> rows, OutputStream outputStream) throws IOException {
        if (rows == null || outputStream == null) {
            throw new IllegalArgumentException();
        }

        writeAll(rows, new OutputStreamWriter(outputStream, getCharset()));
    }

    /**
     * Encodes multiple rows to a character stream.
     *
     * @param rows
     * The rows to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeAll(Iterable<? extends Iterable<?>> rows, Writer writer) throws IOException {
        if (rows == null || writer == null) {
            throw new IllegalArgumentException();
        }

        var bufferedWriter = new BufferedWriter(writer);

        try {
            for (var row : rows) {
                encode(row, bufferedWriter);
            }
        } finally {
            bufferedWriter.flush();
        }
    }

    private void encode(Iterable<?> row, Writer writer) throws IOException {
        var i = 0;

        for (var value : row) {
            if (i > 0) {
                writer.write(',');
            }

            encode(value, writer);

            i++;
        }

        writer.write("\r\n");
    }

    private void encode(Object value, Writer writer) throws IOException {
        if (value == null) {
            return;
        }

        var formatter = formatters.get(value.getClass());

        if (formatter != null) {
            value = formatter.apply(value);
        }

        switch (value) {
            case CharSequence text -> encode(text, writer);
            case Number number -> encode(number, writer);
            case Date date -> encode(date.getTime(), writer);
            case Instant instant -> encode(instant.toEpochMilli(), writer);
            case Boolean flag -> encode(flag, writer);
            default -> encode(value.toString(), writer);
        }
    }

    private void encode(CharSequence text, Writer writer) throws IOException {
        writer.write('"');

        for (int i = 0, n = text.length(); i < n; i++) {
            var c = text.charAt(i);

            if (c == '"') {
                writer.append("\"\"");
            } else {
                writer.append(c);
            }
        }

        writer.write('"');
    }

    private void encode(Number number, Writer writer) throws IOException {
        writer.write(number.toString());
    }

    private void encode(Boolean flag, Writer writer) throws IOException {
        writer.write(flag.toString());
    }
}
