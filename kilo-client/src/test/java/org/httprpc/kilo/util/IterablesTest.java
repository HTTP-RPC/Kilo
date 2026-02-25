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

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

public class IterablesTest {
    @Test
    public void testFilter() {
        var values = listOf(1, 2, 3);

        var result = listOf(filter(values, value -> value < 3)); // 1, 2

        assertEquals(2, result.size());

        assertEquals(1, result.getFirst());
        assertEquals(2, result.getLast());
    }

    @Test
    public void testMapAll() {
        var values = listOf("1", "2", "3");

        var result = listOf(mapAll(values, Integer::valueOf)); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), result);
    }

    @Test
    public void testFirstOf() {
        var values = listOf(1, 2, 3);

        var result = firstOf(values); // 1

        assertEquals(1, result);

        assertNull(firstOf(listOf()));
    }

    @Test
    public void testIsEmpty() {
        var values = listOf();

        var result = isEmpty(values); // true

        assertTrue(result);

        assertFalse(isEmpty(listOf(1, 2, 3)));
    }

    @Test
    public void testToType() {
        var strings = listOf("1", "2", "3");

        var integers = listOf(mapAll(strings, toType(Integer.class))); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), integers);
    }
}
