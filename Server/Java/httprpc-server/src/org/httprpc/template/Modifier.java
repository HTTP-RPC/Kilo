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

package org.httprpc.template;

import java.util.Locale;

/**
 * Interface representing a modifier.
 */
public interface Modifier {
    /**
     * Applies the modifier.
     *
     * @param value
     * The value to which the modifier is being be applied.
     *
     * @param argument
     * The modifier argument, or <tt>null</tt> if no argument was provided.
     *
     * @param locale
     * The locale in which the modifier is being applied.
     *
     * @return
     * The modified value.
     */
    public Object apply(Object value, String argument, Locale locale);
}
