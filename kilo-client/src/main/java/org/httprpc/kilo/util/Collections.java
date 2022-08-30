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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides static utility methods for working with collections.
 */
public class Collections {
    private Collections() {
    }

    /**
     * Creates an immutable list of elements.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The list elements.
     *
     * @return
     * An immutable list containing the provided elements.
     */
    @SafeVarargs
    public static <E> List<E> listOf(E... elements) {
        if (elements == null) {
            throw new IllegalArgumentException();
        }

        return java.util.Collections.unmodifiableList(Arrays.asList(elements));
    }

    /**
     * Creates an immutable map of entries.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @param entries
     * The map entries.
     *
     * @return
     * An immutable map containing the provided entries.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        if (entries == null) {
            throw new IllegalArgumentException();
        }

        Map<K, V> map = new LinkedHashMap<>();

        for (var entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return java.util.Collections.unmodifiableMap(map);
    }

    /**
     * Creates an immutable map entry.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @param key
     * The entry key.
     *
     * @param value
     * The entry value.
     *
     * @return
     * An immutable map entry containing the provided key/value pair.
     */
    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /**
     * Returns an empty list.
     *
     * @param <E>
     * The element type.
     *
     * @param elementType
     * The element type.
     *
     * @return
     * An empty list.
     */
    public static <E> List<E> emptyListOf(Class<E> elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException();
        }

        return listOf();
    }

    /**
     * Returns an empty map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @param keyType
     * The key type.
     *
     * @param valueType
     * The value type.
     *
     * @return
     * An empty map.
     */
    public static <K, V> Map<K, V> emptyMapOf(Class<K> keyType, Class<V> valueType) {
        if (keyType == null || valueType == null) {
            throw new IllegalArgumentException();
        }

        return mapOf();
    }

    /**
     * Returns the value at a given path.
     *
     * @param root
     * The root object.
     *
     * @param path
     * The path to the value.
     *
     * @return
     * The value at the given path, or {@code null} if the value does not
     * exist.
     */
    public static Object valueAt(Object root, Object... path) {
        return valueAt(root, new LinkedList<>(Arrays.asList(path)));
    }

    private static Object valueAt(Object root, List<?> path) {
        if (root == null) {
            return null;
        } else if (path.isEmpty()) {
            return root;
        } else {
            var component = path.remove(0);

            Object value;
            if (root instanceof List<?> && component instanceof Number) {
                value = ((List<?>)root).get(((Number)component).intValue());
            } else if (root instanceof Map<?, ?>) {
                value = ((Map<?, ?>)root).get(component);
            } else {
                throw new IllegalArgumentException();
            }

            return valueAt(value, path);
        }
    }
}
