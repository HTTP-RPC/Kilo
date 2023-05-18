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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptionalsTest {
    @Test
    public void testCoalesce() {
        var a = Optional.ofNullable(null).orElse(Optional.ofNullable(null).orElse("xyz")); // xyz
        var b = Optionals.coalesce(null, null, "xyz"); // xyz

        assertEquals(a, b);
    }

    @Test
    public void testMap() {
        var a = Optional.ofNullable("hello").map(String::length).orElse(null); // 5
        var b = Optionals.map("hello", String::length); // 5

        assertEquals(a, b);

        assertNull(Optionals.map(null, String::length));
    }

    @Test
    public void testMapWithDefaultValue() {
        var a = Optional.ofNullable((String)null).map(String::isEmpty).orElse(true); // true
        var b = Optionals.map(null, String::isEmpty, true); // true

        assertEquals(a, b);

        assertTrue(Optionals.map("xyz", value -> null, true));
    }
}
