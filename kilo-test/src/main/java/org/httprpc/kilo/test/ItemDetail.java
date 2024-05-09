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

import java.util.Date;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.Final;
import org.httprpc.kilo.sql.Table;

@Table("item")
@Description("Represents detailed information about an item in the catalog.")
public interface ItemDetail extends Item {
    @Description("The item's size.")
    @Column("size")
    Size getSize();
    void setSize(Size size);

    @Description("The item's color.")
    @Column("color")
    String getColor();
    void setColor(String color);

    @Description("The item's weight.")
    @Column("weight")
    Double getWeight();
    void setWeight(Double weight);

    @Column("created")
    @Description("The date the item was created.")
    @Final
    Date getCreated();
    void setCreated(Date created);
}
