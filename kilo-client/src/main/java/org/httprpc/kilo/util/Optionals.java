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
        for (var i = 0; i < values.length; i++) {
            var value = values[i];

            if (value != null) {
                return value;
            }
        }

        return null;
    }

    /**
     * Applies a mapping function to an optional value.
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
     * @param transform
     * The mapping function to apply.
     *
     * @return
     * The result of applying the mapping function to the source value, or
     * {@code null} if the source value was {@code null}.
     */
    public static <T, U> U map(T value, Function<? super T, ? extends U> transform) {
        return map(value, transform, null);
    }

    /**
     * Applies a mapping function to an optional value.
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
     * @param transform
     * The mapping function.
     *
     * @param defaultValue
     * The value to return if the source value is {@code null}, or {@code null}
     * for no default value.
     *
     * @return
     * The result of applying the mapping function to the source value, or
     * the default value if the source value was {@code null}.
     */
    public static <T, U> U map(T value, Function<? super T, ? extends U> transform, U defaultValue) {
        if (value == null) {
            return defaultValue;
        } else {
            if (transform == null) {
                throw new IllegalArgumentException();
            }

            return transform.apply(value);
        }
    }

    /**
     * Performs an action on an optional value.
     *
     * @param <T>
     * The source type.
     *
     * @param value
     * The source value.
     *
     * @param action
     * The action to perform if the source value is not {@code null}.
     */
    public static <T> void perform(T value, Consumer<? super T> action) {
        perform(value, action, null);
    }

    /**
     * Performs an action on an optional value.
     *
     * @param <T>
     * The source type.
     *
     * @param value
     * The source value.
     *
     * @param action
     * The action to perform if the source value is not {@code null}.
     *
     * @param defaultAction
     * The action to perform if the source value is {@code null}, or
     * {@code null} for no default action.
     */
    public static <T> void perform(T value, Consumer<? super T> action, Runnable defaultAction) {
        if (value == null) {
            if (defaultAction != null) {
                defaultAction.run();
            }
        } else {
            if (action == null) {
                throw new IllegalArgumentException();
            }

            action.accept(value);
        }
    }
}
