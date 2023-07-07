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

import java.util.function.Function;

/**
 * Provides static utility methods for working with optional values.
 */
public class Optionals {
    private Optionals() {
    }

    /**
     * Returns the first non-{@code null} value in a sequence of values.
     *
     * @param <T>
     * The type of the values in the sequence.
     *
     * @param values
     * The sequence of values.
     *
     * @return
     * The first non-{@code null} value in the sequence.
     */
    @SafeVarargs
    public static <T> T coalesce(T... values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        for (var i = 0; i < values.length; i++) {
            var value = values[i];

            if (value != null) {
                return value;
            }
        }

        return null;
    }

    /**
     * Applies a mapping function to a value.
     *
     * @param <T>
     * The source type.
     *
     * @param <U>
     * The target type.
     *
     * @param value
     * The source value.
     *
     * @param mapper
     * The mapping function.
     *
     * @return
     * The result of applying the mapping function to the source value, or
     * {@code null} if the source value was {@code null} or the mapping
     * function did not produce a value.
     */
    public static <T, U> U map(T value, Function<? super T, ? extends U> mapper) {
        return map(value, mapper, null);
    }

    /**
     * Applies a mapping function to a value.
     *
     * @param <T>
     * The source type.
     *
     * @param <U>
     * The target type.
     *
     * @param value
     * The source value.
     *
     * @param mapper
     * The mapping function.
     *
     * @param defaultValue
     * The default value.
     *
     * @return
     * The result of applying the mapping function to the source value, or
     * the default value if the source value was {@code null} or the mapping
     * function did not produce a value.
     */
    public static <T, U> U map(T value, Function<? super T, ? extends U> mapper, U defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            if (mapper == null) {
                throw new IllegalArgumentException();
            }

            return coalesce(mapper.apply(value), defaultValue);
        }
    }
}
