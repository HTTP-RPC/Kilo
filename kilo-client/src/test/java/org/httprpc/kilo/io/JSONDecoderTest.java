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
        var jsonDecoder = new JSONDecoder();

        assertEquals("abcdéfg", jsonDecoder.read(new StringReader("\"abcdéfg\"")));
        assertEquals("\b\f\r\n\t", jsonDecoder.read(new StringReader("\"\\b\\f\\r\\n\\t\"")));
        assertEquals("é", jsonDecoder.read(new StringReader("\"\\u00E9\"")));
    }

    @Test
    public void testNumber() throws IOException {
        var jsonDecoder = new JSONDecoder();

        assertEquals(Integer.MAX_VALUE, jsonDecoder.read(new StringReader(String.valueOf(Integer.MAX_VALUE))));
        assertEquals(Long.MAX_VALUE, jsonDecoder.read(new StringReader(String.valueOf(Long.MAX_VALUE))));
        assertEquals(Double.MAX_VALUE, jsonDecoder.read(new StringReader(String.valueOf(Double.MAX_VALUE))));

        assertEquals(Integer.MIN_VALUE, jsonDecoder.read(new StringReader(String.valueOf(Integer.MIN_VALUE))));
        assertEquals(Long.MIN_VALUE, jsonDecoder.read(new StringReader(String.valueOf(Long.MIN_VALUE))));
        assertEquals(Double.MIN_VALUE, jsonDecoder.read(new StringReader(String.valueOf(Double.MIN_VALUE))));
    }

    @Test
    public void testBoolean() throws IOException {
        var jsonDecoder = new JSONDecoder();

        assertEquals(true, jsonDecoder.read(new StringReader(String.valueOf(true))));
        assertEquals(false, jsonDecoder.read(new StringReader(String.valueOf(false))));
    }

    @Test
    public void testNull() throws IOException {
        var jsonDecoder = new JSONDecoder();

        assertNull(jsonDecoder.read(new StringReader(String.valueOf((String)null))));
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

        var jsonDecoder = new JSONDecoder();

        var actual = jsonDecoder.read(new StringReader(text));

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

        var jsonDecoder = new JSONDecoder();

        var actual = jsonDecoder.read(new StringReader(text));

        assertEquals(expected, actual);
    }

    @Test
    public void testInvalidCharacters() {
        var jsonDecoder = new JSONDecoder();

        assertThrows(IOException.class, () -> jsonDecoder.read(new StringReader("xyz")));
    }
}