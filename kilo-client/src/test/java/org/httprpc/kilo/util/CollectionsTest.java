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
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionsTest {
    @Test
    public void testListOf() {
        var list = new ArrayList<Integer>(3);

        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(list, listOf(1, 2, 3));
    }

    @Test
    public void testMapOf() {
        var map = new HashMap<String, Integer>();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        assertEquals(map, mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        ));
    }

    @Test
    public void testEmptyListOf() {
        assertTrue(emptyListOf(Integer.class).isEmpty());

        assertEquals(java.util.Collections.<Integer>emptyList(), emptyListOf(Integer.class));
    }

    @Test
    public void testEmptyMapOf() {
        assertTrue(emptyMapOf(String.class, Integer.class).isEmpty());

        assertEquals(java.util.Collections.<String, Integer>emptyMap(), emptyMapOf(String.class, Integer.class));
    }

    @Test
    public void testValueAt() {
        Map<?, ?> map = mapOf(
            entry("a", mapOf(
                entry("b", mapOf(
                    entry("c", listOf(
                        1, 2, 3
                    ))
                ))
            )),
            entry(4, "abc"),
            entry(true, "def")
        );

        assertEquals(Integer.valueOf(2), Collections.valueAt(map, "a", "b", "c", 1));

        assertEquals("abc", Collections.valueAt(map, 4));
        assertEquals("def", Collections.valueAt(map, true));

        assertEquals(map, Collections.valueAt(map));

        assertNull(Collections.valueAt(map, "a", "b", "d", 1));
        assertNull(Collections.valueAt(map, "e"));
        assertNull(Collections.valueAt(null));

        assertThrows(IndexOutOfBoundsException.class, () -> Collections.valueAt(map, "a", "b", "c", 4));
    }
}
