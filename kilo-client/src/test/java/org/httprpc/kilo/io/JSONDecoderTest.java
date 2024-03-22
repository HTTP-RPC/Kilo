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
import java.io.StringReader;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JSONDecoderTest {
    @Test
    public void testString() throws IOException {
        assertEquals("abcdéfg", decode("\"abcdéfg\""));
        assertEquals("\b\f\r\n\t", decode("\"\\b\\f\\r\\n\\t\""));
        assertEquals("é", decode("\"\\u00E9\""));
    }

    @Test
    public void testNumber() throws IOException {
        assertEquals(Integer.MAX_VALUE, decode(String.valueOf(Integer.MAX_VALUE)));
        assertEquals(Long.MAX_VALUE, decode(String.valueOf(Long.MAX_VALUE)));
        assertEquals(Double.MAX_VALUE, decode(String.valueOf(Double.MAX_VALUE)));

        assertEquals(Integer.MIN_VALUE, decode(String.valueOf(Integer.MIN_VALUE)));
        assertEquals(Long.MIN_VALUE, decode(String.valueOf(Long.MIN_VALUE)));
        assertEquals(Double.MIN_VALUE, decode(String.valueOf(Double.MIN_VALUE)));
    }

    @Test
    public void testBoolean() throws IOException {
        assertEquals(true, decode(String.valueOf(true)));
        assertEquals(false, decode(String.valueOf(false)));

        assertThrows(IOException.class, () -> decode("tabc"));
        assertThrows(IOException.class, () -> decode("fxyz"));
    }

    @Test
    public void testNull() throws IOException {
        assertNull(decode(String.valueOf((String)null)));

        assertThrows(IOException.class, () -> decode("n123"));
    }

    @Test
    public void testArray() throws IOException {
        var expected = listOf(
            "abc",
            123,
            true,
            listOf(1, 2.0, 3.0),
            mapOf(entry("x", 1), entry("y", 2.0), entry("z", 3.0))
        );

        var text = "[\"abc\",\t123,,,  true,\n[1, 2.0, 3.0],\n{\"x\": 1, \"y\": 2.0, \"z\": 3.0}]";

        var actual = decode(text);

        assertEquals(expected, actual);
    }

    @Test
    public void testObject() throws IOException {
        var expected = mapOf(
            entry("a", "abc"),
            entry("b", 123),
            entry("c", true),
            entry("d", listOf(1, 2.0, 3.0)),
            entry("e", mapOf(entry("x", 1), entry("y", 2.0), entry("z", 3.0)))
        );

        var text = "{\"a\": \"abc\", \"b\":\t123,,,  \"c\": true,\n\"d\": [1, 2.0, 3.0],\n\"e\": {\"x\": 1, \"y\": 2.0, \"z\": 3.0}}";

        var actual = decode(text);

        assertEquals(expected, actual);
    }

    @Test
    public void testInvalidCharacters() {
        assertThrows(IOException.class, () -> decode("xyz"));
    }

    private static Object decode(String text) throws IOException {
        var jsonDecoder = new JSONDecoder();

        return jsonDecoder.read(new StringReader(text));
    }
}