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
import static org.httprpc.kilo.util.Optionals.*;
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
        var values = listOf("a", "ab", "abc");

        var result = listOf(mapAll(values, String::length)); // 1, 2, 3

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
    public void testGroupingBy() {
        var values = listOf("a", "b", "ab", "bc", "abc");

        var result = map(values, groupingBy(String::length)); // 1: a, b; 2: ab, bc; 3: abc

        assertEquals(mapOf(
            entry(1, listOf("a", "b")),
            entry(2, listOf("ab", "bc")),
            entry(3, listOf("abc"))
        ), result);
    }

    @Test
    public void testToSumInt() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = map(values, toSum(Integer::intValue)); // 15

        assertEquals(15, result);

        assertEquals(result, values.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void testToSumLong() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = map(values, toSum(Integer::longValue)); // 15L

        assertEquals(15L, result);

        assertEquals(result, values.stream().mapToLong(Integer::longValue).sum());
    }

    @Test
    public void testToSumDouble() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = map(values, toSum(Integer::doubleValue)); // 15.0

        assertEquals(15.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::doubleValue).sum());
    }

    @Test
    public void testToAverageInt() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = map(values, toAverage(Integer::intValue)); // 3.0

        assertEquals(3.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::intValue).average().orElse(Double.NaN));

        assertEquals(Double.NaN, map(emptyListOf(Integer.class), toAverage(Integer::intValue)));
    }

    @Test
    public void testToAverageLong() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = map(values, toAverage(Integer::longValue)); // 3.0

        assertEquals(3.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::longValue).average().orElse(Double.NaN));

        assertEquals(Double.NaN, map(emptyListOf(Integer.class), toAverage(Integer::longValue)));
    }

    @Test
    public void testToAverageDouble() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = map(values, toAverage(Integer::doubleValue)); // 3.0

        assertEquals(3.0, result);

        assertEquals(result, values.stream().mapToDouble(Integer::doubleValue).average().orElse(Double.NaN));

        assertEquals(Double.NaN, map(emptyListOf(Integer.class), toAverage(Integer::doubleValue)));
    }

    @Test
    public void testToType() {
        var strings = listOf("1", "2", "3");

        var integers = listOf(mapAll(strings, toType(Integer.class))); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), integers);
    }
}
