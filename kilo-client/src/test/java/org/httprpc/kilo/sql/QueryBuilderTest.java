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

import org.junit.jupiter.api.Test;

import static org.httprpc.kilo.sql.QueryBuilder.allOf;
import static org.httprpc.kilo.sql.QueryBuilder.and;
import static org.httprpc.kilo.sql.QueryBuilder.anyOf;
import static org.httprpc.kilo.sql.QueryBuilder.equalTo;
import static org.httprpc.kilo.sql.QueryBuilder.exists;
import static org.httprpc.kilo.sql.QueryBuilder.in;
import static org.httprpc.kilo.sql.QueryBuilder.notEqualTo;
import static org.httprpc.kilo.sql.QueryBuilder.notExists;
import static org.httprpc.kilo.sql.QueryBuilder.notIn;
import static org.httprpc.kilo.sql.QueryBuilder.or;
import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryBuilderTest {
    @Test
    public void testSelect() {
        var queryBuilder = QueryBuilder.select(":a as a", "'b' as b", "c", "d")
            .from("A")
            .join("B").on("A.id = B.id", and("x = 50"))
            .leftJoin("C").on("B.id = C.id", and("b = :b"))
            .rightJoin("D").on("C.id = D.id", and("c = :c"))
            .where("a > 10", or("b < 200", and("d != ?")))
            .orderBy("a", "b")
            .limit(10)
            .forUpdate()
            .union(QueryBuilder.select("a", "b", "c", "d").from("C").where("c = :c"));

        assertEquals(listOf("a", "b", "c", null, "c"), queryBuilder.getParameters());

        assertEquals("select ? as a, 'b' as b, c, d from A "
            + "join B on A.id = B.id and x = 50 "
            + "left join C on B.id = C.id and b = ? "
            + "right join D on C.id = D.id and c = ? "
            + "where a > 10 or (b < 200 and d != ?) "
            + "order by a, b "
            + "limit 10 "
            + "for update "
            + "union select a, b, c, d from C where c = ?", queryBuilder.getSQL());
    }

    @Test
    public void testSelectWithSubquery() {
        var queryBuilder = QueryBuilder.select("*")
            .from(QueryBuilder.select("a, b, c").from("A"), "a")
            .where("d = :d");

        assertEquals(listOf("d"), queryBuilder.getParameters());

        assertEquals("select * from (select a, b, c from A) a where d = ?", queryBuilder.getSQL());
    }

    @Test
    public void testSelectWithSubqueryJoin() {
        var queryBuilder = QueryBuilder.select("*").from("A")
            .join(QueryBuilder.select("c, d").from("C"), "c").on("c = :c")
            .where("d = :d");

        assertEquals(listOf("c", "d"), queryBuilder.getParameters());

        assertEquals("select * from A join (select c, d from C) c on c = ? where d = ?", queryBuilder.getSQL());
    }

    @Test
    public void testGroupBy() {
        var queryBuilder = QueryBuilder.select("a", "avg(b) as c").from("A").groupBy("b").having("d > 10", and("e like :e"));

        assertEquals(listOf("e"), queryBuilder.getParameters());

        assertEquals("select a, avg(b) as c from A group by b having d > 10 and e like ?", queryBuilder.getSQL());
    }

    @Test
    public void testInsertInto() {
        var queryBuilder = QueryBuilder.insertInto("A").values(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", "'hello'"),
            entry("d", ":d"),
            entry("e", "?"),
            entry("f", QueryBuilder.select("f").from("F").where("g = :g"))
        ));

        assertEquals(listOf("d", null, "g"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c, d, e, f) values (1, true, 'hello', ?, ?, (select f from F where g = ?))", queryBuilder.getSQL());
    }

    @Test
    public void testSelectInto() {
        var queryBuilder = QueryBuilder.select("a, b").from("A")
            .where("a = :a", and("b = :b"))
            .into("B", "c", "d");

        assertEquals(listOf("a", "b"), queryBuilder.getParameters());

        assertEquals("insert into B (c, d) select a, b from A where a = ? and b = ?", queryBuilder.getSQL());
    }

    @Test
    public void testOnDuplicateKeyUpdate1() {
        var queryBuilder = QueryBuilder.insertInto("A").values(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", ":c")
        )).onDuplicateKeyUpdate("b", "c");

        assertEquals(listOf("c"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c) values (1, true, ?) on duplicate key update b = value(b), c = value(c)", queryBuilder.getSQL());
    }

    @Test
    public void testOnDuplicateKeyUpdate2() {
        var queryBuilder = QueryBuilder.insertInto("A").values(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", ":c")
        )).onDuplicateKeyUpdate(mapOf(
            entry("b", false),
            entry("c", ":d")
        ));

        assertEquals(listOf("c", "d"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c) values (1, true, ?) on duplicate key update b = false, c = ?", queryBuilder.getSQL());
    }

    @Test
    public void testUpdate() {
        var queryBuilder = QueryBuilder.update("A").set(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", "'hello'"),
            entry("d", ":d"),
            entry("e", "?"),
            entry("f", QueryBuilder.select("f").from("F").where("g = :g"))
        )).where("a is not null");

        assertEquals(listOf("d", null, "g"), queryBuilder.getParameters());

        assertEquals("update A set a = 1, b = true, c = 'hello', d = ?, e = ?, f = (select f from F where g = ?) where a is not null", queryBuilder.getSQL());
    }

    @Test
    public void testUpdateWithExpression() {
        var queryBuilder = QueryBuilder.update("xyz").set(mapOf(
            entry("foo", ":a + b")
        )).where("c = :d");

        assertEquals(listOf("a", "d"), queryBuilder.getParameters());

        assertEquals("update xyz set foo = ? + b where c = ?", queryBuilder.getSQL());
    }

    @Test
    public void testDeleteFrom() {
        var queryBuilder = QueryBuilder.deleteFrom("A").where("a < 150");

        assertEquals("delete from A where a < 150", queryBuilder.getSQL());
    }

    @Test
    public void testWith() {
        var queryBuilder = QueryBuilder.select("b", "d").from("cte1").join("cte2")
            .where("cte1.a = cte2.c")
            .with(mapOf(
                entry("cte1", QueryBuilder.select("a", ":b as b").from("table1")),
                entry("cte2", QueryBuilder.select("c", ":d as d").from("table2"))
            ));

        assertEquals(listOf("b", "d"), queryBuilder.getParameters());

        assertEquals("with cte1 as (select a, ? as b from table1), "
            + "cte2 as (select c, ? as d from table2) "
            + "select b, d from cte1 join cte2 "
            + "where cte1.a = cte2.c", queryBuilder.getSQL());
    }

    @Test
    public void testCoalesceWithInsert() {
        var queryBuilder = QueryBuilder.insertInto("A").values(mapOf(
            entry("a", 1),
            entry("b", listOf(":b", 0)),
            entry("c", listOf(":c", "c"))
        ));

        assertEquals(listOf("b", "c"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c) values (1, coalesce(?, 0), coalesce(?, c))", queryBuilder.getSQL());
    }

    @Test
    public void testCoalesceWithUpdate() {
        var queryBuilder = QueryBuilder.update("A").set(mapOf(
            entry("a", 1),
            entry("b", listOf(":b", 0)),
            entry("c", listOf(":c", "c"))
        ));

        assertEquals(listOf("b", "c"), queryBuilder.getParameters());

        assertEquals("update A set a = 1, b = coalesce(?, 0), c = coalesce(?, c)", queryBuilder.getSQL());
    }

    @Test
    public void testConditionalGroups() {
        var queryBuilder = QueryBuilder.select("*").from("xyz").where(allOf("a = 1", "b = 2", "c = 3"), and(anyOf("d = 4", "e = 5")));

        assertEquals("select * from xyz where (a = 1 and b = 2 and c = 3) and (d = 4 or e = 5)", queryBuilder.getSQL());
    }

    @Test
    public void testEqualToConditional() {
        var queryBuilder = QueryBuilder.select("*")
            .from("A")
            .where("b", equalTo(
                QueryBuilder.select("b").from("B").where("c = :c")
            ));

        assertEquals("select * from A where b = (select b from B where c = ?)", queryBuilder.getSQL());
    }

    @Test
    public void testNotEqualToConditional() {
        var queryBuilder = QueryBuilder.select("*")
            .from("A")
            .where("b", notEqualTo(
                QueryBuilder.select("b").from("B").where("c = :c")
            ));

        assertEquals("select * from A where b != (select b from B where c = ?)", queryBuilder.getSQL());
    }

    @Test
    public void testInConditional() {
        var queryBuilder = QueryBuilder.select("*")
            .from("B")
            .where("c", in(
                QueryBuilder.select("c").from("C").where("d = :d")
            ));

        assertEquals("select * from B where c in (select c from C where d = ?)", queryBuilder.getSQL());
    }

    @Test
    public void testNotInConditional() {
        var queryBuilder = QueryBuilder.select("*").from("D").where("e", notIn(
                QueryBuilder.select("e").from("E")
            ));

        assertEquals("select * from D where e not in (select e from E)", queryBuilder.getSQL());
    }

    @Test
    public void testExistsConditional() {
        var queryBuilder = QueryBuilder.select("*")
            .from("B")
            .where(exists(
                QueryBuilder.select("c").from("C").where("d = :d")
            ));

        assertEquals("select * from B where exists (select c from C where d = ?)", queryBuilder.getSQL());
    }

    @Test
    public void testNotExistsConditional() {
        var queryBuilder = QueryBuilder.select("*").from("D").where("e", notExists(
            QueryBuilder.select("e").from("E")
        ));

        assertEquals("select * from D where e not exists (select e from E)", queryBuilder.getSQL());
    }

    @Test
    public void testQuotedColon() {
        var queryBuilder = QueryBuilder.select("*").from("xyz").where("foo = 'a:b:c'");

        assertEquals("select * from xyz where foo = 'a:b:c'", queryBuilder.getSQL());
    }

    @Test
    public void testQuotedQuestionMark() {
        var queryBuilder = QueryBuilder.select("'?' as q").from("xyz");

        assertEquals("select '?' as q from xyz", queryBuilder.getSQL());
    }

    @Test
    public void testDoubleColon() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.select("'ab:c'::varchar(16) as abc"));
    }

    @Test
    public void testEscapedQuotes() {
        var queryBuilder = QueryBuilder.select("xyz.*", "''':z' as z").from("xyz").where("foo = 'a''b'':c'''", and("bar = ''''"));

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
    public void testExistingSQL() {
        var queryBuilder = new QueryBuilder("select a, 'b''c:d' as b from foo where bar = :x");

        assertEquals(listOf("x"), queryBuilder.getParameters());

        assertEquals("select a, 'b''c:d' as b from foo where bar = ?", queryBuilder.getSQL());
    }

    @Test
    public void testToString() {
        var queryBuilder = QueryBuilder.select("*").from("xyz").where("foo = :a", and("bar = :b", or("bar = :c")));

        assertEquals("select * from xyz where foo = :a and (bar = :b or bar = :c)", queryBuilder.toString());
    }
}
