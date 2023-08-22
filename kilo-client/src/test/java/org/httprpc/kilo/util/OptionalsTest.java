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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptionalsTest {
    @Test
    public void testCoalesce() {
        var value = "xyz";

        var a = Optional.ofNullable(null).orElse(Optional.ofNullable(null).orElse(value)); // xyz
        var b = Optionals.coalesce(null, null, value); // xyz

        assertEquals(a, b);
    }

    @Test
    public void testMap() {
        var value = "hello";

        var a = Optional.ofNullable(value).map(String::length).orElse(null); // 5
        var b = Optionals.map(value, String::length); // 5

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

    @Test
    public void testPerform() {
        var value = new AtomicInteger(0);

        Optional.ofNullable(value).ifPresent(AtomicInteger::incrementAndGet);
        Optionals.perform(value, AtomicInteger::incrementAndGet);

        Optionals.perform(null, AtomicInteger::incrementAndGet);

        assertEquals(2, value.get());
    }

    @Test
    public void testPerformWithDefaultAction() {
        var value = new AtomicInteger(0);

        Optional.ofNullable((AtomicInteger)null).ifPresentOrElse(AtomicInteger::incrementAndGet, value::decrementAndGet);
        Optionals.perform(null, AtomicInteger::incrementAndGet, value::decrementAndGet);

        Optionals.perform(null, AtomicInteger::incrementAndGet, null);

        assertEquals(-2, value.get());
    }
}
