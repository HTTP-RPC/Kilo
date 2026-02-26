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

import org.httprpc.kilo.beans.BeanAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

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
     * The filtered iterable.
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
     * The transformed iterable.
     */
    public static <T, R> Iterable<R> mapAll(Iterable<T> iterable, Function<? super T, ? extends R> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        return () -> new MapAllIterator<>(iterable.iterator(), transform);
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
     * Determines if an iterable is empty.
     *
     * @param iterable
     * The iterable.
     *
     * @return
     * {@code true} if the iterable is empty; {@code false}, otherwise.
     */
    public static boolean isEmpty(Iterable<?> iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException();
        }

        return !iterable.iterator().hasNext();
    }

    /**
     * Returns a function that groups elements by a classification function.
     *
     * @param <K>
     * The key type.
     *
     * @param <T>
     * The element type.
     *
     * @param classifier
     * The classification function.
     *
     * @return
     * The grouping function.
     */
    public static <K, T> Function<Iterable<T>, Map<K, List<T>>> groupingBy(Function<? super T, ? extends K> classifier) {
        if (classifier == null) {
            throw new IllegalArgumentException();
        }

        return iterable -> {
            var map = new LinkedHashMap<K, List<T>>();

            for (var element : iterable) {
                map.computeIfAbsent(classifier.apply(element), key -> new ArrayList<>()).add(element);
            }

            return map;
        };
    }

    /**
     * Returns a function that calculates a sum.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The reduction function.
     */
    public static <T> Function<Iterable<T>, Integer> toSum(ToIntFunction<T> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return iterable -> {
            var total = 0;

            for (var element : iterable) {
                total += transform.applyAsInt(element);
            }

            return total;
        };
    }

    /**
     * Returns a function that calculates a sum.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The reduction function.
     */
    public static <T> Function<Iterable<T>, Long> toSum(ToLongFunction<T> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return iterable -> {
            var total = 0L;

            for (var element : iterable) {
                total += transform.applyAsLong(element);
            }

            return total;
        };
    }

    /**
     * Returns a function that calculates a sum.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The reduction function.
     */
    public static <T> Function<Iterable<T>, Double> toSum(ToDoubleFunction<T> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return iterable -> {
            var total = 0.0;

            for (var element : iterable) {
                total += transform.applyAsDouble(element);
            }

            return total;
        };
    }

    /**
     * Returns a function that calculates an average.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The reduction function.
     */
    public static <T> Function<Iterable<T>, Double> toAverage(ToIntFunction<T> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return iterable -> {
            var total = 0;
            var n = 0;

            for (var element : iterable) {
                total += transform.applyAsInt(element);

                n++;
            }

            return (double)total / n;
        };
    }

    /**
     * Returns a function that calculates an average.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The reduction function.
     */
    public static <T> Function<Iterable<T>, Double> toAverage(ToLongFunction<T> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return iterable -> {
            var total = 0L;
            var n = 0;

            for (var element : iterable) {
                total += transform.applyAsLong(element);

                n++;
            }

            return (double)total / n;
        };
    }

    /**
     * Returns a function that calculates an average.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The reduction function.
     */
    public static <T> Function<Iterable<T>, Double> toAverage(ToDoubleFunction<T> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return iterable -> {
            var total = 0.0;
            var n = 0;

            for (var element : iterable) {
                total += transform.applyAsDouble(element);

                n++;
            }

            return total / n;
        };
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
}
