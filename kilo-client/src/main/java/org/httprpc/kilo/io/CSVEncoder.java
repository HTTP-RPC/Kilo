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
import java.text.Format;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Optionals.*;

/**
 * Encodes CSV content.
 */
public class CSVEncoder extends Encoder<Iterable<?>> {
    private List<String> keys;

    private Format numberFormat = null;
    private Format booleanFormat = null;
    private Format dateFormat = null;

    private ResourceBundle resourceBundle = null;

    private static final char DELIMITER = ',';

    /**
     * Constructs a new CSV encoder.
     *
     * @param keys
     * The column keys.
     */
    public CSVEncoder(List<String> keys) {
        if (keys == null) {
            throw new IllegalArgumentException();
        }

        this.keys = keys;
    }

    /**
     * Returns the number format.
     *
     * @return
     * The number format, or {@code null} if a number format has not been set.
     */
    public Format getNumberFormat() {
        return numberFormat;
    }

    /**
     * Sets the number format.
     *
     * @param numberFormat
     * The number format, or {@code null} for no number format.
     */
    public void setNumberFormat(Format numberFormat) {
        this.numberFormat = numberFormat;
    }

    /**
     * Returns the boolean format.
     *
     * @return
     * The boolean format, or {@code null} if a boolean format has not been set.
     */
    public Format getBooleanFormat() {
        return booleanFormat;
    }

    /**
     * Sets the boolean format.
     *
     * @param booleanFormat
     * The boolean format, or {@code null} for no boolean format.
     */
    public void setBooleanFormat(Format booleanFormat) {
        this.booleanFormat = booleanFormat;
    }

    /**
     * Returns the date format.
     *
     * @return
     * The date format, or {@code null} if a date format has not been set.
     */
    public Format getDateFormat() {
        return dateFormat;
    }

    /**
     * Sets the date format.
     *
     * @param dateFormat
     * The date format, or {@code null} for no date format.
     */
    public void setDateFormat(Format dateFormat) {
        this.dateFormat = dateFormat;
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
                throw new IllegalStateException();
            }

            if (i > 0) {
                writer.write(DELIMITER);
            }

            String heading;
            if (resourceBundle == null) {
                heading = key;
            } else {
                heading = resourceBundle.getObject(key).toString();
            }

            encode(heading, writer);

            i++;
        }

        writer.write("\r\n");

        for (var row : rows) {
            var map = coalesce(cast(row, Map.class), new BeanAdapter(row));

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

        if (value instanceof CharSequence text) {
            encode(text, writer);
        } else if (value instanceof Number number) {
            encode(number, writer);
        } else if (value instanceof Boolean flag) {
            encode(flag, writer);
        } else if (value instanceof Date date) {
            encode(date, writer);
        } else {
            writer.write(value.toString());
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
        if (numberFormat != null) {
            encode(numberFormat.format(number), writer);
        } else {
            writer.write(number.toString());
        }
    }

    private void encode(Boolean flag, Writer writer) throws IOException {
        if (booleanFormat != null) {
            encode(booleanFormat.format(flag), writer);
        } else {
            writer.write(flag.toString());
        }
    }

    private void encode(Date date, Writer writer) throws IOException {
        if (dateFormat != null) {
            encode(dateFormat.format(date), writer);
        } else {
            writer.write(String.valueOf(date.getTime()));
        }
    }
}
