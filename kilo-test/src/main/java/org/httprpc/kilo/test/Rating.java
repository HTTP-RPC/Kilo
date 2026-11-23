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

import org.httprpc.kilo.Name;
import org.httprpc.kilo.web.service.Description;

@Description("Represents a film rating.")
public enum Rating {
    @Description("A \"G\" rating.") G,
    @Description("A \"PG\" rating.") PG,
    @Description("A \"PG-13\" rating.") @Name("PG-13") PG_13,
    @Description("An \"R\" rating.") R,
    @Description("An \"NC-17\" rating.") @Name("NC-17") NC_17;
}
