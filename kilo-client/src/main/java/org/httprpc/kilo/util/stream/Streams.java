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
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Iterables} instead.
     */
    @Deprecated
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
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Iterables} instead.
     */
    @Deprecated
    public static <T> Stream<T> streamOf(Collection<T> collection) {
        if (collection == null) {
            throw new IllegalArgumentException();
        }

        return collection.stream();
    }

    /**
     * Collects stream contents.
     *
     * @param <T>
     * The element type.
     *
     * @param <R>
     * The result type.
     *
     * @param stream
     * The stream to collect.
     *
     * @param collector
     * The collector function.
     *
     * @return
     * The collection result.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections} instead.
     */
    @Deprecated
    public static <T, R> R collect(Stream<T> stream, Function<Stream<T>, R> collector) {
        if (stream == null || collector == null) {
            throw new IllegalArgumentException();
        }

        return collector.apply(stream);
    }

    /**
     * Returns a function that produces an iterable.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections} instead.
     */
    @Deprecated
    public static <E> Function<Stream<E>, Iterable<E>> toIterable() {
        return stream -> stream::iterator;
    }

    /**
     * Returns a function that produces a list.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#listOf(Iterable)} instead.
     */
    @Deprecated
    public static <E> Function<Stream<E>, List<E>> toList() {
        return Streams::listOf;
    }

    /**
     * Returns a function that produces an immutable list.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#immutableListOf(Iterable)}
     * instead.
     */
    @Deprecated
    public static <E> Function<Stream<E>, List<E>> toImmutableList() {
        return stream -> java.util.Collections.unmodifiableList(listOf(stream));
    }

    private static <E> List<E> listOf(Stream<E> stream) {
        var list = new ArrayList<E>();

        for (var element : collect(stream, toIterable())) {
            list.add(element);
        }

        return list;
    }

    /**
     * Returns a function that produces a map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#mapOf(Iterable)} instead.
     */
    @Deprecated
    public static <K, V> Function<Stream<Map.Entry<K, V>>, Map<K, V>> toMap() {
        return Streams::mapOf;
    }

    /**
     * Returns a function that produces an immutable map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#immutableMapOf(Iterable)}
     * instead.
     */
    @Deprecated
    public static <K, V> Function<Stream<Map.Entry<K, V>>, Map<K, V>> toImmutableMap() {
        return stream -> java.util.Collections.unmodifiableMap(mapOf(stream));
    }

    private static <K, V> Map<K, V> mapOf(Stream<Map.Entry<K, V>> stream) {
        var map = new LinkedHashMap<K, V>();

        for (var entry : collect(stream, toIterable())) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    /**
     * Returns a function that produces a sorted map.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#sortedMapOf(Iterable)}
     * instead.
     */
    @Deprecated
    public static <K extends Comparable<? super K>, V> Function<Stream<Map.Entry<K, V>>, SortedMap<K, V>> toSortedMap() {
        return Streams::sortedMapOf;
    }

    private static <K extends Comparable<? super K>, V> SortedMap<K, V> sortedMapOf(Stream<Map.Entry<K, V>> stream) {
        var map = new TreeMap<K, V>();

        for (var entry : collect(stream, toIterable())) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    /**
     * Returns a function that produces a set.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#setOf(Iterable)} instead.
     */
    @Deprecated
    public static <E> Function<Stream<E>, Set<E>> toSet() {
        return Streams::setOf;
    }

    /**
     * Returns a function that produces an immutable set.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#immutableSetOf(Iterable)}
     * instead.
     */
    @Deprecated
    public static <E> Function<Stream<E>, Set<E>> toImmutableSet() {
        return stream -> java.util.Collections.unmodifiableSet(setOf(stream));
    }

    private static <E> Set<E> setOf(Stream<E> stream) {
        var set = new LinkedHashSet<E>();

        for (var element : collect(stream, toIterable())) {
            set.add(element);
        }

        return set;
    }

    /**
     * Returns a function that produces a sorted set.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     *
     * @deprecated
     * Use {@link org.httprpc.kilo.util.Collections#sortedSetOf(Iterable)}
     * instead.
     */
    @Deprecated
    public static <E extends Comparable<? super E>> Function<Stream<E>, SortedSet<E>> toSortedSet() {
        return Streams::sortedSetOf;
    }

    private static <E extends Comparable<? super E>> SortedSet<E> sortedSetOf(Stream<E> stream) {
        var set = new TreeSet<E>();

        for (var element : collect(stream, toIterable())) {
            set.add(element);
        }

        return set;
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
     *
     * @deprecated
     * Use {@link BeanAdapter#toType(Class)} instead.
     */
    @Deprecated
    public static <T> Function<Object, T> toType(Class<T> type) {
        return value -> BeanAdapter.coerce(value, type);
    }
}
