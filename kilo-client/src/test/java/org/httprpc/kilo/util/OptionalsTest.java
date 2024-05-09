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

import static org.httprpc.kilo.util.Optionals.coalesce;
import static org.httprpc.kilo.util.Optionals.map;
import static org.httprpc.kilo.util.Optionals.perform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OptionalsTest {
    @Test
    public void testCoalesce() {
        var value = 123;

        var a = Optional.ofNullable(null).orElse(Optional.ofNullable(null).orElse(value)); // 123
        var b = coalesce(null, null, value); // 123

        assertEquals(a, b);
    }

    @Test
    public void testMap() {
        var value = "hello";

        var a = Optional.ofNullable(value).map(String::length).orElse(null); // 5
        var b = map(value, String::length); // 5

        assertEquals(a, b);

        assertNull(map(null, String::length));
    }

    @Test
    public void testPerform() {
        var stringBuilder = new StringBuilder();

        Optional.ofNullable("abc").ifPresent(stringBuilder::append);
        perform("def", stringBuilder::append);

        perform(null, stringBuilder::append);

        assertEquals("abcdef", stringBuilder.toString());
    }
}
