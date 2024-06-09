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
import org.httprpc.kilo.sql.Table;

import java.time.LocalDate;
import java.util.List;

@Table("employees")
public interface EmployeeDetails extends Employee {
    @Table("titles")
    interface Title {
        @Column("emp_no")
        @ForeignKey(Employee.class)
        Integer getEmployeeNumber();
        @Column("title")
        String getTitle();
        @Column("from_date")
        LocalDate getFromDate();
        @Column("to_date")
        LocalDate getToDate();
    }

    @Table("salaries")
    interface Salary {
        @Column("emp_no")
        @ForeignKey(Employee.class)
        Integer getEmployeeNumber();
        @Column("salary")
        String getTitle();
        @Column("from_date")
        LocalDate getFromDate();
        @Column("to_date")
        LocalDate getToDate();
    }

    List<Title> getTitles();
    void setTitles(List<Title> titles);

    List<Salary> getSalaries();
    void setSalaries(List<Salary> salaries);
}
