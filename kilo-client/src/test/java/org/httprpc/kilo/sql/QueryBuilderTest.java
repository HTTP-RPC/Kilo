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

import static org.httprpc.kilo.sql.PredicateComponent.allOf;
import static org.httprpc.kilo.sql.PredicateComponent.and;
import static org.httprpc.kilo.sql.PredicateComponent.anyOf;
import static org.httprpc.kilo.sql.PredicateComponent.or;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.A;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.B;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.C;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.D;
import static org.httprpc.kilo.sql.QueryBuilderTest.ASchema.E;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        D,
        @Column("e")
        E
    }

    @Table("B")
    public enum BSchema implements SchemaElement {
        @Column("id")
        ID,
        @Column("b")
        B
    }

    @Table("C")
    public enum CSchema implements SchemaElement {
        @Column("id")
        ID,
        @Column("c")
        C
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
            .join(BSchema.class).on(ASchema.ID.eq(BSchema.ID), and(ASchema.A.eq("a")))
            .leftJoin(CSchema.class).on(BSchema.ID.eq(CSchema.ID), and(BSchema.B.eq("b")))
            .rightJoin(DSchema.class).on(CSchema.ID.eq(DSchema.ID), and(CSchema.C.eq("c")))
            .where(A.gt("m"), or(B.lt("n")))
            .orderBy(A, B.asc(), C.desc())
            .limit(10)
            .forUpdate()
            .union(QueryBuilder.select(A, B, C, D).from(ASchema.class).where(C.eq("z")));

        assertEquals(listOf("a", "b", "c", "m", "n", "z"), queryBuilder.getParameters());

        assertEquals("select A.a as x, A.b, A.c, A.d "
            + "from A "
            + "join B on A.id = B.id and A.a = ? "
            + "left join C on B.id = C.id and B.b = ? "
            + "right join D on C.id = D.id and C.c = ? "
            + "where A.a > ? or A.b < ? "
            + "order by A.a, A.b asc, A.c desc "
            + "limit 10 "
            + "for update "
            + "union select A.a, A.b, A.c, A.d from A where A.c = ?", queryBuilder.toString());
    }

    @Test
    public void testSelectAll() {
        var queryBuilder = QueryBuilder.select(SchemaElement.all(ASchema.class), BSchema.B)
            .from(ASchema.class)
            .join(BSchema.class).on(ASchema.ID.eq(BSchema.ID));

        assertEquals("select A.*, B.b "
            + "from A "
            + "join B on A.id = B.id", queryBuilder.toString());
    }

    @Test
    public void testGroupBy() {
        var queryBuilder = QueryBuilder.select(A, B.avg().as("y"))
            .from(ASchema.class)
            .groupBy(A)
            .having(C.gt("c"), and(D.like("d")));

        assertEquals(listOf("c", "d"), queryBuilder.getParameters());

        assertEquals("select A.a, avg(A.b) as y "
            + "from A "
            + "group by A.a "
            + "having A.c > ? and A.d like ?", queryBuilder.toString());
    }

    @Test
    public void testInsertInto() {
        var queryBuilder = QueryBuilder.insertInto(ASchema.class, A, B, C)
            .values("a", "b", "c");

        assertEquals(listOf("a", "b", "c"), queryBuilder.getParameters());

        assertEquals("insert into A (a, b, c) values (?, ?, ?)", queryBuilder.toString());
    }

    @Test
    public void testUpdate() {
        var queryBuilder = QueryBuilder.update(ASchema.class, A, B, C.optional())
            .set("a", "b", "c")
            .where(A.gt("m"), and(B.isNull()));

        assertEquals(listOf("a", "b", "c", "m"), queryBuilder.getParameters());

        assertEquals("update A set a = ?, b = ?, c = coalesce(?, c) where A.a > ? and A.b is null", queryBuilder.toString());
    }

    @Test
    public void testDeleteFrom() {
        var queryBuilder = QueryBuilder.deleteFrom(ASchema.class)
            .where(A.lt("x"), and(B.isNotNull()));

        assertEquals(listOf("x"), queryBuilder.getParameters());

        assertEquals("delete from A "
            + "where A.a < ? and A.b is not null", queryBuilder.toString());
    }

    @Test
    public void testConditionalGroups() {
        var queryBuilder = QueryBuilder.selectAll()
            .from(ASchema.class)
            .where(allOf(A.eq("a"), B.ne("b"), C.lt("c")), and(anyOf(D.gt("d"), E.notIn("m", "n"))));

        assertEquals(listOf("a", "b", "c", "d", "m", "n"), queryBuilder.getParameters());

        assertEquals("select * "
            + "from A "
            + "where (A.a = ? and A.b != ? and A.c < ?) and (A.d > ? or A.e not in (?, ?))", queryBuilder.toString());
    }

    @Test
    public void testWhereExists() {
        var queryBuilder = QueryBuilder.selectAll()
            .from(BSchema.class)
            .whereExists(QueryBuilder.select(CSchema.C)
                .from(CSchema.class)
                .where(CSchema.ID.eq(BSchema.ID), and(CSchema.C.eq("c"))));

        assertEquals(listOf("c"), queryBuilder.getParameters());

        assertEquals("select * from B "
            + "where exists (select C.c from C where C.id = B.id and C.c = ?)", queryBuilder.toString());
    }

    @Test
    public void testWhereNotExists() {
        var queryBuilder = QueryBuilder.selectAll()
            .from(BSchema.class)
            .whereNotExists(QueryBuilder.select(CSchema.C)
                .from(CSchema.class)
                .where(CSchema.ID.eq(BSchema.ID), and(CSchema.C.eq("c"))));

        assertEquals(listOf("c"), queryBuilder.getParameters());

        assertEquals("select * from B "
            + "where not exists (select C.c from C where C.id = B.id and C.c = ?)", queryBuilder.toString());
    }

    @Test
    public void testAppend() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select a, 'b''c:d' as b from foo\n");
        queryBuilder.append("where bar = :x\n");

        assertEquals(listOf("x"), queryBuilder.getParameters());

        assertEquals("select a, 'b''c:d' as b from foo\nwhere bar = ?\n", queryBuilder.toString());
    }

    @Test
    public void testQuotedQuestionMark() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select '?' as q from xyz");

        assertTrue(queryBuilder.getParameters().isEmpty());

        assertEquals("select '?' as q from xyz", queryBuilder.toString());
    }

    @Test
    public void testEscapedQuotes() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''");

        assertTrue(queryBuilder.getParameters().isEmpty());

        assertEquals("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''", queryBuilder.toString());
    }

    @Test
    public void testDoubleColon() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.append("select 'ab:c'::varchar(16) as abc"));
    }

    @Test
    public void testMissingParameterName() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.append("select * from xyz where foo = : and bar is null"));
    }
}
