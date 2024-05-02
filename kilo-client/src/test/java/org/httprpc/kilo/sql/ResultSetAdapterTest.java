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
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResultSetAdapterTest {
    @Test
    public void testTemporalAccessors() throws SQLException {
        var date = LocalDate.now();
        var time = LocalTime.now();
        var instant = Instant.now();

        var id = insertTemporalAccessorTest(date, time, instant);

        var temporalAccessorTest = selectTemporalAccessorTest(id);

        assertEquals(id, temporalAccessorTest.getID());
        assertEquals(date, temporalAccessorTest.getDate());
        assertEquals(time.truncatedTo(ChronoUnit.SECONDS), temporalAccessorTest.getTime());
        assertEquals(instant, temporalAccessorTest.getInstant());
    }

    private int insertTemporalAccessorTest(LocalDate date, LocalTime time, Instant instant) throws SQLException {
        var queryBuilder = QueryBuilder.insert(TemporalAccessorTest.class);

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

    private TemporalAccessorTest selectTemporalAccessorTest(int id) throws SQLException {
        var queryBuilder = QueryBuilder.select(TemporalAccessorTest.class).filterByPrimaryKey("id");

        try (var connection = getConnection();
            var statement = queryBuilder.prepare(connection);
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("id", id)
            ))) {
            return results.stream().findFirst().map(result -> BeanAdapter.coerce(result, TemporalAccessorTest.class)).orElseThrow();
        }
    }

    @Test
    public void testJSONArray() throws SQLException {
        var list = listOf(1, 2, 3);

        var id = insertJSONTest(list);

        var jsonTest = selectJSONTest(id);

        assertEquals(list, jsonTest.getValue());
    }

    @Test
    public void testJSONObject() throws SQLException {
        var map = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        var id = insertJSONTest(map);

        var jsonTest = selectJSONTest(id);

        assertEquals(map, jsonTest.getValue());
    }

    private int insertJSONTest(Object value) throws SQLException {
        var queryBuilder = QueryBuilder.insert(JSONTest.class);

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("value", value)
            ));
        }

        return queryBuilder.getGeneratedKey(0, Integer.class);
    }

    private JSONTest selectJSONTest(int id) throws SQLException {
        var queryBuilder = QueryBuilder.select(JSONTest.class).filterByPrimaryKey("id");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("id", id)
            ))) {
            return results.stream().findFirst().map(result -> BeanAdapter.coerce(result, JSONTest.class)).orElseThrow();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://db.local:3306/demo", "demo", "demo123!");
    }
}
