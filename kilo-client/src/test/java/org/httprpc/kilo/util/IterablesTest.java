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

import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.stream.Collector;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

public class IterablesTest {
    @Test
    public void testFilter() {
        var values = listOf(1, 2, 3, 4, 5);

        var result = listOf(filter(values, value -> value < 3));

        assertEquals(2, result.size());

        assertEquals(1, result.getFirst());
        assertEquals(2, result.getLast());
    }

    @Test
    public void testMapAll() {
        // TODO
    }

    @Test
    public void testCollect() {
        var values = listOf(1.0, 2.0, 3.0);

        var a = values.stream().collect(Collector.of(() -> new DoubleAccumulator(Double::sum, 0.0),
            DoubleAccumulator::accumulate,
            (left, right) -> new DoubleAccumulator(Double::sum, left.doubleValue() + right.doubleValue()),
            DoubleAccumulator::doubleValue)); // 6.0

        var b = collect(values, iterable -> {
            var total = 0.0;

            for (var value : iterable) {
                total += value;
            }

            return total;
        }); // 6.0

        assertEquals(a, b);
    }

    @Test
    public void testFirstOf() {
        // TODO
    }

    @Test
    public void testToType() {
        var strings = listOf("1", "2", "3");

        var integers = listOf(mapAll(strings, toType(Integer.class))); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), integers);
    }
}
