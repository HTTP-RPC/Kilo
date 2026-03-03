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
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.ForeignKey;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

import java.time.LocalDate;

@Table("pet")
@Description("Represents a pet.")
public interface Pet {
    @Column("name")
    @PrimaryKey
    @Index
    @Description("The pet's name.")
    String getName();

    @Column("owner")
    @ForeignKey(Owner.class)
    @Description("The pet's owner.")
    String getOwner();

    @Column("species")
    @Description("The pet's species.")
    String getSpecies();

    @Column("sex")
    @Description("The pet's gender.")
    String getSex();

    @Column("birth")
    @Description("The pet's date of birth.")
    LocalDate getBirth();

    @Column("death")
    @Description("The pet's date of death.")
    LocalDate getDeath();
}
