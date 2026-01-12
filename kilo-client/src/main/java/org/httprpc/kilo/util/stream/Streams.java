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

import java.util.function.Function;

/**
 * Provides static utility methods for working with streams.
 */
public class Streams {
    private Streams() {
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
    public static <T> Function<Object, T> to(Class<T> type) {
        return value -> BeanAdapter.coerce(value, type);
    }
}
