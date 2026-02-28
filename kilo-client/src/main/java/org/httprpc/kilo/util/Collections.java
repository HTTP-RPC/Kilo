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
import java.util.function.Predicate;

/**
 * Provides static utility methods for working with collections.
 */
public class Collections {
    private Collections() {
    }

    /**
     * Creates a list.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The list elements.
     *
     * @return
     * A random-access list containing the provided elements in the order
     * given.
     */
    @SafeVarargs
    public static <E> List<E> listOf(E... elements) {
        var list = new ArrayList<E>(elements.length);

        java.util.Collections.addAll(list, elements);

        return list;
    }

    /**
     * Creates a list.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The list elements.
     *
     * @return
     * A random-access list containing the provided elements in the order
     * given.
     */
    public static <E> List<E> listOf(Iterable<? extends E> elements) {
        if (elements == null) {
            throw new IllegalArgumentException();
        }

        var list = new ArrayList<E>();

        for (var element : elements) {
            list.add(element);
        }

        return list;
    }

    /**
     * Creates an immutable list.
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
     * Creates an immutable list.
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
    public static <E> List<E> immutableListOf(Iterable<? extends E> elements) {
        return java.util.Collections.unmodifiableList(listOf(elements));
    }

    /**
     * Creates an empty list.
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

        return java.util.Collections.emptyList();
    }

    /**
     * Creates a sorted list.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The list elements.
     *
     * @return
     * A sorted list of the provided elements.
     */
    @SafeVarargs
    public static <E extends Comparable<? super E>> List<E> sortedListOf(E... elements) {
        var list = listOf(elements);

        java.util.Collections.sort(list);

        return list;
    }

    /**
     * Creates a sorted list.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The list elements.
     *
     * @return
     * A sorted list of the provided elements.
     */
    public static <E extends Comparable<? super E>> List<E> sortedListOf(Iterable<? extends E> elements) {
        List<E> list = listOf(elements);

        java.util.Collections.sort(list);

        return list;
    }

    /**
     * Creates a map.
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
     * A map containing the provided entries in the order given.
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        var map = new LinkedHashMap<K, V>(entries.length);

        for (var entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    /**
     * Creates a map.
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
     * A map containing the provided entries in the order given.
     */
    public static <K, V> Map<K, V> mapOf(Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
        if (entries == null) {
            throw new IllegalArgumentException();
        }

        var map = new LinkedHashMap<K, V>();

        for (var entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    /**
     * Creates an immutable map.
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
     * Creates an immutable map.
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
    public static <K, V> Map<K, V> immutableMapOf(Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
        return java.util.Collections.unmodifiableMap(mapOf(entries));
    }

    /**
     * Creates an empty map.
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

        return java.util.Collections.emptyMap();
    }

    /**
     * Creates a sorted map.
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
    public static <K extends Comparable<? super K>, V> SortedMap<K, V> sortedMapOf(Map.Entry<K, V>... entries) {
        var map = new TreeMap<K, V>();

        for (var entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    /**
     * Creates a sorted map.
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
    public static <K extends Comparable<? super K>, V> SortedMap<K, V> sortedMapOf(Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
        if (entries == null) {
            throw new IllegalArgumentException();
        }

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
     * Creates a set.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The set elements.
     *
     * @return
     * A set containing the provided elements in the order given.
     */
    @SafeVarargs
    public static <E> Set<E> setOf(E... elements) {
        var set = new LinkedHashSet<E>(elements.length);

        java.util.Collections.addAll(set, elements);

        return set;
    }

    /**
     * Creates a set.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The set elements.
     *
     * @return
     * A set containing the provided elements in the order given.
     */
    public static <E> Set<E> setOf(Iterable<? extends E> elements) {
        if (elements == null) {
            throw new IllegalArgumentException();
        }

        var set = new LinkedHashSet<E>();

        for (var element : elements) {
            set.add(element);
        }

        return set;
    }

    /**
     * Creates an immutable set.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The set elements.
     *
     * @return
     * An immutable set containing the provided elements in the order given.
     */
    @SafeVarargs
    public static <E> Set<E> immutableSetOf(E... elements) {
        return java.util.Collections.unmodifiableSet(setOf(elements));
    }

    /**
     * Creates an immutable set.
     *
     * @param <E>
     * The element type.
     *
     * @param elements
     * The set elements.
     *
     * @return
     * An immutable set containing the provided elements in the order given.
     */
    public static <E> Set<E> immutableSetOf(Iterable<? extends E> elements) {
        return java.util.Collections.unmodifiableSet(setOf(elements));
    }

    /**
     * Creates an empty set.
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

        return java.util.Collections.emptySet();
    }

    /**
     * Creates a sorted set.
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
    public static <E extends Comparable<? super E>> SortedSet<E> sortedSetOf(E... elements) {
        var set = new TreeSet<E>();

        java.util.Collections.addAll(set, elements);

        return set;
    }

    /**
     * Creates a sorted set.
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
    public static <E extends Comparable<? super E>> SortedSet<E> sortedSetOf(Iterable<? extends E> elements) {
        if (elements == null) {
            throw new IllegalArgumentException();
        }

        var set = new TreeSet<E>();

        for (var element : elements) {
            set.add(element);
        }

        return set;
    }

    /**
     * Creates a synchronized list.
     *
     * @param <E>
     * The element type.
     *
     * @param list
     * The source list.
     *
     * @return
     * The synchronized list.
     */
    public static <E> List<E> synchronizedListOf(List<E> list) {
        if (list == null) {
            throw new IllegalArgumentException();
        }

        return java.util.Collections.synchronizedList(list);
    }

    /**
     * Creates a synchronized map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @param map
     * The source map.
     *
     * @return
     * The synchronized map.
     */
    public static <K, V> Map<K, V> synchronizedMapOf(Map<K, V> map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        return java.util.Collections.synchronizedMap(map);
    }

    /**
     * Creates a synchronized set.
     *
     * @param <E>
     * The element type.
     *
     * @param set
     * The source set.
     *
     * @return
     * The synchronized set.
     */
    public static <E> Set<E> synchronizedSetOf(Set<E> set) {
        if (set == null) {
            throw new IllegalArgumentException();
        }

        return java.util.Collections.synchronizedSet(set);
    }

    /**
     * Returns the index of the first list element that matches a given
     * predicate.
     *
     * @param <E>
     * The element type.
     *
     * @param list
     * The list to search.
     *
     * @param predicate
     * The predicate to match.
     *
     * @return
     * The index of the first matching element, or -1 if no match is found.
     */
    public static <E> int indexWhere(List<E> list, Predicate<? super E> predicate) {
        if (list == null || predicate == null) {
            throw new IllegalArgumentException();
        }

        var i = 0;

        for (var element : list) {
            if (predicate.test(element)) {
                return i;
            }

            i++;
        }

        return -1;
    }

    /**
     * Returns the index of the last list element that matches a given
     * predicate.
     *
     * @param <E>
     * The element type.
     *
     * @param list
     * The list to search.
     *
     * @param predicate
     * The predicate to match.
     *
     * @return
     * The index of the last matching element, or -1 if no match is found.
     */
    public static <E> int lastIndexWhere(List<E> list, Predicate<? super E> predicate) {
        if (list == null || predicate == null) {
            throw new IllegalArgumentException();
        }

        var i = list.size() - 1;

        for (var element : list.reversed()) {
            if (predicate.test(element)) {
                break;
            }

            i--;
        }

        return i;
    }
}
