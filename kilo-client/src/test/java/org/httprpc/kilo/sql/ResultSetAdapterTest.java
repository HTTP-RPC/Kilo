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

package org.httprpc.kilo.sql;

import org.httprpc.kilo.beans.BeanAdapter;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResultSetAdapterTest {
    @Test
    public void testTemporalAccessors() throws SQLException {
        var date = LocalDate.now();
        var time = LocalTime.now();
        var instant = Instant.now();

        var id = insertTemporalAccessors(date, time, instant);

        var temporalAccessors = selectTemporalAccessors(id);

        assertEquals(date, temporalAccessors.getDate());
        assertEquals(time.truncatedTo(ChronoUnit.SECONDS), temporalAccessors.getTime());
        assertEquals(instant, temporalAccessors.getInstant());
    }

    private int insertTemporalAccessors(LocalDate date, LocalTime time, Instant instant) throws SQLException {
        var queryBuilder = QueryBuilder.insert(TemporalAccessors.class);

        try (var connection = getConnection();
            var statement = queryBuilder.prepare(connection)) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("date", date),
                entry("time", time),
                entry("instant", instant)
            ));
        }

        return queryBuilder.getGeneratedKey(0, Integer.class);
    }

    private TemporalAccessors selectTemporalAccessors(int id) throws SQLException {
        var queryBuilder = QueryBuilder.select(TemporalAccessors.class).filterByPrimaryKey("id");

        try (var connection = getConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("id", id)
            ))) {
            return results.stream().findFirst().map(result -> BeanAdapter.coerce(result, TemporalAccessors.class)).orElseThrow();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://db.local:3306/demo", "demo", "demo123!");
    }
}
