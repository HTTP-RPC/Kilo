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
import org.httprpc.kilo.Name;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Index;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

import java.time.LocalDate;

@Table("film")
@Description("Represents a film.")
public interface Film {
    @Name("id")
    @Column("film_id")
    @PrimaryKey
    @Description("The film's ID.")
    Integer getID();

    @Column("title")
    @Index
    @Description("The title of the film.")
    @Required
    String getTitle();

    @Column("rating")
    @Description("The film's rating.")
    Rating getRating();

    @Column("release_year")
    @Description("The date the film was released.")
    LocalDate getReleaseDate();
}
