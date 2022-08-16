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
     * Returns the first non-<code>null</code> value in a sequence of values.
     *
     * @param values
     * The sequence of values.
     *
     * @param <T>
     * The type of the values in the sequence.
     *
     * @return
     * The first non-<code>null</code> value in the sequence.
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
     * Maps a non-<code>null</code> value to another value.
     *
     * @param value
     * The value to map.
     *
     * @param mapper
     * The mapping function.
     *
     * @param <T>
     * The original value type.
     *
     * @param <U>
     * The mapped value type.
     *
     * @return
     * The mapped value, or <code>null</code> if the original value is
     * <code>null</code>.
     */
    public static <T, U> U map(T value, Function<? super T, ? extends U> mapper) {
        if (value == null) {
            return null;
        }

        if (mapper == null) {
            throw new IllegalArgumentException();
        }

        return mapper.apply(value);
    }
}
