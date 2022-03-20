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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.httprpc.util.Optionals.coalesce;
import static org.httprpc.util.Optionals.map;

public class OptionalsTest {
    @Test
    public void testCoalesce() {
        Assertions.assertEquals("abc", coalesce(null, null, "abc"));
    }

    @Test
    public void testMap() {
        Assertions.assertEquals(5, map("hello", String::length));
    }
}
