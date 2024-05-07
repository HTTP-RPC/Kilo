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
     * Returns the first non-{@code null} argument value.
     *
     * @param <T>
     * The argument type.
     *
     * @param values
     * The argument values.
     *
     * @return
     * The first non-{@code null} argument value, or {@code null} if only
     * {@code null} values were provided.
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
     * Performs an action on an optional value.
     *
     * @param <T>
     * The value type.
     *
     * @param <U>
     * The type produced by the action, which may be {@link Void}.
     *
     * @param value
     * The optional value.
     *
     * @param action
     * The action to perform.
     *
     * @return
     * The result of performing the action on the provided value, or
     * {@code null} if the value was {@code null}.
     */
    public static <T, U> U perform(T value, Function<? super T, ? extends U> action) {
        if (action == null) {
            throw new IllegalArgumentException();
        }

        return (value == null) ? null : action.apply(value);
    }
}
