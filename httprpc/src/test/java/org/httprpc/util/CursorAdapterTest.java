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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.httprpc.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class CursorAdapterTest extends AbstractTest {
    @Test
    public void testCursorAdapter() {
        List<Map<String, Object>> values = listOf(
            mapOf(
                entry("a", "abcdef"),
                entry("b", 1.0),
                entry("c", false),
                entry("d", listOf(1, 2, 3)),
                entry("e", mapOf(
                    entry("f", "def"),
                    entry("g", 2.0)
                ))
            )
        );

        List<Map<String, Object>> expected = listOf(
            mapOf(
                entry("A", "abc"),
                entry("B", 5.0),
                entry("C", true),
                entry("D", 6.0),
                entry("E", 4.0)
            )
        );

        CursorAdapter adapter = new CursorAdapter(values, mapOf(
            entry("A", "a.substring(0, 3)"),
            entry("B", "b + 4"),
            entry("C", "!c"),
            entry("D", "d[0] + d[1] + d[2]"),
            entry("E", "e.g * 2")
        ));

        List<Map<String, Object>> actual = StreamSupport.stream(adapter.spliterator(), false).collect(Collectors.toList());

        Assert.assertEquals(expected, actual);
    }
}
