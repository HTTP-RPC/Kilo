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

package org.httprpc.sql;

import org.junit.jupiter.api.Test;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryBuilderTest {
    @Test
    public void testSelect() {
        QueryBuilder queryBuilder = QueryBuilder.select(":a as a", "b", "c", "d")
            .from("A")
            .join("B").on("A.id = B.id and x = 50")
            .leftJoin("C").on("B.id = C.id and b = :b")
            .rightJoin("D").on("C.id = D.id and c = :c")
            .where("(a > 10 or b < 200) and d != ?")
            .orderBy("a", "b")
            .limit(10)
            .forUpdate()
            .union(QueryBuilder.select("a", "b", "c", "d").from("C").where("c = :c"));

        assertEquals(listOf("a", "b", "c", null, "c"), queryBuilder.getParameters());

        assertEquals("select ? as a, b, c, d from A "
            + "join B on A.id = B.id and x = 50 "
            + "left join C on B.id = C.id and b = ? "
            + "right join D on C.id = D.id and c = ? "
            + "where (a > 10 or b < 200) and d != ? "
            + "order by a, b "
            + "limit 10 "
            + "for update "
            + "union select a, b, c, d from C where c = ?", queryBuilder.getSQL());
    }

    @Test
    public void testInsertInto() {
        QueryBuilder queryBuilder = QueryBuilder.insertInto("A").values(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", "hello"),
            entry("d", ":d"),
            entry("e", "?"),
            entry("f", QueryBuilder.select("f").from("F").where("g = :g"))
        ));

        assertEquals(listOf("d", null, "g"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c, d, e, f) values (1, true, 'hello', ?, ?, (select f from F where g = ?))", queryBuilder.getSQL());
    }

    @Test
    public void testUpdate() {
        QueryBuilder queryBuilder = QueryBuilder.update("A").set(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", "hello"),
            entry("d", ":d"),
            entry("e", "?"),
            entry("f", QueryBuilder.select("f").from("F").where("g = :g"))
        )).where("a is not null");

        assertEquals(listOf("d", null, "g"), queryBuilder.getParameters());

        assertEquals("update A set a = 1, b = true, c = 'hello', d = ?, e = ?, f = (select f from F where g = ?) where a is not null", queryBuilder.getSQL());
    }

    @Test
    public void testDelete() {
        QueryBuilder queryBuilder = QueryBuilder.deleteFrom("A").where("a < 150");

        assertEquals("delete from A where a < 150", queryBuilder.getSQL());
    }

    @Test
    public void testExistingSQL() {
        QueryBuilder queryBuilder = new QueryBuilder("select a, 'b''c:d' as b from foo where bar = :x");

        assertEquals(listOf("x"), queryBuilder.getParameters());

        assertEquals("select a, 'b''c:d' as b from foo where bar = ?", queryBuilder.getSQL());
    }

    @Test
    public void testQuotedColon() {
        QueryBuilder queryBuilder = QueryBuilder.select("*").from("xyz").where("foo = 'a:b:c'");

        assertEquals("select * from xyz where foo = 'a:b:c'", queryBuilder.getSQL());
    }

    @Test
    public void testQuotedQuestionMark() {
        QueryBuilder queryBuilder = QueryBuilder.select("'?' as q").from("xyz");

        assertEquals("select '?' as q from xyz", queryBuilder.getSQL());
    }

    @Test
    public void testDoubleColon() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.select("'ab:c'::varchar(16) as abc"));
    }

    @Test
    public void testEscapedQuotes() {
        QueryBuilder queryBuilder = QueryBuilder.select("xyz.*", "''':z' as z").from("xyz").where("foo = 'a''b'':c''' and bar = ''''");

        assertEquals("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''", queryBuilder.getSQL());
    }

    @Test
    public void testMissingPredicateParameterName() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.select("*").from("xyz").where("foo = :"));
    }

    @Test
    public void testMissingValueParameterName() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.insertInto("xyz").values(mapOf(
            entry("foo", ":")
        )));
    }

    @Test
    public void testInvalidValueParameterName() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.insertInto("xyz").values(mapOf(
            entry("foo", ":a + b")
        )));
    }

    @Test
    public void testToString() {
        QueryBuilder queryBuilder = QueryBuilder.select("*").from("xyz").where("foo = :a and bar = :b");

        assertEquals("select * from xyz where foo = :a and bar = :b", queryBuilder.toString());
    }
}
