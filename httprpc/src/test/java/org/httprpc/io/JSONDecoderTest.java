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
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.httprpc.AbstractTest;
import org.httprpc.io.JSONDecoder;
import org.junit.Assert;
import org.junit.Test;

public class JSONDecoderTest extends AbstractTest {
    @Test
    public void testString() throws IOException {
        Assert.assertEquals("abcdéfg", decode("\"abcdéfg\""));
        Assert.assertEquals("\b\f\r\n\t", decode("\"\\b\\f\\r\\n\\t\""));
        Assert.assertEquals("é", decode("\"\\u00E9\""));
    }

    @Test
    public void testNumber() throws IOException {
        Assert.assertEquals(42L, (long)decode("42"));
        Assert.assertEquals(42.5, (double)decode("42.5"), 0);

        Assert.assertEquals(-789L, (long)decode("-789"));
        Assert.assertEquals(-789.10, (double)decode("-789.10"), 0);
    }

    @Test
    public void testBoolean() throws IOException {
        Assert.assertEquals(true, decode("true"));
        Assert.assertEquals(false, decode("false"));
    }

    @Test
    public void testArray() throws IOException {
        List<?> expected = listOf(
            "abc",
            123L,
            true,
            listOf(1L, 2.0, 3.0),
            mapOf(entry("x", 1L), entry("y", 2.0), entry("z", 3.0))
        );

        List<?> list = decode("[\"abc\",\t123,,,  true,\n[1, 2.0, 3.0],\n{\"x\": 1, \"y\": 2.0, \"z\": 3.0}]");

        Assert.assertEquals(expected, list);
    }

    @Test
    public void testObject() throws IOException {
        Map<String, ?> expected = mapOf(
            entry("a", "abc"),
            entry("b", 123L),
            entry("c", true),
            entry("d", listOf(1L, 2.0, 3.0)),
            entry("e", mapOf(entry("x", 1L), entry("y", 2.0), entry("z", 3.0)))
        );

        Map<String, ?> map = decode("{\"a\": \"abc\", \"b\":\t123,,,  \"c\": true,\n\"d\": [1, 2.0, 3.0],\n\"e\": {\"x\": 1, \"y\": 2.0, \"z\": 3.0}}");

        Assert.assertEquals(expected, map);
    }

    @Test(expected=IOException.class)
    public void testInvalidCharacters() throws IOException {
        decode("xyz");
    }

    @Test(expected=IOException.class)
    public void testMissingArrayCommas() throws IOException {
        decode("[1 2 3]");
    }

    @Test(expected=IOException.class)
    public void testMissingArrayClosingBracket() throws IOException {
        decode("[1 2 3  ");
    }

    @Test(expected=IOException.class)
    public void testWrongArrayClosingBracket() throws IOException {
        decode("[1 2 3}");
    }

    @Test(expected=IOException.class)
    public void testMissingObjectCommas() throws IOException {
        decode("{a:1 b:2 c:3}");
    }

    @Test(expected=IOException.class)
    public void testMissingObjectClosingBracket() throws IOException {
        decode("{a:1, b:2, c:3  ");
    }

    @Test(expected=IOException.class)
    public void testWrongObjectClosingBracket() throws IOException {
        decode("{a:1, b:2, c:3]");
    }

    private static <T> T decode(String text) throws IOException {
        JSONDecoder jsonDecoder = new JSONDecoder();

        return jsonDecoder.readValue(new StringReader(text));
    }
}