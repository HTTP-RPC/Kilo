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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Provides static utility methods for working with collections.
 */
public class Collections {
    private Collections() {
    }

    /**
     * Creates a list of elements.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The list elements.
     *
     * @return
     * A mutable, random-access list containing the provided elements in the
     * order given.
     */
    @SafeVarargs
    public static <E> List<E> listOf(E... elements) {
        var list = new ArrayList<E>(elements.length);

        java.util.Collections.addAll(list, elements);

        return list;
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
     * An immutable, random-access list containing the provided elements in the
     * order given.
     */
    @SafeVarargs
    public static <E> List<E> immutableListOf(E... elements) {
        return java.util.Collections.unmodifiableList(listOf(elements));
    }

    /**
     * Creates a map of entries.
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
     * A mutable map containing the provided entries in the order given.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        var map = new LinkedHashMap<K, V>();

        for (var entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
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
     * An immutable map containing the provided entries in the order given.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> immutableMapOf(Map.Entry<K, V>... entries) {
        return java.util.Collections.unmodifiableMap(mapOf(entries));
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
     * Creates a set of elements.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The set elements.
     *
     * @return
     * A mutable set containing the provided elements.
     */
    @SafeVarargs
    public static <E> Set<E> setOf(E... elements) {
        var set = new HashSet<E>(elements.length);

        java.util.Collections.addAll(set, elements);

        return set;
    }

    /**
     * Creates an immutable set of elements.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The set elements.
     *
     * @return
     * An immutable set containing the provided elements.
     */
    @SafeVarargs
    public static <E> Set<E> immutableSetOf(E... elements) {
        return java.util.Collections.unmodifiableSet(setOf(elements));
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
     * Returns an empty set.
     *
     * @param <E>
     * The element type.
     *
     * @param elementType
     * The element type.
     *
     * @return
     * An empty set.
     */
    public static <E> Set<E> emptySetOf(Class<E> elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException();
        }

        return setOf();
    }

    /**
     * Returns the index of the first element in a list that matches the given
     * predicate.
     *
     * @param <E>
     * The element type.
     *
     * @param list
     * The list of elements.
     *
     * @param predicate
     * The predicate.
     *
     * @return
     * The index of the first matching element, or {@code -1} if no match was
     * found.
     */
    public static <E> int firstIndexWhere(List<E> list, Predicate<E> predicate) {
        var iterator = list.iterator();

        var i = 0;

        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                return i;
            }

            i++;
        }

        return -1;
    }

    /**
     * Returns the index of the last element in a list that matches the given
     * predicate.
     *
     * @param <E>
     * The element type.
     *
     * @param list
     * The list of elements.
     *
     * @param predicate
     * The predicate.
     *
     * @return
     * The index of the last matching element, or {@code -1} if no match was
     * found.
     */
    public static <E> int lastIndexWhere(List<E> list, Predicate<E> predicate) {
        var i = list.size();

        var iterator = list.listIterator(i);

        while (iterator.hasPrevious()) {
            i--;

            if (predicate.test(iterator.previous())) {
                return i;
            }
        }

        return -1;
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
        return valueAt(root, Arrays.asList(path));
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
    public static Object valueAt(Object root, List<?> path) {
        if (root == null) {
            return null;
        } else {
            if (path == null) {
                throw new IllegalArgumentException();
            }

            if (path.isEmpty()) {
                return root;
            } else {
                var component = path.get(0);

                Object value;
                if (root instanceof List<?> list && component instanceof Number number) {
                    value = list.get(number.intValue());
                } else if (root instanceof Map<?, ?> map) {
                    value = map.get(component);
                } else {
                    throw new IllegalArgumentException();
                }

                return valueAt(value, path.subList(1, path.size()));
            }
        }
    }
}
