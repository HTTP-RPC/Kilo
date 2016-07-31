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

package org.httprpc;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

public class ResultTest {
    @Test
    public void testGetValue() {
        Map<String, ?> root = mapOf(entry("a", mapOf(entry("b", mapOf(entry("c", 42))))));

        Number value = Result.getValue(root, "a.b.c");

        Assert.assertEquals(42, value.intValue());
    }

    @Test
    public void testGetMissingValue() {
        Map<String, ?> root = mapOf(entry("a", mapOf(entry("b", mapOf(entry("c", 42))))));

        Assert.assertEquals(null, Result.getValue(root, "a.b.x"));
    }
}

