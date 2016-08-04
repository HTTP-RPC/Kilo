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

import org.junit.Assert;
import org.junit.Test;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

public class JSONDecoderTest {
    @Test
    public void testString() {
        Assert.assertEquals("abcdefg", decode("\"abcdefg\""));
    }

    @Test
    public void testNumber() {
        Assert.assertEquals(42L, decode("42"));
    }

    @Test
    public void testBoolean() {
        Assert.assertEquals(true, decode("true"));
        Assert.assertEquals(false, decode("false"));
    }

    @Test
    public void testArray() {
        Assert.assertEquals(listOf(
            "abc",
            123L,
            true,
            listOf(1L, 2L, 3.0),
            mapOf(entry("x", 1L), entry("y", 2L), entry("z", 3.0))
        ), decode("[\"abc\", 123, true, [1, 2, 3.0], {\"x\": 1, \"y\": 2, \"z\": 3.0}]"));
    }

    @Test
    public void testObject() {
        Assert.assertEquals(mapOf(
            entry("a", "abc"),
            entry("b", 123L),
            entry("c", true),
            entry("d", listOf(1L, 2L, 3.0)),
            entry("e", mapOf(entry("x", 1L), entry("y", 2L), entry("z", 3.0)))
        ), decode("{\"a\": \"abc\", \"b\": 123, \"c\": true, \"d\": [1, 2, 3.0], \"e\": {\"x\": 1, \"y\": 2, \"z\": 3.0}}"));
    }

    @SuppressWarnings("unchecked")
    private <V> V decode(String json) {
        JSONDecoder decoder = new JSONDecoder();

        V value;
        try {
            value = (V)decoder.readValue(new StringReader(json));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return value;
    }
}
