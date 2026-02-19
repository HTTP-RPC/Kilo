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
    public void testIndexWhere() {
        var strings = listOf("a", "bc", "def");

        var i = indexWhere(streamOf(strings), value -> value.length() == 3); // 2

        assertEquals(2, i);

        var j = indexWhere(streamOf(strings), value -> value.length() == 4); // -1

        assertEquals(-1, j);
    }

    @Test
    public void testToType() {
        var strings = listOf("1", "2", "3");

        var integers = streamOf(strings).map(toType(Integer.class)).toList(); // 1, 2, 3

        assertEquals(listOf(1, 2, 3), integers);
    }
}
