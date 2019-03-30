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

package org.httprpc.io;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.httprpc.AbstractTest;
import org.httprpc.io.JSONEncoder;
import org.junit.Assert;
import org.junit.Test;

public class JSONEncoderTest extends AbstractTest {
    @Test
    public void testString() throws IOException {
        Assert.assertEquals("\"abcdéfg\"", encode("abcdéfg"));
        Assert.assertEquals("\"\\b\\f\\r\\n\\t\"", encode("\b\f\r\n\t"));
    }

    @Test
    public void testNumber() throws IOException {
        Assert.assertEquals("42", encode(42L));
        Assert.assertEquals("42.5", encode(42.5));

        Assert.assertEquals("-789", encode(-789));
        Assert.assertEquals("-789.1", encode(-789.10));
    }

    @Test
    public void testBoolean() throws IOException {
        Assert.assertEquals("true", encode(true));
        Assert.assertEquals("false", encode(false));
    }

    @Test
    public void testDate() throws IOException {
        Assert.assertEquals("0", encode(new Date(0)));
    }

    @Test
    public void testURL() throws IOException {
        Assert.assertEquals("\"http://localhost:8080\"", encode(new URL("http://localhost:8080")));
    }

    @Test
    public void testEnum() throws IOException {
        Assert.assertEquals("3", encode(DayOfWeek.THURSDAY));
    }

    @Test
    public void testArray() throws IOException {
        String expected = "[\n"
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

        List<?> list = listOf(
            "abc",
            123L,
            true,
            listOf(1L, 2.0, 3.0),
            mapOf(entry("x", 1L), entry("y", 2.0), entry("z", 3.0))
        );

        Assert.assertEquals(expected, encode(list));
    }

    @Test
    public void testObject() throws IOException {
        String expected = "{\n"
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

        Map<String, ?> map = mapOf(
            entry("a", "abc"),
            entry("b", 123L),
            entry("c", true),
            entry("d", listOf(1L, 2.0, 3.0)),
            entry("e", mapOf(entry("x", 1L), entry("y", 2.0), entry("z", 3.0)))
        );

        Assert.assertEquals(expected, encode(map));
    }

    @Test
    public void testCompact() throws IOException {
        String expected = "{\"a\":1,\"b\":2,\"c\":3}";

        Map<String, ?> map = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        Assert.assertEquals(expected, encode(map, true));
    }

    private static String encode(Object value) throws IOException {
        return encode(value, false);
    }

    private static String encode(Object value, boolean compact) throws IOException {
        StringWriter writer = new StringWriter();

        JSONEncoder jsonEncoder = new JSONEncoder(compact);

        jsonEncoder.write(value, writer);

        return writer.toString();
    }
}
