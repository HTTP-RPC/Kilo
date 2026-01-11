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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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

    public record Record(
        String a,
        int b,
        boolean c
    ) {
    }

    @Test
    public void testMaps() throws IOException {
        var rows = listOf(
            mapOf(
                entry("a", "A,B,\"C\" "),
                entry("b", 1),
                entry("c", 2.0),
                entry("d", true)
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
            + "\"A,B,\"\"C\"\" \",1,2.0,true,\r\n"
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
    public void testRecords() throws IOException {
        var rows = listOf(
            new Record("hello", 123, true),
            new Record("goodbye", 456, false)
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
    public void testKeys() throws IOException {
        var date = LocalDate.now();

        var keys = listOf("a", date);

        var csvEncoder = new CSVEncoder(keys);

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

        csvEncoder.format(LocalDate.class, dateFormatter::format);
        csvEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getPackageName() + ".csv"));

        var rows = listOf(
            mapOf(
                entry("a", true),
                entry(date, 123)
            ),
            mapOf(
                entry("a", false),
                entry(date, 456)
            )
        );

        var writer = new StringWriter();

        csvEncoder.write(rows, writer);

        var expected = "\"a\",\"" + dateFormatter.format(date) + "\"\r\n"
            + "true,123\r\n"
            + "false,456\r\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    public void testFormat() throws IOException {
        var date = new Date();
        var localDateTime = LocalDateTime.now();

        var rows = listOf(
            mapOf(
                entry("a", true),
                entry("b", date),
                entry("c", localDateTime)
            )
        );

        var csvEncoder = new CSVEncoder(listOf("a", "b", "c"));

        csvEncoder.format(Boolean.class, flag -> flag ? "Y" : "N");
        csvEncoder.format(Instant.class, instant -> String.valueOf(instant.toEpochMilli()));

        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT);

        csvEncoder.format(LocalDateTime.class, dateTimeFormatter::format);

        var writer = new StringWriter();

        csvEncoder.write(rows, writer);

        var expected = "\"a\",\"b\",\"c\"\r\n"
            + "\"Y\","
            + "\"" + date.getTime() + "\","
            + "\"" + dateTimeFormatter.format(localDateTime) + "\"\r\n";

        assertEquals(expected, writer.toString());
    }
}
