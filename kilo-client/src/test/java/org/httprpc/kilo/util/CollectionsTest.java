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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.emptyListOf;
import static org.httprpc.kilo.util.Collections.emptyMapOf;
import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.immutableListOf;
import static org.httprpc.kilo.util.Collections.immutableMapOf;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionsTest {
    @Test
    public void testListOf() {
        var expected = new ArrayList<Integer>(3);

        expected.add(1);
        expected.add(2);
        expected.add(3);

        var actual = listOf(1, 2, 3);

        assertEquals(expected, actual);

        actual.remove(2);

        assertEquals(2, actual.size());
    }

    @Test
    public void testImmutableListOf() {
        var list = immutableListOf(1, 2, 3);

        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void testMapOf() {
        var expected = new HashMap<String, Integer>();

        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);

        var actual = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        assertEquals(expected, actual);

        actual.remove("c");

        assertEquals(2, actual.size());
    }

    @Test
    public void testImmutableMapOf() {
        var map = immutableMapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
    }

    @Test
    public void testEmptyListOf() {
        var list1 = java.util.Collections.<Integer>emptyList();
        var list2 = emptyListOf(Integer.class);

        assertTrue(list2.isEmpty());
        assertEquals(list1, list2);
    }

    @Test
    public void testEmptyMapOf() {
        var map1 = java.util.Collections.<String, Integer>emptyMap();
        var map2 = emptyMapOf(String.class, Integer.class);

        assertTrue(map2.isEmpty());
        assertEquals(map1, map2);
    }

    @Test
    public void testFirstIndexWhere() {
        var list = listOf("a", "b", "c", "b", "d");

        var i = Collections.firstIndexWhere(list, element -> element.equals("b"));

        assertEquals(1, i);

        var j = Collections.firstIndexWhere(list, element -> element.equals("e"));

        assertEquals(-1, j);
    }

    @Test
    public void testLastIndexWhere() {
        var list = listOf("a", "b", "c", "b", "d");

        var i = Collections.lastIndexWhere(list, element -> element.equals("b"));

        assertEquals(3, i);

        var j = Collections.lastIndexWhere(list, element -> element.equals("e"));

        assertEquals(-1, j);
    }

    @Test
    public void testValueAt() {
        var map = mapOf(
            entry("a", mapOf(
                entry("b", mapOf(
                    entry("c", listOf(1, 2, 3))
                ))
            )),
            entry(4, "abc"),
            entry(true, "def")
        );

        var value = Collections.valueAt(map, "a", "b", "c", 1);

        assertEquals(2, value);

        assertEquals("abc", Collections.valueAt(map, 4));
        assertEquals("def", Collections.valueAt(map, true));

        assertEquals(map, Collections.valueAt(map));

        assertNull(Collections.valueAt(map, "a", "b", "d", 1));
        assertNull(Collections.valueAt(map, "e"));
        assertNull(Collections.valueAt(null));

        assertThrows(IndexOutOfBoundsException.class, () -> Collections.valueAt(map, "a", "b", "c", 4));
    }
}
