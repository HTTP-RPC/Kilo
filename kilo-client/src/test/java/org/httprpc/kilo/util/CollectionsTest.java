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
import java.util.HashSet;

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
    public void testListOfIterable() {
        var expected = listOf(1, 2, 3);
        var actual = listOf(listOf(1, 2, 3));

        assertEquals(expected, actual);
    }

    @Test
    public void testImmutableListOf() {
        var list = immutableListOf(1, 2, 3);

        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void testImmutableListOfIterable() {
        var list = immutableListOf(listOf(1, 2, 3));

        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void testEmptyListOf() {
        assertTrue(emptyListOf(Integer.class).isEmpty());
    }

    @Test
    public void testSortedListOf() {
        var sortedList = sortedListOf(3, 2, 1);

        assertEquals(listOf(1, 2, 3), sortedList);
    }

    @Test
    public void testSortedListOfIterable() {
        var sortedList = sortedListOf(listOf(3, 2, 1));

        assertEquals(listOf(1, 2, 3), sortedList);
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

        assertThrows(UnsupportedOperationException.class, () -> mapOf(
            entry("a", 1),
            entry("a", 2)
        ));
    }

    @Test
    public void testMapOfIterable() {
        var expected = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        var actual = mapOf(expected.entrySet());

        assertEquals(expected, actual);

        assertThrows(UnsupportedOperationException.class, () -> mapOf(
            entry("a", 1),
            entry("a", 2)
        ));
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
    public void testImmutableMapOfIterable() {
        var map = immutableMapOf(mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        ).entrySet());

        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
    }

    @Test
    public void testImmutableMapOfMap() {
        var map = immutableMapOf(mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        ));

        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
    }

    @Test
    public void testEmptyMapOf() {
        assertTrue(emptyMapOf(String.class, Integer.class).isEmpty());
    }

    @Test
    public void testSortedMapOf() {
        var sortedMap = sortedMapOf(
            entry("c", 3),
            entry("b", 2),
            entry("a", 1)
        );

        assertEquals(listOf(1, 2, 3), new ArrayList<>(sortedMap.values()));

        assertThrows(UnsupportedOperationException.class, () -> mapOf(
            entry("a", 1),
            entry("a", 2)
        ));
    }

    @Test
    public void testSortedMapOfIterable() {
        var sortedMap = sortedMapOf(mapOf(
            entry("c", 3),
            entry("b", 2),
            entry("a", 1)
        ).entrySet());

        assertEquals(listOf(1, 2, 3), new ArrayList<>(sortedMap.values()));

        assertThrows(UnsupportedOperationException.class, () -> mapOf(
            entry("a", 1),
            entry("a", 2)
        ));
    }

    @Test
    public void testSetOf() {
        var expected = new HashSet<Integer>();

        expected.add(1);
        expected.add(2);
        expected.add(3);

        var actual = setOf(1, 2, 3);

        assertEquals(expected, actual);

        actual.remove(2);

        assertEquals(2, actual.size());
    }

    @Test
    public void testSetOfIterable() {
        var expected = setOf(1, 2, 3);
        var actual = setOf(setOf(1, 2, 3));

        assertEquals(expected, actual);
    }

    @Test
    public void testImmutableSetOf() {
        var set = immutableSetOf(1, 2, 3);

        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void testImmutableSetOfIterable() {
        var set = immutableSetOf(setOf(1, 2, 3));

        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void testEmptySetOf() {
        assertTrue(emptySetOf(String.class).isEmpty());
    }

    @Test
    public void testSortedSetOf() {
        var sortedSet = sortedSetOf(3, 2, 1);

        assertEquals(listOf(1, 2, 3), new ArrayList<>(sortedSet));
    }

    @Test
    public void testSortedSetOfIterable() {
        var sortedSet = sortedSetOf(setOf(3, 2, 1));

        assertEquals(listOf(1, 2, 3), new ArrayList<>(sortedSet));
    }

    @Test
    public void testSynchronizedListOf() {
        assertNotNull(synchronizedListOf(listOf()));
    }

    @Test
    public void testSynchronizedMapOf() {
        assertNotNull(synchronizedMapOf(mapOf()));
    }

    @Test
    public void testSynchronizedSetOf() {
        assertNotNull(synchronizedSetOf(setOf()));
    }

    @Test
    public void testIndexWhere() {
        var strings = listOf("a", "bc", "def");

        var i = indexWhere(strings, value -> value.length() == 3); // 2

        assertEquals(2, i);

        var j = indexWhere(strings, String::isEmpty); // -1

        assertEquals(-1, j);
    }

    @Test
    public void testLastIndexWhere() {
        var strings = listOf("a", "bc", "def");

        var i = lastIndexWhere(strings, value -> value.length() == 1); // 0

        assertEquals(0, i);

        var j = lastIndexWhere(strings, String::isEmpty); // -1

        assertEquals(-1, j);
    }
}
