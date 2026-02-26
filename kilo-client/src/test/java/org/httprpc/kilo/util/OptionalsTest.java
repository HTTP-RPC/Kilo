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

package org.httprpc.kilo.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.httprpc.kilo.util.Optionals.*;
import static org.junit.jupiter.api.Assertions.*;

public class OptionalsTest {
    @Test
    public void testCoalesce() {
        var result = coalesce(null, () -> 123); // 123

        assertEquals(123, result);

        assertEquals(result, Optional.ofNullable(null).orElse(123));

        assertNull(coalesce(null, () -> null));
    }

    @Test
    public void testMap() {
        var result = map("hello", String::length); // 5

        assertEquals(5, result);

        assertEquals(result, Optional.ofNullable("hello").map(String::length).orElse(null));

        assertNull(map(null, String::length));
    }

    @Test
    public void testPerform() {
        var stringBuilder = new StringBuilder("abc");

        perform("d", stringBuilder::append); // abcd

        assertEquals("abcd", stringBuilder.toString());

        Optional.ofNullable("e").ifPresent(stringBuilder::append);

        perform(null, stringBuilder::append);

        assertEquals("abcde", stringBuilder.toString());
    }

    @Test
    public void testCast() {
        var text = cast("abc", String.class); // abc

        assertNotNull(text);

        var number = cast("abc", Double.class); // null

        assertNull(number);
    }
}
