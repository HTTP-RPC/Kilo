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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class JSONDecoderTest {
    public interface Row {
        String getA();
        int getB();
        boolean isC();
    }

    private static class ListType implements ParameterizedType {
        Type[] actualTypeArguments;

        ListType(Type elementType) {
            actualTypeArguments = new Type[] {elementType};
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    @Test
    public void testString() throws IOException {
        assertEquals("abcdéfg", decode("\"abcdéfg\""));
        assertEquals("\b\f\r\n\t", decode("\"\\b\\f\\r\\n\\t\""));
        assertEquals("\0é", decode("\"\\u0000\\u00E9\""));
    }

    @Test
    public void testUnterminatedString() {
        assertThrows(IOException.class, () -> decode("\"abc"));
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
    public void testRowArray() throws IOException {
        var expected = BeanAdapter.coerceList(listOf(
            mapOf(
                entry("a", "hello"),
                entry("b", 123),
                entry("c", true)
            ),
            mapOf(
                entry("a", "goodbye"),
                entry("b", 456),
                entry("c", false)
            )
        ), Row.class);

        var text = "[{\"a\": \"hello\", \"b\": 123, \"c\": true}, {\"a\": \"goodbye\", \"b\": 456, \"c\": false}]";

        var actual = decode(text, () -> new JSONDecoder(new ListType(Row.class)));

        assertEquals(expected, actual);
    }

    @Test
    public void testStringArray() throws IOException {
        var expected = listOf("1", "2", "3");

        var text = "[1, 2, 3]";

        var actual = decode(text, () -> new JSONDecoder(new ListType(String.class)));

        assertEquals(expected, actual);
    }

    @Test
    public void testNestedArray() throws IOException {
        var expected = listOf(
            listOf(1),
            listOf(2, 3),
            listOf(4, 5, 6)
        );

        var text = "[[1], [2, 3], [4, 5, 6]]";

        var actual = decode(text, () -> new JSONDecoder(new ListType(new ListType(Integer.class))));

        assertEquals(expected, actual);
    }

    @Test
    public void testUnterminatedArray() {
        assertThrows(IOException.class, () -> decode("[1, 2, 3"));
        assertThrows(IOException.class, () -> decode("[1, 2, 3, "));
    }

    @Test
    public void testInvalidArray() {
        assertThrows(UnsupportedOperationException.class, () -> decode("\"abc\"", () -> new JSONDecoder(new ListType(Double.class))));
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
    public void testUnterminatedObject() {
        assertThrows(IOException.class, () -> decode("{\"a\": 1, \"b\": 2, \"c\": 3"));
        assertThrows(IOException.class, () -> decode("{\"a\": 1, \"b\": 2, \"c\": 3, "));
    }

    @Test
    public void testInvalidKey() {
        assertThrows(IOException.class, () -> decode("{a: 1}"));
    }

    @Test
    public void testMissingColon() {
        assertThrows(IOException.class, () -> decode("{\"a\"}"));
    }

    @Test
    public void testInvalidCharacters() {
        assertThrows(IOException.class, () -> decode("xyz"));
    }

    private static Object decode(String text) throws IOException {
        return decode(text, JSONDecoder::new);
    }

    private static Object decode(String text, Supplier<JSONDecoder> factory) throws IOException {
        var jsonDecoder = factory.get();

        return jsonDecoder.read(new StringReader(text));
    }
}