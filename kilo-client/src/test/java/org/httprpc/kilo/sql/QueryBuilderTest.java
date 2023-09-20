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

import static org.httprpc.kilo.sql.Conditionals.allOf;
import static org.httprpc.kilo.sql.Conditionals.and;
import static org.httprpc.kilo.sql.Conditionals.anyOf;
import static org.httprpc.kilo.sql.Conditionals.or;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.A;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.B;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.C;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.D;
import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryBuilderTest {
    @Table("A")
    public enum ASchema implements SchemaElement {
        @Column("id")
        ID,
        @Column("a")
        A,
        @Column("b")
        B,
        @Column("c")
        C,
        @Column("d")
        D
    }

    @Table("B")
    public enum BSchema implements SchemaElement {
        @Column("id")
        ID
    }

    @Table("C")
    public enum CSchema implements SchemaElement {
        @Column("id")
        ID
    }

    @Table("D")
    public enum DSchema implements SchemaElement {
        @Column("id")
        ID
    }

    @Test
    public void testSelect() {
        var queryBuilder = QueryBuilder.select(A.as("x"), B, C, D)
            .from(ASchema.class)
            .join(BSchema.class).on(ASchema.ID.eq(BSchema.ID), PredicateComponent.and(ASchema.A.eq("a")))
            .leftJoin(CSchema.class).on("B.id = C.id", and("b = :b"))
            .rightJoin(DSchema.class).on("C.id = D.id", and("c = :c"))
            .where("a > 10", or("b < 200"), or("d != ?"))
            .orderBy("a", "b")
            .limit(10)
            .forUpdate()
            .union(QueryBuilder.select("a", "b", "c", "d").from("C").where("c = :c"));

        assertEquals(listOf("a", "b", "c", null, "c"), queryBuilder.getParameters());

        assertEquals("select a as x, b, c, d "
            + "from A "
            + "join B on A.id = B.id and a = :a "
            + "left join C on B.id = C.id and b = ? "
            + "right join D on C.id = D.id and c = ? "
            + "where a > 10 or b < 200 or d != ? "
            + "order by a, b "
            + "limit 10 "
            + "for update "
            + "union select a, b, c, d from C where c = ?", queryBuilder.toString());
    }

    @Test
    public void testGroupBy() {
        var queryBuilder = QueryBuilder.select(A, B.avg().as("c"))
            .from(ASchema.class)
            .groupBy("b").having("d > 10", and("e like :e"));

        assertEquals(listOf("e"), queryBuilder.getParameters());

        assertEquals("select a, avg(b) as c from A group by b having d > 10 and e like ?", queryBuilder.toString());
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

        assertEquals("insert into A (a, b, c, d, e, f) values (1, true, 'hello', ?, ?, (select f from F where g = ?))", queryBuilder.toString());
    }

    @Test
    public void testOnDuplicateKeyUpdate() {
        var queryBuilder = QueryBuilder.insertInto("A").values(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", ":c")
        )).onDuplicateKeyUpdate(mapOf(
            entry("b", false),
            entry("c", ":d")
        ));

        assertEquals(listOf("c", "d"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c) values (1, true, ?) on duplicate key update b = false, c = ?", queryBuilder.toString());
    }

    @Test
    public void testUpdate() {
        var queryBuilder = QueryBuilder.update("A").set(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", "'hello'"),
            entry("d", ":d + 2"),
            entry("e", "?"),
            entry("f", QueryBuilder.select("f").from("F").where("g = :g"))
        )).where("a is not null");

        assertEquals(listOf("d", null, "g"), queryBuilder.getParameters());

        assertEquals("update A set a = 1, b = true, c = 'hello', d = ? + 2, e = ?, f = (select f from F where g = ?) where a is not null", queryBuilder.toString());
    }

    @Test
    public void testDeleteFrom() {
        var queryBuilder = QueryBuilder.deleteFrom("A").where("a < 150");

        assertEquals("delete from A where a < 150", queryBuilder.toString());
    }

    @Test
    public void testAppend() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select a, 'b''c:d' as b\n");
        queryBuilder.append("from foo where bar = :x\n");

        assertEquals(listOf("x"), queryBuilder.getParameters());

        assertEquals("select a, 'b''c:d' as b\nfrom foo where bar = ?\n", queryBuilder.toString());
    }

    @Test
    public void testCoalesceWithInsert() {
        var queryBuilder = QueryBuilder.insertInto("A").values(mapOf(
            entry("a", 1),
            entry("b", listOf(":b", 0)),
            entry("c", listOf(":c", "c"))
        ));

        assertEquals(listOf("b", "c"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c) values (1, coalesce(?, 0), coalesce(?, c))", queryBuilder.toString());
    }

    @Test
    public void testCoalesceWithUpdate() {
        var queryBuilder = QueryBuilder.update("A").set(mapOf(
            entry("a", 1),
            entry("b", listOf(":b", 0)),
            entry("c", listOf(":c", "c"))
        ));

        assertEquals(listOf("b", "c"), queryBuilder.getParameters());

        assertEquals("update A set a = 1, b = coalesce(?, 0), c = coalesce(?, c)", queryBuilder.toString());
    }

    @Test
    public void testConditionalGroups() {
        var queryBuilder = QueryBuilder.select("*").from("xyz").where(allOf("a = 1", "b = 2", "c = 3"), and(anyOf("d = 4", "e = 5")));

        assertEquals("select * from xyz where (a = 1 and b = 2 and c = 3) and (d = 4 or e = 5)", queryBuilder.toString());
    }

    @Test
    public void testWhereExists() {
        var queryBuilder = QueryBuilder.select("*")
            .from("B")
            .whereExists(QueryBuilder.select("c").from("C").where("d = :d"));

        assertEquals("select * from B where exists (select c from C where d = ?)", queryBuilder.toString());
    }

    @Test
    public void testWhereNotExists() {
        var queryBuilder = QueryBuilder.select("*")
            .from("D")
            .whereNotExists(QueryBuilder.select("e").from("E"));

        assertEquals("select * from D where not exists (select e from E)", queryBuilder.toString());
    }

    @Test
    public void testQuotedColon() {
        var queryBuilder = QueryBuilder.select("*").from("xyz").where("foo = 'a:b:c'");

        assertEquals("select * from xyz where foo = 'a:b:c'", queryBuilder.toString());
    }

    @Test
    public void testQuotedQuestionMark() {
        var queryBuilder = QueryBuilder.select("'?' as q").from("xyz");

        assertEquals("select '?' as q from xyz", queryBuilder.toString());
    }

    @Test
    public void testDoubleColon() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.select("'ab:c'::varchar(16) as abc"));
    }

    @Test
    public void testEscapedQuotes() {
        var queryBuilder = QueryBuilder.select("xyz.*", "''':z' as z").from("xyz").where("foo = 'a''b'':c'''", and("bar = ''''"));

        assertEquals("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''", queryBuilder.toString());
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
}
