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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides static utility methods for working with optional values.
 */
public class Optionals {
    private Optionals() {
    }

    /**
     * Returns either an optional value or the value produced by the given
     * supplier.
     *
     * @param <T>
     * The value type.
     *
     * @param value
     * The optional value.
     *
     * @param supplier
     * The supplier to invoke if the provided value is {@code null}.
     *
     * @return
     * The provided value, or the value produced by the supplier if the value
     * was {@code null}.
     */
    public static <T> T coalesce(T value, Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException();
        }

        return (value != null) ? value : supplier.get();
    }

    /**
     * Applies a mapping function to an optional value.
     *
     * @param <T>
     * The value type.
     *
     * @param <U>
     * The type produced by the mapping function.
     *
     * @param value
     * The optional value.
     *
     * @param transform
     * The mapping function to apply if the provided value is {@code null}.
     *
     * @return
     * The result of applying the mapping function to the provided value, or
     * {@code null} if the value was {@code null}.
     */
    public static <T, U> U map(T value, Function<? super T, ? extends U> transform) {
        if (transform == null) {
            throw new IllegalArgumentException();
        }

        return (value == null) ? null : transform.apply(value);
    }

    /**
     * Performs an action on an optional value.
     *
     * @param <T>
     * The value type.
     *
     * @param value
     * The optional value.
     *
     * @param action
     * The action to perform if the provided value is not {@code null}.
     */
    public static <T> void perform(T value, Consumer<? super T> action) {
        if (action == null) {
            throw new IllegalArgumentException();
        }

        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * Casts an optional value to a given type.
     *
     * @param <T>
     * The target type.
     *
     * @param value
     * The optional value.
     *
     * @param type
     * The target type.
     *
     * @return
     * The value as an instance of the target type, or {@code null} if the
     * value is {@code null} or cannot be cast to the target type.
     */
    public static <T> T cast(Object value, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        } else {
            return null;
        }
    }
}
