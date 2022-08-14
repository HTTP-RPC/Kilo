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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JSONDecoderTest {
    @Test
    public void testString() throws IOException {
        assertEquals("abcdéfg", decode("\"abcdéfg\"", String.class));
        assertEquals("\b\f\r\n\t", decode("\"\\b\\f\\r\\n\\t\"", String.class));
        assertEquals("é", decode("\"\\u00E9\"", String.class));
    }

    @Test
    public void testNumber() throws IOException {
        assertEquals(42, (int)decode("42", Integer.class));
        assertEquals((long)Integer.MAX_VALUE + 1, (long)decode(String.valueOf((long)Integer.MAX_VALUE + 1), Long.class));
        assertEquals(42.5, decode("42.5", Double.class), 0);

        assertEquals(-789, (int)decode("-789", Integer.class));
        assertEquals((long)Integer.MIN_VALUE - 1, (long)decode(String.valueOf((long)Integer.MIN_VALUE - 1), Long.class));
        assertEquals(-789.10, decode("-789.10", Double.class), 0);
    }

    @Test
    public void testBoolean() throws IOException {
        assertEquals(true, decode("true", Boolean.class));
        assertEquals(false, decode("false", Boolean.class));
    }

    @Test
    public void testNull() throws IOException {
        assertNull(decode("null", Object.class));
    }

    @Test
    public void testArray() throws IOException {
        List<?> expected = listOf(
            "abc",
            123,
            true,
            listOf(1, 2.0, 3.0),
            mapOf(entry("x", 1), entry("y", 2.0), entry("z", 3.0))
        );

        List<?> list = decode("[\"abc\",\t123,,,  true,\n[1, 2.0, 3.0],\n{\"x\": 1, \"y\": 2.0, \"z\": 3.0}]",
            List.class, Object.class);

        assertEquals(expected, list);
    }

    @Test
    public void testObject() throws IOException {
        Map<?, ?> expected = mapOf(
            entry("a", "abc"),
            entry("b", 123),
            entry("c", true),
            entry("d", listOf(1, 2.0, 3.0)),
            entry("e", mapOf(entry("x", 1), entry("y", 2.0), entry("z", 3.0)))
        );

        Map<?, ?> map = decode("{\"a\": \"abc\", \"b\":\t123,,,  \"c\": true,\n\"d\": [1, 2.0, 3.0],\n\"e\": {\"x\": 1, \"y\": 2.0, \"z\": 3.0}}",
            Map.class, String.class, Object.class);

        assertEquals(expected, map);
    }

    @Test
    public void testInvalidCharacters() {
        assertThrows(IOException.class, () -> decode("xyz", Object.class));
    }

    private static <T> T decode(String text, Class<T> rawType, Type... actualTypeArguments) throws IOException {
        var jsonDecoder = new JSONDecoder();

        return BeanAdapter.coerce(jsonDecoder.read(new StringReader(text)), rawType, actualTypeArguments);
    }
}