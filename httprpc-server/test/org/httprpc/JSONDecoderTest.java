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

package org.httprpc;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class JSONDecoderTest extends AbstractTest {
    @Test
    public void testString() throws IOException {
        Assert.assertTrue(decode("\"abcdéfg\"").equals("abcdéfg"));
        Assert.assertTrue(decode("\"\\b\\f\\r\\n\\t\"").equals("\b\f\r\n\t"));
        Assert.assertTrue(decode("\"\\u00E9\"").equals("é"));
    }

    @Test
    public void testNumber() throws IOException {
        Assert.assertTrue(decode("42").equals(42));
        Assert.assertTrue(decode("42").equals(42L));
        Assert.assertTrue(decode("42").equals(42F));
        Assert.assertTrue(decode("42").equals(42.0));

        Assert.assertFalse(decode("42").equals(42.5));

        Assert.assertTrue(decode("123.0").equals(123));
        Assert.assertTrue(decode("123.0").equals(123L));
        Assert.assertTrue(decode("123.0").equals(123F));
        Assert.assertTrue(decode("123.0").equals(123.0));

        Assert.assertTrue(decode("123.456").equals(123.456));

        Assert.assertFalse(decode("123.456").equals(123));

        Assert.assertTrue(decode("-789").equals(-789));
        Assert.assertTrue(decode("-789").equals(-789L));
        Assert.assertTrue(decode("-789.0").equals(-789F));
        Assert.assertTrue(decode("-789.0").equals(-789.0));

        Assert.assertTrue(decode("-789.10").equals(-789.10));

        Assert.assertFalse(decode("-789.10").equals(-789));

        HashSet<Number> numbers = new HashSet<>();

        numbers.add(decode(String.valueOf(101)));

        Assert.assertTrue(numbers.contains(decode(String.valueOf(101))));
        Assert.assertTrue(numbers.contains(decode(String.valueOf(101L))));
        Assert.assertTrue(numbers.contains(decode(String.valueOf(101F))));
        Assert.assertTrue(numbers.contains(decode(String.valueOf(101.0))));

        numbers.add(decode(String.valueOf(202.5)));

        Assert.assertFalse(numbers.contains(decode(String.valueOf(202))));
        Assert.assertFalse(numbers.contains(decode(String.valueOf(202L))));

        Assert.assertTrue(numbers.contains(decode(String.valueOf(202.5F))));
        Assert.assertTrue(numbers.contains(decode(String.valueOf(202.5))));

        Date now = new Date();

        Assert.assertTrue(decode(Long.toString(now.getTime())).equals(now.getTime()));
    }

    @Test
    public void testBoolean() throws IOException {
        Assert.assertTrue(decode("true").equals(true));
        Assert.assertTrue(decode("false").equals(false));
    }

    @Test(expected=IOException.class)
    public void testInvalidCharacters() throws IOException {
        decode("xyz");
    }

    @Test
    public void testArray() throws IOException {
        Object value = decode("[\"abc\",\t123,,,  true,\n[1, 2.0, 3.0],\n{\"x\": 1, \"y\": 2.0, \"z\": 3.0}]");

        Assert.assertTrue(value.equals(listOf(
            "abc",
            123,
            true,
            listOf(1, 2L, 3.0),
            mapOf(entry("x", 1), entry("y", 2F), entry("z", 3.0))
        )));
    }

    @Test(expected=IOException.class)
    public void testMissingObjectCommas() throws IOException {
        decode("{a:1 b:2 c:3}");
    }

    @Test(expected=IOException.class)
    public void testMissingObjectClosingBracket() throws IOException {
        decode("{a:1, b:2, c:3");
    }

    @Test(expected=IOException.class)
    public void testWrongObjectClosingBracket() throws IOException {
        decode("{a:1, b:2, c:3]");
    }

    @Test(expected=IOException.class)
    public void testMissingArrayCommas() throws IOException {
        decode("[1 2 3]");
    }

    @Test(expected=IOException.class)
    public void testMissingArrayClosingBracket() throws IOException {
        decode("[1 2 3");
    }

    @Test(expected=IOException.class)
    public void testWrongArrayClosingBracket() throws IOException {
        decode("[1 2 3}");
    }

    @Test
    public void testObject() throws IOException {
        Object value = decode("{\"a\": \"abc\", \"b\":\t123,,,  \"c\": true,\n\"d\": [1, 2.0, 3.0],\n\"e\": {\"x\": 1, \"y\": 2.0, \"z\": 3.0}}");

        Assert.assertTrue(value.equals(mapOf(
            entry("a", "abc"),
            entry("b", 123),
            entry("c", true),
            entry("d", listOf(1, 2L, 3.0)),
            entry("e", mapOf(entry("x", 1), entry("y", 2F), entry("z", 3.0)))
        )));
    }

    @Test
    public void testValueAt() throws IOException {
        Map<String, ?> map = decode("{\"a\": {\"b\": {\"c\": 123}}}");

        Assert.assertTrue(valueAt(map, "a.b.c").equals(123));
        Assert.assertTrue(valueAt(map, "x.y.z") == null);
    }

    @Test
    public void testCoalesce() throws IOException {
        Map<String, ?> map = decode("{\"abc\": null}");

        Assert.assertTrue(coalesce(valueAt(map, "abc"), null, 123).equals(123));
        Assert.assertTrue(coalesce(valueAt(map, "xyz"), 456).equals(456));
    }

    private static <T> T decode(String text) throws IOException {
        JSONDecoder decoder = new JSONDecoder();

        return decoder.readValue(new StringReader(text));
    }

    @SuppressWarnings("unchecked")
    private static <V> V valueAt(Map<String, ?> root, String path) {
        if (root == null) {
            throw new IllegalArgumentException();
        }

        if (path == null) {
            throw new IllegalArgumentException();
        }

        Object value = root;

        String[] components = path.split("\\.");

        for (int i = 0; i < components.length; i++) {
            String component = components[i];

            if (value instanceof Map<?, ?>) {
                value = ((Map<?, ?>)value).get(component);
            } else {
                value = null;

                break;
            }
        }

        return (V)value;
    }

    @SafeVarargs
    private static <V> V coalesce(V... values) {
        V value = null;

        for (int i = 0; i < values.length; i++) {
            value = values[i];

            if (value != null) {
                break;
            }
        }

        return value;
    }
}