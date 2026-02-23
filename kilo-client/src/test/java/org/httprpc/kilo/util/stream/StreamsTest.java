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

package org.httprpc.kilo.util.stream;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.stream.Streams.*;
import static org.junit.jupiter.api.Assertions.*;

public class StreamsTest {
    @Test
    public void testStreamOfIterable() {
        var iterable = (Iterable<Integer>)listOf(1, 2, 3);

        var a = StreamSupport.stream(iterable.spliterator(), false).findFirst().orElseThrow(); // 1
        var b = streamOf(iterable).findFirst().orElseThrow(); // 1

        assertEquals(a, b);
    }

    @Test
    public void testStreamOfCollection() {
        assertEquals(1, streamOf(listOf(1, 2, 3)).findFirst().orElseThrow());
    }

    @Test
    public void testCollect() {
        var values = listOf(1.0, 2.0, 3.0);

        var a = values.stream().collect(Collector.of(() -> new DoubleAccumulator(Double::sum, 0.0),
            DoubleAccumulator::accumulate,
            (left, right) -> new DoubleAccumulator(Double::sum, left.doubleValue() + right.doubleValue()),
            DoubleAccumulator::doubleValue)); // 6.0

        var b = collect(streamOf(values), stream -> {
            var total = 0.0;

            for (var value : collect(stream, toIterable())) {
                total += value;
            }

            return total;
        }); // 6.0

        assertEquals(a, b);
    }

    @Test
    public void testToIterable() {
        var list1 = listOf(1, 2, 3);

        var list2 = listOf();

        for (var element : collect(streamOf(list1), toIterable())) {
            list2.add(element);
        }

        assertEquals(list1, list2);
    }

    @Test
    public void testToList() {
        var list1 = listOf(1, 2, 3);

        var list2 = collect(streamOf(list1), toList());

        assertEquals(list1, list2);

        list2.remove(2);

        assertEquals(2, list2.size());
    }

    @Test
    public void testToImmutableList() {
        var list1 = immutableListOf(1, 2, 3);

        var list2 = collect(streamOf(list1), toImmutableList());

        assertThrows(UnsupportedOperationException.class, () -> list2.add(4));

        assertEquals(list1, list2);
    }

    @Test
    public void testToMap() {
        var map1 = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        var map2 = collect(streamOf(map1.entrySet()), toMap());

        assertEquals(map1, map2);

        map2.remove("c");

        assertEquals(2, map2.size());
    }

    @Test
    public void testToImmutableMap() {
        var map1 = immutableMapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        var map2 = collect(streamOf(map1.entrySet()), toImmutableMap());

        assertThrows(UnsupportedOperationException.class, () -> map2.put("d", 4));

        assertEquals(map1, map2);
    }

    @Test
    public void testToSortedMap() {
        var sortedMap = collect(streamOf(listOf(
            entry("c", 3),
            entry("b", 2),
            entry("a", 1)
        )), toSortedMap());

        assertEquals("a", sortedMap.firstKey());
        assertEquals("c", sortedMap.lastKey());
    }

    @Test
    public void testToSet() {
        var set1 = setOf(1, 2, 3);

        var set2 = collect(streamOf(set1), toSet());

        assertEquals(set1, set2);

        set2.remove(2);

        assertEquals(2, set2.size());
    }

    @Test
    public void testToImmutableSet() {
        var set1 = immutableSetOf(1, 2, 3);

        var set2 = collect(streamOf(set1), toImmutableSet());

        assertThrows(UnsupportedOperationException.class, () -> set2.add(4));

        assertEquals(set1, set2);
    }

    @Test
    public void testToSortedSet() {
        var sortedSet = collect(streamOf(listOf(3, 2, 1)), toSortedSet());

        assertEquals(1, sortedSet.first());
        assertEquals(3, sortedSet.last());
    }

    @Test
    public void testToType() {
        var strings = listOf("1", "2", "3");

        var integers = collect(streamOf(strings).map(toType(Integer.class)), toList()); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), integers);
    }
}
