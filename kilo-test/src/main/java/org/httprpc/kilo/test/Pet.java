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
import org.httprpc.kilo.sql.SchemaElement;
import org.httprpc.kilo.sql.Table;

import java.util.Date;

public interface Pet {
    @Table("pet")
    enum Schema implements SchemaElement {
        @Column("name")
        NAME,
        @Column("owner")
        OWNER,
        @Column("species")
        SPECIES,
        @Column("sex")
        SEX,
        @Column("birth")
        BIRTH,
        @Column("death")
        DEATH
    }

    String getName();
    String getOwner();
    String getSpecies();
    String getSex();
    Date getBirth();
    Date getDeath();
}
