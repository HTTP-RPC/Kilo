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

import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.ForeignKey;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

import java.time.LocalDate;

@Table("pet")
public interface Pet {
    @Column("name")
    @PrimaryKey
    @Index
    String getName();

    @Column("owner")
    @ForeignKey(Owner.class)
    String getOwner();

    @Column("species")
    String getSpecies();

    @Column("sex")
    String getSex();

    @Column("birth")
    LocalDate getBirth();

    @Column("death")
    LocalDate getDeath();
}
