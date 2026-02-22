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
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Provides static utility methods for working with streams.
 */
public class Streams {
    // List collector
    private static class ListCollector<E> implements Collector<E, List<E>, List<E>> {
        @Override
        public Supplier<List<E>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<E>, E> accumulator() {
            return List::add;
        }

        @Override
        public BinaryOperator<List<E>> combiner() {
            return (list1, list2) -> {
                throw new UnsupportedOperationException();
            };
        }

        @Override
        public Function<List<E>, List<E>> finisher() {
            return list -> list;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return emptySetOf(Characteristics.class);
        }
    }

    // Immutable list collector
    private static class ImmutableListCollector<E> extends ListCollector<E> {
        @Override
        public Function<List<E>, List<E>> finisher() {
            return java.util.Collections::unmodifiableList;
        }
    }

    // Map collector
    private static class MapCollector<K, V> implements Collector<Map.Entry<K, V>, Map<K, V>, Map<K, V>> {
        @Override
        public Supplier<Map<K, V>> supplier() {
            return LinkedHashMap::new;
        }

        @Override
        public BiConsumer<Map<K, V>, Map.Entry<K, V>> accumulator() {
            return (map, entry) -> map.put(entry.getKey(), entry.getValue());
        }

        @Override
        public BinaryOperator<Map<K, V>> combiner() {
            return (map1, map2) -> {
                throw new UnsupportedOperationException();
            };
        }

        @Override
        public Function<Map<K, V>, Map<K, V>> finisher() {
            return map -> map;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return emptySetOf(Characteristics.class);
        }
    }

    // Immutable map collector
    private static class ImmutableMapCollector<K, V> extends MapCollector<K, V> {
        @Override
        public Function<Map<K, V>, Map<K, V>> finisher() {
            return java.util.Collections::unmodifiableMap;
        }
    }

    // Set collector
    private static class SetCollector<E> implements Collector<E, Set<E>, Set<E>> {
        @Override
        public Supplier<Set<E>> supplier() {
            return LinkedHashSet::new;
        }

        @Override
        public BiConsumer<Set<E>, E> accumulator() {
            return Set::add;
        }

        @Override
        public BinaryOperator<Set<E>> combiner() {
            return (set1, set2) -> {
                throw new UnsupportedOperationException();
            };
        }

        @Override
        public Function<Set<E>, Set<E>> finisher() {
            return set -> set;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return emptySetOf(Characteristics.class);
        }
    }

    // Immutable set collector
    private static class ImmutableSetCollector<E> extends SetCollector<E> {
        @Override
        public Function<Set<E>, Set<E>> finisher() {
            return java.util.Collections::unmodifiableSet;
        }
    }

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
        return new ListCollector<>();
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
        return new ImmutableListCollector<>();
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
        return new MapCollector<>();
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
        return new ImmutableMapCollector<>();
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
        // TODO
        return null;
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
        return new SetCollector<>();
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
        return new ImmutableSetCollector<>();
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
        // TODO
        return null;
    }
}
