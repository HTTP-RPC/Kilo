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
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Date;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JSONEncoderTest {
    @Test
    public void testNull() throws IOException {
        assertEquals("null", encode(null));
    }

    @Test
    public void testString() throws IOException {
        assertEquals("\"abcdéfg\"", encode("abcdéfg"));
        assertEquals("\"\\b\\f\\r\\n\\t\"", encode("\b\f\r\n\t"));
    }

    @Test
    public void testNumber() throws IOException {
        assertEquals("42", encode(42L));
        assertEquals("42.5", encode(42.5));

        assertEquals("-789", encode(-789));
        assertEquals("-789.1", encode(-789.10));
    }

    @Test
    public void testBoolean() throws IOException {
        assertEquals("true", encode(true));
        assertEquals("false", encode(false));
    }

    @Test
    public void testEnum() throws IOException {
        assertEquals("\"MONDAY\"", encode(DayOfWeek.MONDAY));
    }

    @Test
    public void testDate() throws IOException {
        assertEquals("0", encode(new Date(0)));
    }

    @Test
    public void testTemporalAccessors() throws IOException {
        assertEquals("\"1970-01-01T00:00:00.001Z\"", encode(Instant.ofEpochMilli(1)));

        assertEquals("\"2018-06-28\"", encode(LocalDate.parse("2018-06-28")));
        assertEquals("\"10:45\"", encode(LocalTime.parse("10:45")));
        assertEquals("\"2018-06-28T10:45\"", encode(LocalDateTime.parse("2018-06-28T10:45")));
    }

    @Test
    public void testTemporalAmounts() throws IOException {
        assertEquals("\"PT2H30M\"", encode(Duration.parse("PT2H30M")));
        assertEquals("\"P3Y2M\"", encode(Period.parse("P3Y2M")));
    }

    @Test
    public void testUUID() throws IOException {
        var uuid = UUID.randomUUID();

        assertEquals(String.format("\"%s\"", uuid), encode(uuid));
    }

    @Test
    public void testURL() throws IOException {
        assertEquals("\"http://localhost:8080\"", encode(new URL("http://localhost:8080")));
    }

    @Test
    public void testArray() throws IOException {
        var expected = "[\n"
            + "  \"abc\",\n"
            + "  123,\n"
            + "  true,\n"
            + "  [\n"
            + "    1,\n"
            + "    2.0,\n"
            + "    3.0\n"
            + "  ],\n"
            + "  {\n"
            + "    \"x\": 1,\n"
            + "    \"y\": 2.0,\n"
            + "    \"z\": 3.0\n"
            + "  }\n"
            + "]";

        var list = listOf(
            "abc",
            123L,
            true,
            listOf(1L, 2.0, 3.0),
            mapOf(entry("x", 1L), entry("y", 2.0), entry("z", 3.0))
        );

        var actual = encode(list);

        assertEquals(expected, actual);
    }

    @Test
    public void testObject() throws IOException {
        var expected = "{\n"
            + "  \"a\": \"abc\",\n"
            + "  \"b\": 123,\n"
            + "  \"c\": true,\n"
            + "  \"d\": [\n"
            + "    1,\n"
            + "    2.0,\n"
            + "    3.0\n"
            + "  ],\n"
            + "  \"e\": {\n"
            + "    \"x\": 1,\n"
            + "    \"y\": 2.0,\n"
            + "    \"z\": 3.0\n"
            + "  }\n"
            + "}";

        var map = mapOf(
            entry("a", "abc"),
            entry("b", 123L),
            entry("c", true),
            entry("d", listOf(1L, 2.0, 3.0)),
            entry("e", mapOf(entry("x", 1L), entry("y", 2.0), entry("z", 3.0)))
        );

        var actual = encode(map);

        assertEquals(expected, actual);
    }

    @Test
    public void testInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> encode(new Object()));
    }

    @Test
    public void testCompact() throws IOException {
        var expected = "{\"a\":1,\"b\":2,\"c\":3}";

        var map = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        var actual = encode(map, true);

        assertEquals(expected, actual);
    }

    private static String encode(Object value) throws IOException {
        return encode(value, false);
    }

    private static String encode(Object value, boolean compact) throws IOException {
        var jsonEncoder = new JSONEncoder(compact);

        var writer = new StringWriter();

        jsonEncoder.write(value, writer);

        return writer.toString();
    }
}
