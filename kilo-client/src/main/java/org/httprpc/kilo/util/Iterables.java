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
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides static utility methods for working with iterables.
 */
public class Iterables {
    private Iterables() {
    }

    public static <T> Iterable<T> filter(Iterable<T> iterable, Predicate<? super T> predicate) {
        // TODO
        return null;
    }

    public static <T, R> Iterable<R> map(Iterable<T> iterable, Function<? super T, ? extends R> transform) {
        // TODO
        return null;
    }

    /**
     * Collects iterable contents.
     *
     * @param <T>
     * The element type.
     *
     * @param <R>
     * The result type.
     *
     * @param iterable
     * The iterable to collect.
     *
     * @param collector
     * The collector function.
     *
     * @return
     * The collection result.
     */
    public static <T, R> R collect(Iterable<T> iterable, Function<Iterable<T>, R> collector) {
        if (iterable == null || collector == null) {
            throw new IllegalArgumentException();
        }

        return collector.apply(iterable);
    }

    /**
     * Returns a function that produces a list.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     */
    public static <E> Function<Iterable<E>, List<E>> toList() {
        return Iterables::listOf;
    }

    /**
     * Returns a function that produces an immutable list.
     *
     * @param <E>
     * The element type.
     *
     * @return
     * The collector function.
     */
    public static <E> Function<Iterable<E>, List<E>> toImmutableList() {
        return iterable -> java.util.Collections.unmodifiableList(listOf(iterable));
    }

    private static <E> List<E> listOf(Iterable<E> iterable) {
        var list = new ArrayList<E>();

        for (var element : iterable) {
            list.add(element);
        }

        return list;
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
