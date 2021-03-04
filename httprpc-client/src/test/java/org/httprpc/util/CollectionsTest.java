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

package org.httprpc.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.httprpc.util.Collections.valueAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CollectionsTest {
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

        assertEquals(Integer.valueOf(2), valueAt(map, "a", "b", "c", 1));

        assertEquals("abc", valueAt(map, 4));
        assertEquals("def", valueAt(map, true));

        assertEquals(map, valueAt(map));

        assertNull(valueAt(map, "a", "b", "d", 1));
        assertNull(valueAt(map, "e"));
        assertNull(valueAt(null));

        assertThrows(IndexOutOfBoundsException.class, () -> valueAt(map, "a", "b", "c", 4));
    }
}
