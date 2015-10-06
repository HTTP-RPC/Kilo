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

package vellum.webrpc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for simplifying argument map generation.
 */
public class Arguments {
    private Arguments() {
        // No-op
    }

    /**
     * Creates a map entry.
     *
     * @param key
     * The entry's key.
     *
     * @param value
     * The entry's value.
     *
     * @return
     * The map entry.
     */
    public static Map.Entry<String, Object> entry(final String key, final Object value) {
        return new Map.Entry<String, Object>() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public Object setValue(Object value) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Creates a map from a list of entries.
     *
     * @param entries
     * The entries from which the map will be created.
     *
     * @return
     * A map containing the given entries.
     */
    @SafeVarargs
    public static Map<String, Object> mapOf(Map.Entry<String, Object>... entries) {
        HashMap<String, Object> map = new HashMap<>();

        for (Map.Entry<String, Object> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(map);
    }
}
