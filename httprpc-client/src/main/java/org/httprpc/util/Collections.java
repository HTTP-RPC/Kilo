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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that provides static utility methods for working with collections.
 */
public class Collections {
    private Collections() {
    }

    /**
     * Creates an immutable list of elements.
     *
     * @param elements
     * The list elements.
     *
     * @param <E>
     * The type of the elements in the list.
     *
     * @return
     * An immutable list containing the provided elements.
     */
    @SafeVarargs
    public static <E> List<E> listOf(E... elements) {
        return java.util.Collections.unmodifiableList(Arrays.asList(elements));
    }

    /**
     * Creates an immutable map of entries.
     *
     * @param entries
     * The map entries.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * An immutable map containing the provided entries.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();

        for (Map.Entry<K, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return java.util.Collections.unmodifiableMap(map);
    }

    /**
     * Creates an immutable map entry.
     *
     * @param key
     * The entry key.
     *
     * @param value
     * The entry value.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * An immutable map entry containing the provided key/value pair.
     */
    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
