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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
     * Creates a sorted map of entries.
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
     * A map containing the provided entries sorted by key.
     */
    @SafeVarargs
    public static <K extends Comparable<K>, V> SortedMap<K, V> sortedMapOf(Map.Entry<K, V>... entries) {
        var map = new TreeMap<K, V>();

        for (var entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
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
        var set = new LinkedHashSet<E>(elements.length);

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
     * Creates a sorted set of elements.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The set elements.
     *
     * @return
     * A sorted set containing the provided elements.
     */
    @SafeVarargs
    public static <E extends Comparable<E>> SortedSet<E> sortedSetOf(E... elements) {
        var set = new TreeSet<E>();

        java.util.Collections.addAll(set, elements);

        return set;
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

        return immutableListOf();
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

        return immutableMapOf();
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

        return immutableSetOf();
    }
}
