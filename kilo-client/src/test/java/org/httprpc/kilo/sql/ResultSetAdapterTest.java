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
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class ResultSetAdapterTest {
    private static final String INTEGRITY_CONSTRAINT_VIOLATION_CODE = "23000";

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
        assertEquals(instant.truncatedTo(ChronoUnit.MICROS), temporalAccessorTest.getInstant());
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
    public void testJSON() throws SQLException {
        var list1 = listOf(1, 2, 3);

        var map1 = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        var record1 = new JSONTest.Record(1, 2, 3);

        var id = insertJSONTest(list1, map1, record1);

        var jsonTest1 = selectJSONTest(id);

        assertEquals(list1, jsonTest1.getList());
        assertEquals(map1, jsonTest1.getMap());
        assertEquals(record1, jsonTest1.getRecord());

        var list2 = listOf(4, 5, 6);

        var map2 = mapOf(
            entry("a", 4),
            entry("b", 5),
            entry("c", 6)
        );

        var record2 = new JSONTest.Record(4, 5, 6);

        updateJSONTest(id, list2, map2, record2);

        var jsonTest2 = selectJSONTest(id);

        assertEquals(list2, jsonTest2.getList());
        assertEquals(map2, jsonTest2.getMap());
        assertEquals(record1, jsonTest1.getRecord());
    }

    private int insertJSONTest(List<?> list, Map<String, ?> map, JSONTest.Record record) throws SQLException {
        var queryBuilder = QueryBuilder.insert(JSONTest.class);

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("list", list),
                entry("map", map),
                entry("record", record)
            ));
        }

        return queryBuilder.getGeneratedKey(0, Integer.class);
    }

    private void updateJSONTest(int id, List<?> list, Map<String, ?> map, JSONTest.Record record) throws SQLException {
        var queryBuilder = QueryBuilder.update(JSONTest.class).filterByPrimaryKey("id");

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("list", list),
                entry("map", map),
                entry("record", record),
                entry("id", id)
            ));
        }
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

    @Test
    public void testXML() throws Exception {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        var documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document1;
        try (var inputStream = getClass().getResourceAsStream("test1.xml")) {
            document1 = documentBuilder.parse(inputStream);
        }

        var id = insertXMLTest(document1);

        assertTrue(document1.isEqualNode(selectXMLTest(id).document()));

        Document document2;
        try (var inputStream = getClass().getResourceAsStream("test2.xml")) {
            document2 = documentBuilder.parse(inputStream);
        }

        updateXMLTest(id, document2);

        assertTrue(document2.isEqualNode(selectXMLTest(id).document()));
    }

    @Test
    public void testXMLNull() {
        var exception = assertThrows(SQLException.class, () -> insertXMLTest(null));

        assertEquals(INTEGRITY_CONSTRAINT_VIOLATION_CODE, exception.getSQLState());
    }

    private int insertXMLTest(Document document) throws SQLException {
        var queryBuilder = QueryBuilder.insert(XMLTest.class);

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(new XMLTest(null, document)));
        }

        return queryBuilder.getGeneratedKey(0, Integer.class);
    }

    private void updateXMLTest(int id, Document document) throws SQLException {
        var queryBuilder = QueryBuilder.update(XMLTest.class);

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, new BeanAdapter(new XMLTest(id, document)));
        }
    }

    private XMLTest selectXMLTest(int id) throws SQLException {
        var queryBuilder = QueryBuilder.select(XMLTest.class).filterByPrimaryKey("id");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("id", id)
            ))) {
            return results.stream().findFirst().map(result -> BeanAdapter.coerce(result, XMLTest.class)).orElseThrow();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://db.local:3306/demo", "demo", "demo123!");
    }
}
