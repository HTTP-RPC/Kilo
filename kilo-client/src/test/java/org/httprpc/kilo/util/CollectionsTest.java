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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

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
    public void testSetOf() {
        var expected = new HashSet<Integer>(3);

        expected.add(1);
        expected.add(2);
        expected.add(3);

        var actual = setOf(1, 2, 3);

        assertEquals(expected, actual);

        actual.remove(2);

        assertEquals(2, actual.size());
    }

    @Test
    public void testImmutableSetOf() {
        var set = immutableSetOf(1, 2, 3);

        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
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
    public void testEmptySetOf() {
        var set1 = java.util.Collections.<Integer>emptySet();
        var set2 = emptySetOf(Integer.class);

        assertTrue(set2.isEmpty());
        assertEquals(set1, set2);
    }
}
