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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides static utility methods for working with iterables.
 */
public class Iterables {
    private static class FilterIterator<T> implements Iterator<T> {
        Iterator<T> iterator;
        Predicate<? super T> predicate;

        Boolean hasNext = null;
        T next = null;

        FilterIterator(Iterator<T> iterator, Predicate<? super T> predicate) {
            this.iterator = iterator;
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext() {
            if (hasNext == null) {
                hasNext = Boolean.FALSE;
                next = null;

                while (iterator.hasNext()) {
                    var element = iterator.next();

                    if (predicate.test(element)) {
                        hasNext = Boolean.TRUE;
                        next = element;

                        break;
                    }
                }
            }

            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            hasNext = null;

            return next;
        }
    }

    private static class MapAllIterator<T, R> implements Iterator<R> {
        Iterator<T> iterator;
        Function<? super T, ? extends R> transform;

        MapAllIterator(Iterator<T> iterator, Function<? super T, ? extends R> transform) {
            this.iterator = iterator;
            this.transform = transform;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public R next() {
            return transform.apply(iterator.next());
        }
    }

    private Iterables() {
    }

    /**
     * Retrieves the first element from an iterable.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable.
     *
     * @return
     * The iterable's first element, or {@code null} if the iterable is empty.
     */
    public static <T> T firstOf(Iterable<T> iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException();
        }

        var iterator = iterable.iterator();

        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * Filters iterable contents.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to filter.
     *
     * @param predicate
     * The predicate function.
     *
     * @return
     * The filtered contents.
     */
    public static <T> Iterable<T> filter(Iterable<T> iterable, Predicate<? super T> predicate) {
        if (iterable == null || predicate == null) {
            throw new IllegalArgumentException();
        }

        return () -> new FilterIterator<>(iterable.iterator(), predicate);
    }

    /**
     * Transforms iterable contents.
     *
     * @param <T>
     * The element type.
     *
     * @param <R>
     * The target type.
     *
     * @param iterable
     * The iterable to transform.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The transformed contents.
     */
    public static <T, R> Iterable<R> mapAll(Iterable<T> iterable, Function<? super T, ? extends R> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        return () -> new MapAllIterator<>(iterable.iterator(), transform);
    }

    /**
     * Indexes iterable contents.
     *
     * @param <T>
     * The element type.
     *
     * @param <K>
     * The key type.
     *
     * @param iterable
     * The iterable to index.
     *
     * @param indexer
     * The indexing function.
     *
     * @return
     * The indexed contents.
     */
    public static <T, K extends Comparable<? super K>> Iterable<Map.Entry<K, List<T>>> index(Iterable<T> iterable, Function<? super T, ? extends K> indexer) {
        if (iterable == null || indexer == null) {
            throw new IllegalArgumentException();
        }

        var map = new TreeMap<K, List<T>>();

        for (var element : iterable) {
            map.computeIfAbsent(indexer.apply(element), key -> new LinkedList<>()).add(element);
        }

        return map.entrySet();
    }

    /**
     * Determines if any element matches a given predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to search.
     *
     * @param predicate
     * The predicate function.
     *
     * @return
     * {@code true} if a matching element is found; {@code false}, otherwise.
     */
    public static <T> boolean exists(Iterable<T> iterable, Predicate<? super T> predicate) {
        return firstOf(filter(iterable, predicate)) != null;
    }
}
