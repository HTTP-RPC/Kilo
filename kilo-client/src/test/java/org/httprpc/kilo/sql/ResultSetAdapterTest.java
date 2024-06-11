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
        var list = listOf(1, 2, 3);

        var id = insertJSONTest(list);

        assertEquals(list, selectJSONTest(id).getValue());

        var map = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        updateJSONTest(id, map);

        assertEquals(map, selectJSONTest(id).getValue());
    }

    @Test
    public void testJSONNull() {
        var exception = assertThrows(SQLException.class, () -> insertJSONTest(null));

        assertEquals(INTEGRITY_CONSTRAINT_VIOLATION_CODE, exception.getSQLState());
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

    private void updateJSONTest(int id, Object value) throws SQLException {
        var queryBuilder = QueryBuilder.update(JSONTest.class).filterByPrimaryKey("id");

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("value", value),
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

        assertTrue(document1.isEqualNode(selectXMLTest(id).getDocument()));

        Document document2;
        try (var inputStream = getClass().getResourceAsStream("test2.xml")) {
            document2 = documentBuilder.parse(inputStream);
        }

        updateXMLTest(id, document2);

        assertTrue(document2.isEqualNode(selectXMLTest(id).getDocument()));
    }

    @Test
    public void testXMLNull() {
        var exception = assertThrows(SQLException.class, () -> insertXMLTest(null));

        assertEquals(INTEGRITY_CONSTRAINT_VIOLATION_CODE, exception.getSQLState());
    }

    private int insertXMLTest(Document document) throws SQLException {
        var queryBuilder = QueryBuilder.insert(XMLTest.class);

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("document", document)
            ));
        }

        return queryBuilder.getGeneratedKey(0, Integer.class);
    }

    private void updateXMLTest(int id, Document document) throws SQLException {
        var queryBuilder = QueryBuilder.update(XMLTest.class);

        try (var statement = queryBuilder.prepare(getConnection())) {
            queryBuilder.executeUpdate(statement, mapOf(
                entry("document", document),
                entry("id", id)
            ));
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
