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

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
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

    /**
     * Creates an "equal to" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param <U>
     * The value type.
     *
     * @param transform
     * The transform function.
     *
     * @param value
     * The value to compare.
     *
     * @return
     * The comparison predicate.
     */
    public static <T, U extends Comparable<? super U>> Predicate<? super T> whereEqualTo(Function<? super T, U> transform, U value) {
        if (transform == null || value == null) {
            throw new IllegalArgumentException();
        }

        return element -> transform.apply(element).compareTo(value) == 0;
    }

    /**
     * Creates a "not equal to" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param <U>
     * The value type.
     *
     * @param transform
     * The transform function.
     *
     * @param value
     * The value to compare.
     *
     * @return
     * The comparison predicate.
     */
    public static <T, U extends Comparable<? super U>> Predicate<? super T> whereNotEqualTo(Function<? super T, U> transform, U value) {
        return whereEqualTo(transform, value).negate();
    }

    /**
     * Creates a "less than" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param <U>
     * The value type.
     *
     * @param transform
     * The transform function.
     *
     * @param value
     * The value to compare.
     *
     * @return
     * The comparison predicate.
     */
    public static <T, U extends Comparable<? super U>> Predicate<T> whereLessThan(Function<? super T, U> transform, U value) {
        if (transform == null || value == null) {
            throw new IllegalArgumentException();
        }

        return element -> transform.apply(element).compareTo(value) < 0;
    }

    /**
     * Creates a "less than or equal to" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param <U>
     * The value type.
     *
     * @param transform
     * The transform function.
     *
     * @param value
     * The value to compare.
     *
     * @return
     * The comparison predicate.
     */
    public static <T, U extends Comparable<? super U>> Predicate<T> whereLessThanOrEqualTo(Function<? super T, U> transform, U value) {
        if (transform == null || value == null) {
            throw new IllegalArgumentException();
        }

        return element -> transform.apply(element).compareTo(value) <= 0;
    }

    /**
     * Creates a "greater than" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param <U>
     * The value type.
     *
     * @param transform
     * The transform function.
     *
     * @param value
     * The value to compare.
     *
     * @return
     * The comparison predicate.
     */
    public static <T, U extends Comparable<? super U>> Predicate<T> whereGreaterThan(Function<? super T, U> transform, U value) {
        if (transform == null || value == null) {
            throw new IllegalArgumentException();
        }

        return element -> transform.apply(element).compareTo(value) > 0;
    }

    /**
     * Creates a "greater than or equal to" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param <U>
     * The value type.
     *
     * @param transform
     * The transform function.
     *
     * @param value
     * The value to compare.
     *
     * @return
     * The comparison predicate.
     */
    public static <T, U extends Comparable<? super U>> Predicate<T> whereGreaterThanOrEqualTo(Function<? super T, U> transform, U value) {
        if (transform == null || value == null) {
            throw new IllegalArgumentException();
        }

        return element -> transform.apply(element).compareTo(value) >= 0;
    }

    /**
     * Creates an "is null" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The comparison predicate.
     */
    public static <T> Predicate<? super T> whereNull(Function<? super T, Object> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return element -> transform.apply(element) == null;
    }

    /**
     * Creates an "is not null" predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The comparison predicate.
     */
    public static <T> Predicate<? super T> whereNotNull(Function<? super T, Object> transform) {
        return whereNull(transform).negate();
    }

    /**
     * Calculates a sum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The sum of the transformed values.
     */
    public static <T> int sumOf(Iterable<T> iterable, ToIntFunction<T> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        var total = 0;

        for (var element : iterable) {
            total += transform.applyAsInt(element);
        }

        return total;
    }

    /**
     * Calculates a sum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The sum of the transformed values.
     */
    public static <T> long sumOf(Iterable<T> iterable, ToLongFunction<T> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        var total = 0L;

        for (var element : iterable) {
            total += transform.applyAsLong(element);
        }

        return total;
    }

    /**
     * Calculates a sum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The sum of the transformed values.
     */
    public static <T> double sumOf(Iterable<T> iterable, ToDoubleFunction<T> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        var total = 0.0;

        for (var element : iterable) {
            total += transform.applyAsDouble(element);
        }

        return total;
    }

    /**
     * Calculates an average.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The average of the transformed values.
     */
    public static <T> double averageOf(Iterable<T> iterable, ToDoubleFunction<T> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        var total = 0.0;
        var n = 0;

        for (var element : iterable) {
            total += transform.applyAsDouble(element);

            n++;
        }

        return total / n;
    }

    /**
     * Calculates a minimum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The minimum transformed value.
     */
    public static <T> double minimumOf(Iterable<T> iterable, ToDoubleFunction<T> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        var minimum = Double.POSITIVE_INFINITY;

        for (var element : iterable) {
            minimum = Math.min(transform.applyAsDouble(element), minimum);
        }

        return minimum;
    }

    /**
     * Calculates a maximum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param transform
     * The transform function.
     *
     * @return
     * The maximum transformed value.
     */
    public static <T> double maximumOf(Iterable<T> iterable, ToDoubleFunction<T> transform) {
        if (iterable == null || transform == null) {
            throw new IllegalArgumentException();
        }

        var maximum = Double.NEGATIVE_INFINITY;

        for (var element : iterable) {
            maximum = Math.max(transform.applyAsDouble(element), maximum);
        }

        return maximum;
    }

    /**
     * Calculates a minimum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @return
     * The minimum value.
     */
    public static <T extends Comparable<? super T>> T minimumOf(Iterable<T> iterable) {
        return minimumOf(iterable, Comparator.naturalOrder());
    }

    /**
     * Calculates a maximum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @return
     * The maximum value.
     */
    public static <T extends Comparable<? super T>> T maximumOf(Iterable<T> iterable) {
        return maximumOf(iterable, Comparator.naturalOrder());
    }

    /**
     * Calculates a minimum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param comparator
     * The comparator.
     *
     * @return
     * The minimum value.
     */
    public static <T> T minimumOf(Iterable<T> iterable, Comparator<? super T> comparator) {
        if (iterable == null || comparator == null) {
            throw new IllegalArgumentException();
        }

        T minimum = null;

        for (var element : iterable) {
            if (minimum == null || comparator.compare(element, minimum) < 0) {
                minimum = element;
            }
        }

        return minimum;
    }

    /**
     * Calculates a maximum.
     *
     * @param <T>
     * The element type.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @param comparator
     * The comparator.
     *
     * @return
     * The maximum value.
     */
    public static <T> T maximumOf(Iterable<T> iterable, Comparator<? super T> comparator) {
        if (iterable == null || comparator == null) {
            throw new IllegalArgumentException();
        }

        T maximum = null;

        for (var element : iterable) {
            if (maximum == null || comparator.compare(element, maximum) > 0) {
                maximum = element;
            }
        }

        return maximum;
    }

    /**
     * Calculates an element count.
     *
     * @param iterable
     * The iterable to reduce.
     *
     * @return
     * The element count.
     */
    public static <T> int countOf(Iterable<?> iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException();
        }

        var n = 0;

        var iterator = iterable.iterator();

        while (iterator.hasNext()) {
            iterator.next();

            n++;
        }

        return n;
    }
}
