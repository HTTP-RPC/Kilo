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

import org.httprpc.kilo.Name;
import org.httprpc.kilo.Required;
import org.junit.jupiter.api.Test;

import static org.httprpc.kilo.util.Collections.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryBuilderTest {
    @Table("A")
    public interface A {
        @Column("a")
        @PrimaryKey
        String getA();
        @Column("b")
        @Required
        Double getB();
        @Column("c")
        @Required
        Boolean getC();
        @Name("x")
        @Column("d")
        Boolean getD();
    }

    @Table("B")
    public interface B extends A {
        @Column("e")
        Double getE();
        @Name("y")
        @Column("f")
        Boolean getF();
        Integer getG();
    }

    @Table("C")
    public record C(
        @Column("a")
        @ForeignKey(A.class)
        String a,
        @Column("b")
        Double b,
        @Column("c")
        Boolean c
    ) {
    }

    @Table("D")
    public interface D {
        @Column("d")
        @PrimaryKey
        String getD();
        @Column("e")
        String getE();
    }

    @Table("E")
    public record E(
        @Column("a")
        @ForeignKey(A.class)
        String a,
        @Column("d")
        @ForeignKey(D.class)
        String d,
        @Column("z")
        Boolean z
    ) {
    }

    @Table("F")
    public interface F {
        @Column("f")
        @PrimaryKey
        Integer getF();
        @Column("q")
        String getQ();
    }

    @Table("G")
    public interface G {
        @Column("g")
        @PrimaryKey
        Integer getG();
        @Column("f")
        @ForeignKey(F.class)
        Integer getF();
        @Column("r")
        String getR();
    }

    @Table("H")
    public interface H {
        @Column("h")
        @PrimaryKey
        Integer getH();
        @Column("g")
        @ForeignKey(G.class)
        Integer getG();
        @Column("s")
        String getS();
    }

    @Table("I")
    public interface I {
        @Column("i")
        @PrimaryKey
        Integer getI();
        @Column("h")
        @ForeignKey(H.class)
        Integer getH();
        @Column("t")
        String getT();
    }

    @Test
    public void testSelectA() {
        var queryBuilder = QueryBuilder.select(A.class).wherePrimaryKeyEquals("a");

        assertEquals("select A.a, A.b, A.c, A.d as x from A\nwhere A.a = ?\n", queryBuilder.toString());
        assertEquals(listOf("a"), queryBuilder.getParameters());
    }

    @Test
    public void testSelectB() {
        var queryBuilder = QueryBuilder.select(B.class).wherePrimaryKeyEquals("a");

        assertEquals("select B.a, B.b, B.c, B.e, B.d as x, B.f as y from B\nwhere B.a = ?\n", queryBuilder.toString());
        assertEquals(listOf("a"), queryBuilder.getParameters());
    }

    @Test
    public void testSelectC() {
        var queryBuilder = QueryBuilder.select(C.class).whereForeignKeyEquals(A.class, "a");

        assertEquals("select C.a, C.b, C.c from C\nwhere C.a = ?\n", queryBuilder.toString());
        assertEquals(listOf("a"), queryBuilder.getParameters());
    }

    @Test
    public void testSelectAtoD() {
        var queryBuilder = QueryBuilder.select(A.class)
            .joinOnForeignKey(E.class)
            .whereForeignKeyEquals(D.class, "d");

        assertEquals("select A.a, A.b, A.c, A.d as x from A\njoin E on A.a = E.a\nwhere E.d = ?\n", queryBuilder.toString());
        assertEquals(listOf("d"), queryBuilder.getParameters());
    }

    @Test
    public void testSelectDtoA() {
        var queryBuilder = QueryBuilder.select(D.class)
            .joinOnForeignKey(E.class)
            .whereForeignKeyEquals(A.class, "a");

        assertEquals("select D.d, D.e from D\njoin E on D.d = E.d\nwhere E.a = ?\n", queryBuilder.toString());
        assertEquals(listOf("a"), queryBuilder.getParameters());
    }

    @Test
    public void testSelectFGHI() {
        var queryBuilder = QueryBuilder.select(I.class)
            .joinOnPrimaryKey(H.class)
            .joinOnPrimaryKey(G.class)
            .whereForeignKeyEquals(F.class, "f");

        assertEquals("select I.h, I.i, I.t from I\njoin H on H.h = I.h\njoin G on G.g = H.g\nwhere G.f = ?\n", queryBuilder.toString());
        assertEquals(listOf("f"), queryBuilder.getParameters());
    }

    @Test
    public void testSelfJoin() {
        assertThrows(IllegalArgumentException.class, () -> QueryBuilder.select(A.class).joinOnPrimaryKey(A.class));
    }

    @Test
    public void testInsert() {
        var queryBuilder = QueryBuilder.insert(A.class);

        assertEquals("insert into A (b, c, d) values (?, ?, ?)\n", queryBuilder.toString());
        assertEquals(listOf("b", "c", "x"), queryBuilder.getParameters());
    }

    @Test
    public void testUpdate() {
        var queryBuilder = QueryBuilder.update(A.class).wherePrimaryKeyEquals("a");

        assertEquals("update A set b = ?, c = ?, d = coalesce(?, d)\nwhere A.a = ?\n", queryBuilder.toString());
        assertEquals(listOf("b", "c", "x", "a"), queryBuilder.getParameters());
    }

    @Test
    public void testDelete() {
        var queryBuilder = QueryBuilder.delete(A.class).wherePrimaryKeyEquals("a");

        assertEquals("delete from A\nwhere A.a = ?\n", queryBuilder.toString());
        assertEquals(listOf("a"), queryBuilder.getParameters());
    }

    @Test
    public void testAppendLine() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("select a, 'b''c:d' as b from foo");
        queryBuilder.appendLine("where bar = :x");

        assertEquals(listOf("x"), queryBuilder.getParameters());

        assertEquals("select a, 'b''c:d' as b from foo\nwhere bar = ?\n", queryBuilder.toString());
    }

    @Test
    public void testQuotedQuestionMark() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("select '?' as q from xyz");

        assertTrue(queryBuilder.getParameters().isEmpty());

        assertEquals("select '?' as q from xyz\n", queryBuilder.toString());
    }

    @Test
    public void testEscapedQuotes() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''");

        assertTrue(queryBuilder.getParameters().isEmpty());

        assertEquals("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''\n", queryBuilder.toString());
    }

    @Test
    public void testDoubleColon() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.appendLine("select 'ab:c'::varchar(16) as abc"));
    }

    @Test
    public void testMissingParameterName() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.appendLine("select * from xyz where foo = : and bar is null"));
    }
}
