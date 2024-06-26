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

@Table("item")
@Description("Represents an item in the catalog.")
public interface Item {
    @Name("id")
    @Column("id")
    @PrimaryKey
    @Description("The item's ID.")
    Integer getID();
    void setID(Integer id);

    @Column("description")
    @Index
    @Description("The item's description.")
    @Required
    String getDescription();
    void setDescription(String description);

    @Column("price")
    @Description("The item's price.")
    @Required
    Double getPrice();
    void setPrice(Double price);
}
