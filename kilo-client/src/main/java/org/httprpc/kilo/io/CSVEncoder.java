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

import org.httprpc.kilo.beans.BeanAdapter;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * Encodes CSV content.
 */
public class CSVEncoder extends Encoder<Iterable<?>> {
    private Collection<?> keys;

    private ResourceBundle resourceBundle = null;

    private Map<Class<?>, Function<Object, String>> formatters = new HashMap<>();

    private static final char DELIMITER = ',';

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The column keys.
     */
    public CSVEncoder(Collection<?> keys) {
        if (keys == null) {
            throw new IllegalArgumentException();
        }

        this.keys = keys;
    }

    /**
     * Returns the resource bundle.
     *
     * @return
     * The resource bundle, or {@code null} if a resource bundle has not been
     * set.
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * Sets the resource bundle.
     *
     * @param resourceBundle
     * The resource bundle, or {@code null} for no resource bundle.
     */
    public void setResourceBundle(ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

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
    public void write(Iterable<?> rows, Writer writer) throws IOException {
        if (rows == null || writer == null) {
            throw new IllegalArgumentException();
        }

        writer = new BufferedWriter(writer);

        try {
            encode(rows, writer);
        } finally {
            writer.flush();
        }
    }

    private void encode(Iterable<?> rows, Writer writer) throws IOException {
        var i = 0;

        for (var key : keys) {
            if (key == null) {
                throw new IllegalStateException("Missing key.");
            }

            if (i > 0) {
                writer.write(DELIMITER);
            }

            if (resourceBundle == null) {
                encode(key, writer);
            } else {
                encode(resourceBundle.getObject(key.toString()), writer);
            }

            i++;
        }

        writer.write("\r\n");

        for (var row : rows) {
            if (!(BeanAdapter.adapt(row) instanceof Map<?, ?> map)) {
                throw new IllegalArgumentException("Invalid row type.");
            }

            i = 0;

            for (var key : keys) {
                if (i > 0) {
                    writer.write(DELIMITER);
                }

                encode(map.get(key), writer);

                i++;
            }

            writer.write("\r\n");
        }
    }

    private void encode(Object value, Writer writer) throws IOException {
        if (value == null) {
            return;
        }

        if (value instanceof Date date) {
            value = date.toInstant();
        }

        var formatter = formatters.get(value.getClass());

        if (formatter != null) {
            value = formatter.apply(value);
        }

        switch (value) {
            case CharSequence text -> encode(text, writer);
            case Number number -> encode(number, writer);
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
