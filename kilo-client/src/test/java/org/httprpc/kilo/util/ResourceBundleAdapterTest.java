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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceBundleAdapterTest {
    @Test
    public void testResourceBundleAdapter() {
        var resourceBundle = ResourceBundleAdapter.getBundle(getClass(), "test");

        var resourceBundleAdapter = new ResourceBundleAdapter(resourceBundle);

        assertTrue(resourceBundleAdapter.containsKey("a"));

        assertEquals(resourceBundle.getString("a"), resourceBundleAdapter.get("a"));
        assertEquals(resourceBundle.getString("b"), resourceBundleAdapter.get("b"));
        assertEquals(resourceBundle.getString("c"), resourceBundleAdapter.get("c"));

        assertFalse(resourceBundleAdapter.containsKey("d"));

        assertNull(resourceBundleAdapter.get("d"));
    }
}
