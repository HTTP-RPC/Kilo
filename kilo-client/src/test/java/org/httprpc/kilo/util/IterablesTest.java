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
import java.util.stream.Collectors;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

public class IterablesTest {
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
    public void testFirstOf() {
        var values = listOf(1, 2, 3);

        var result = firstOf(values); // 1

        assertEquals(1, result);

        assertEquals(result, values.stream().findFirst().orElse(null));

        assertNull(firstOf(listOf()));
    }

    @Test
    public void testExists() {
        var values = listOf(1, 2, 3);

        var result = exists(values, value -> value < 3); // true

        assertTrue(result);

        assertEquals(result, values.stream().anyMatch(value -> value < 3));

        assertFalse(exists(values, value -> value > 3));
    }

    @Test
    public void testCollect() {
        var values = listOf("a", "bc", "def");

        var result = collect(values, iterable -> {
            var stringBuilder = new StringBuilder();

            for (var element : iterable) {
                stringBuilder.append(element);
            }

            return stringBuilder.toString();
        }); // abcdef

        assertEquals("abcdef", result);
    }

    @Test
    public void testToSumInt() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toSum(Integer::intValue)); // 15

        assertEquals(15, result);

        assertEquals(result, values.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void testToSumLong() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toSum(Integer::longValue)); // 15L

        assertEquals(15L, result);

        assertEquals(result, values.stream().mapToLong(Integer::longValue).sum());
    }

    @Test
    public void testToSumDouble() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toSum(Integer::doubleValue)); // 15.0

        assertEquals(15.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::doubleValue).sum());
    }

    @Test
    public void testToAverageInt() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toAverage(Integer::intValue)); // 3.0

        assertEquals(3.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::intValue).average().orElse(Double.NaN));

        assertEquals(Double.NaN, collect(emptyListOf(Integer.class), toAverage(Integer::intValue)));
    }

    @Test
    public void testToAverageLong() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toAverage(Integer::longValue)); // 3.0

        assertEquals(3.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::longValue).average().orElse(Double.NaN));

        assertEquals(Double.NaN, collect(emptyListOf(Integer.class), toAverage(Integer::longValue)));
    }

    @Test
    public void testToAverageDouble() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toAverage(Integer::doubleValue)); // 3.0

        assertEquals(3.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::doubleValue).average().orElse(Double.NaN));

        assertEquals(Double.NaN, collect(emptyListOf(Integer.class), toAverage(Integer::doubleValue)));
    }

    @Test
    public void testToMinimumInt() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toMinimum(Integer::intValue)); // 1

        assertEquals(1, result);

        assertEquals(result, values.stream().mapToInt(Integer::intValue).min().orElseThrow());

        assertNull(collect(emptyListOf(Integer.class), toMinimum(Integer::intValue)));
    }

    @Test
    public void testToMinimumLong() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toMinimum(Integer::longValue)); // 1L

        assertEquals(1L, result);

        assertEquals(result, values.stream().mapToLong(Integer::longValue).min().orElseThrow());

        assertNull(collect(emptyListOf(Integer.class), toMinimum(Integer::longValue)));
    }

    @Test
    public void testToMinimumDouble() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toMinimum(Integer::doubleValue)); // 1.0

        assertEquals(1.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::doubleValue).min().orElseThrow());

        assertNull(collect(emptyListOf(Integer.class), toMinimum(Integer::doubleValue)));
    }

    @Test
    public void testToMaximumInt() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toMaximum(Integer::intValue)); // 5

        assertEquals(5, result);

        assertEquals(result, values.stream().mapToInt(Integer::intValue).max().orElseThrow());

        assertNull(collect(emptyListOf(Integer.class), toMaximum(Integer::intValue)));
    }

    @Test
    public void testToMaximumLong() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toMaximum(Integer::longValue)); // 5L

        assertEquals(5L, result);

        assertEquals(result, values.stream().mapToLong(Integer::longValue).max().orElseThrow());

        assertNull(collect(emptyListOf(Integer.class), toMaximum(Integer::longValue)));
    }

    @Test
    public void testToMaximumDouble() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = collect(values, toMaximum(Integer::doubleValue)); // 5.0

        assertEquals(5.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::doubleValue).max().orElseThrow());

        assertNull(collect(emptyListOf(Integer.class), toMaximum(Integer::doubleValue)));
    }

    @Test
    public void testToMinimum() {
        var values = listOf("a", "b", "c", "d", "e");

        var result = collect(values, toMinimum()); // a

        assertEquals("a", result);

        assertEquals(result, values.stream().min(String::compareTo).orElse(null));

        assertNull(collect(emptyListOf(String.class), toMinimum()));
    }

    @Test
    public void testToMaximum() {
        var values = listOf("a", "b", "c", "d", "e");

        var result = collect(values, toMaximum()); // e

        assertEquals("e", result);

        assertEquals(result, values.stream().max(String::compareTo).orElse(null));

        assertNull(collect(emptyListOf(String.class), toMaximum()));
    }

    @Test
    public void testGroupingBy() {
        var values = listOf("a", "b", "ab", "bc", "abc");

        var result = collect(values, groupingBy(String::length)); // 1: a, b; 2: ab, bc; 3: abc

        assertEquals(mapOf(
            entry(1, listOf("a", "b")),
            entry(2, listOf("ab", "bc")),
            entry(3, listOf("abc"))
        ), result);

        assertEquals(result, values.stream().collect(Collectors.groupingBy(String::length)));
    }

    @Test
    public void testToType() {
        var strings = listOf("1", "2", "3");

        var integers = listOf(mapAll(strings, toType(Integer.class))); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), integers);
    }
}
