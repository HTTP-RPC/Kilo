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

package org.httprpc.kilo.util.stream;

import org.httprpc.kilo.beans.BeanAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides static utility methods for working with streams.
 */
public class Streams {
    private Streams() {
    }

    /**
     * Returns a stream over an iterable.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable instance.
     *
     * @return
     * A stream over the iterable.
     */
    public static <T> Stream<T> streamOf(Iterable<T> iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException();
        }

        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns a stream over a collection.
     *
     * @param <T>
     * The element type.
     *
     * @param collection
     * The collection instance.
     *
     * @return
     * A stream over the collection.
     */
    public static <T> Stream<T> streamOf(Collection<T> collection) {
        if (collection == null) {
            throw new IllegalArgumentException();
        }

        return collection.stream();
    }

    /**
     * Returns a function that coerces a value to a given type.
     *
     * @param <T>
     * The target type.
     *
     * @param type
     * The target type.
     *
     * @return
     * The coercion function.
     */
    public static <T> Function<Object, T> toType(Class<T> type) {
        return value -> BeanAdapter.coerce(value, type);
    }

    /**
     * Returns a collector that produces a list.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The list collector.
     */
    public static <E> Collector<E, ?, List<E>> toList() {
        return Collector.of(() -> new ArrayList<E>(),
            List::add,
            Streams::combine,
            list -> list);
    }

    /**
     * Returns a collector that produces an immutable list.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The immutable list collector.
     */
    public static <E> Collector<E, ?, List<E>> toImmutableList() {
        return Collector.of(() -> new ArrayList<E>(),
            List::add,
            Streams::combine,
            java.util.Collections::unmodifiableList);
    }

    /**
     * Returns a collector that produces a map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * The map collector.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMap() {
        return Collector.of(() -> new LinkedHashMap<K, V>(),
            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
            Streams::combine,
            map -> map);
    }

    /**
     * Returns a collector that produces an immutable map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * The immutable map collector.
     */
    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toImmutableMap() {
        return Collector.of(() -> new LinkedHashMap<K, V>(),
            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
            Streams::combine,
            java.util.Collections::unmodifiableMap);
    }

    /**
     * Returns a collector that produces a sorted map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * The sorted map collector.
     */
    public static <K extends Comparable<? super K>, V> Collector<Map.Entry<K, V>, ?, SortedMap<K, V>> toSortedMap() {
        return Collector.of(TreeMap::new,
            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
            (map1, map2) -> {
                throw new UnsupportedOperationException();
            }, Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Returns a collector that produces a set.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The set collector.
     */
    public static <E> Collector<E, ?, Set<E>> toSet() {
        return Collector.of(LinkedHashSet::new,
            Set::add,
            Streams::combine,
            Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Returns a collector that produces an immutable set.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The immutable set collector.
     */
    public static <E> Collector<E, ?, Set<E>> toImmutableSet() {
        return Collector.of(() -> new LinkedHashSet<E>(),
            Set::add,
            Streams::combine,
            java.util.Collections::unmodifiableSet);
    }

    /**
     * Returns a collector that produces a sorted set.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The sorted set collector.
     */
    public static <E extends Comparable<? super E>> Collector<E, ?, SortedSet<E>> toSortedSet() {
        return Collector.of(TreeSet::new,
            Set::add,
            Streams::combine,
            Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH);
    }

    private static <T> T combine(T t1, T t2) {
        throw new UnsupportedOperationException();
    }
}
