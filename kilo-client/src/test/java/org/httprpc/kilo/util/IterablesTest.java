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

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

public class IterablesTest {
    @Test
    public void testFirstOf() {
        var values = listOf(1, 2, 3);

        var result = firstOf(values); // 1

        assertEquals(1, result);

        assertEquals(result, values.stream().findFirst().orElse(null));

        assertNull(firstOf(listOf()));
    }

    @Test
    public void testFilter() {
        var values = listOf(1, 2, 3);

        var result = listOf(filter(values, value -> value < 3)); // 1, 2

        assertEquals(2, result.size());

        assertEquals(1, result.getFirst());
        assertEquals(2, result.getLast());

        assertEquals(result, values.stream().filter(value -> value < 3).collect(Collectors.toList()));

        assertEquals(listOf(), listOf(filter(values, value -> value > 3)));
    }

    @Test
    public void testMapAllToList() {
        var values = listOf("a", "ab", "abc");

        var result = listOf(mapAll(values, String::length)); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), result);

        assertEquals(result, values.stream().map(String::length).collect(Collectors.toList()));
    }

    @Test
    public void testMapAllToMap() {
        var values = Arrays.asList(DayOfWeek.values());

        var result = mapOf(mapAll(values, value -> entry(value, value.ordinal())));

        var i = result.get(DayOfWeek.MONDAY); // 0

        assertEquals(0, i);

        assertEquals(result, values.stream().collect(Collectors.toMap(value -> value, Enum::ordinal)));
    }

    @Test
    public void testIndex() {
        var values = listOf("a", "b", "c", "ab", "bc", "abc");

        var result = sortedMapOf(mapAll(index(values, String::length), entry -> {
            var length = entry.getKey();
            var size = entry.getValue().size();

            return entry(length, size);
        })); // 1: 3, 2: 2, 3: 1

        assertEquals(sortedMapOf(
            entry(1, 3),
            entry(2, 2),
            entry(3, 1)
        ), result);

        assertEquals(result, values.stream()
            .collect(Collectors.groupingBy(String::length)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size(), (v1, v2) -> {
                    throw new IllegalStateException();
                }, TreeMap::new)));
    }

    @Test
    public void testExists() {
        var values = listOf(1, 2, 3);

        var result = exists(values, value -> value < 3); // true

        assertTrue(result);

        assertEquals(result, values.stream().anyMatch(value -> value < 3));

        assertFalse(exists(values, value -> value > 3));
    }
}
