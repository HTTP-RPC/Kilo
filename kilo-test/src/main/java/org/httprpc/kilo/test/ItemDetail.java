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
import org.httprpc.kilo.sql.Final;

import java.util.Date;

@Description("Represents detailed information about an item in the catalog.")
public interface ItemDetail extends Item {
    @Column("size")
    @Description("The item's size.")
    Size getSize();
    void setSize(Size size);

    @Column("color")
    @Description("The item's color.")
    String getColor();
    void setColor(String color);

    @Column("weight")
    @Description("The item's weight.")
    Double getWeight();
    void setWeight(Double weight);

    @Column("created")
    @Final
    @Description("The date the item was created.")
    Date getCreated();
    void setCreated(Date created);
}
