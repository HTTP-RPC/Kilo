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

public class OptionalsTest {
    @Test
    public void testCoalesce() {
        String a = null;
        String b = null;

        assertEquals(Optional.ofNullable(a).orElse(Optional.ofNullable(b).orElse("xyz")), Optionals.coalesce(a, b, "xyz"));
    }

    @Test
    public void testMap() {
        var text = "hello";

        assertEquals(Optional.ofNullable(text).map(String::length).orElse(null), Optionals.map(text, String::length));

        assertNull(Optionals.map(null, String::length));
    }
}
