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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class CSVEncoderTest {
    public static class Row {
        private String a;
        private int b;
        private boolean c;

        public Row(String a, int b, boolean c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public String getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        public boolean isC() {
            return c;
        }
    }

    @Test
    public void testMaps() throws IOException {
        var rows = listOf(
            mapOf(
                entry("a", "A,B,\"C\" "),
                entry("b", 1),
                entry("c", 2.0),
                entry("d", true),
                entry("e", new Date(0))
            ),
            mapOf(
                entry("a", " D\r\nÉ\r\nF\r\n"),
                entry("b", 2),
                entry("c", 4.0)
            )
        );

        var csvEncoder = new CSVEncoder(listOf("a", "b", "c", "d", "e"));

        csvEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getPackageName() + ".csv"));

        var writer = new StringWriter();

        csvEncoder.write(rows, writer);

        var expected = "\"a\",\"b\",\"c\",\"D\",\"É\"\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true,0\r\n"
            + "\" D\r\nÉ\r\nF\r\n\",2,4.0,,\r\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    public void testBeans() throws IOException {
        var rows = listOf(
            new Row("hello", 123, true),
            new Row("goodbye", 456, false)
        );

        var csvEncoder = new CSVEncoder(listOf("a", "b", "c"));

        var writer = new StringWriter();

        csvEncoder.write(rows, writer);

        var expected = "\"a\",\"b\",\"c\"\r\n"
            + "\"hello\",123,true\r\n"
            + "\"goodbye\",456,false\r\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    public void testFormats() throws IOException {
        var rows = listOf(
            mapOf(
                entry("a", 1),
                entry("b", true),
                entry("c", new Date(0))
            )
        );

        var csvEncoder = new CSVEncoder(listOf("a", "b", "c"));

        var numberFormat = NumberFormat.getNumberInstance();

        numberFormat.setMinimumFractionDigits(2);

        csvEncoder.setNumberFormat(numberFormat);

        var booleanFormat = new Format() {
            @Override
            public StringBuffer format(Object object, StringBuffer stringBuffer, FieldPosition fieldPosition) {
                return stringBuffer.append((boolean)object ? "Y" : "N");
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                throw new UnsupportedOperationException();
            }
        };

        csvEncoder.setBooleanFormat(booleanFormat);

        var dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

        csvEncoder.setDateFormat(dateFormat);

        var writer = new StringWriter();

        csvEncoder.write(rows, writer);

        var expected = "\"a\",\"b\",\"c\"\r\n"
            + "\"" + numberFormat.format(1) + "\","
            + "\"" + booleanFormat.format(true) + "\","
            + "\"" + dateFormat.format(new Date(0)) + "\"\r\n";

        assertEquals(expected, writer.toString());
    }
}
