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
import org.httprpc.kilo.Required;
import org.httprpc.kilo.sql.Column;
import org.httprpc.kilo.sql.PrimaryKey;
import org.httprpc.kilo.sql.Table;

import java.time.LocalDate;

@Table("employees")
@Description("Represents an employee.")
public interface Employee {
    @Column("emp_no")
    @PrimaryKey
    @Description("The employee number.")
    Integer getEmployeeNumber();

    @Column("first_name")
    @Required
    @Description("The employee's first name.")
    String getFirstName();

    @Column("last_name")
    @Required
    @Description("The employee's last name.")
    String getLastName();

    @Column("gender")
    @Required
    @Description("The employee's gender.")
    String getGender();

    @Column("birth_date")
    @Required
    @Description("The employee's birth date.")
    LocalDate getBirthDate();

    @Column("hire_date")
    @Required
    @Description("The employee's hire date.")
    LocalDate getHireDate();
}
