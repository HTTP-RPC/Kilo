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

package org.httprpc.kilo.test;

import org.httprpc.kilo.Description;

@Description("Represents a film rating.")
public enum Rating {
    @Description("A \"G\" rating.")
    G("G"),
    @Description("A \"PG\" rating.")
    PG("PG"),
    @Description("A \"PG-13\" rating.")
    PG_13("PG-13"),
    @Description("An \"R\" rating.")
    R("R"),
    @Description("An \"NC-17\" rating.")
    NC_17("NC-17");

    private final String value;

    Rating(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
