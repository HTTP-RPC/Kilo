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

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
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
     * Returns the index of the first element that matches a given predicate.
     *
     * @param <T>
     * The element type.
     *
     * @param stream
     * The stream to search.
     *
     * @param predicate
     * The predicate to match.
     *
     * @return
     * The index of the first matching element, or -1 if no match is found.
     *
     */
    public static <T> int indexWhere(Stream<T> stream, Predicate<? super T> predicate) {
        if (stream == null || predicate == null) {
            throw new IllegalArgumentException();
        }

        var iterator = stream.iterator();

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
