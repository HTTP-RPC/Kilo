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

import jakarta.servlet.annotation.WebServlet;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.WebService;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

@WebServlet(urlPatterns = {"/salaries/*"}, loadOnStartup = 1)
public class SalaryService extends WebService {
    @Override
    protected String getDataSourceName() {
        return "java:comp/env/jdbc/EmployeeDB";
    }

    @RequestMethod("GET")
    @ResourcePath("average")
    public Double getAverageSalary(LocalDate fromDate, LocalDate toDate) throws SQLException {
        fromDate = coalesce(fromDate, () -> LocalDate.of(1900, 1, 1));
        toDate = coalesce(toDate, () -> LocalDate.of(9999, 1, 1));

        var queryBuilder = QueryBuilder.select(Salary.class);

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement)) {
            var employeeSalaries = filter(mapAll(results, BeanAdapter.toType(Salary.class)),
                whereGreaterThanOrEqualTo(Salary::fromDate, fromDate)
                    .and(whereLessThanOrEqualTo(Salary::toDate, toDate)));

            var average = averageOf(employeeSalaries, Salary::salary);

            return Double.isNaN(average) ? null : Math.round(average * 100) / 100.0;
        }
    }
}
