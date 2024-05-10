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

package org.httprpc.kilo.sql;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.Required;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Table("temporal_accessor_test")
public interface TemporalAccessorTest {
    @Name("id")
    @Column("id")
    @PrimaryKey
    Integer getID();
    @Column("local_date")
    @Required
    LocalDate getDate();
    @Column("local_time")
    @Required
    LocalTime getTime();
    @Column("instant")
    @Required
    Instant getInstant();
}
