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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class QueryBuilderTest {
    @Table("A")
    public interface A {
        @Column("a")
        @PrimaryKey
        String getA();

        @Column("b")
        @Identifier(1)
        @Required
        Double getB();

        @Column("c")
        @Identifier(2)
        @Required
        Boolean getC();

        @Name("x")
        @Column("d")
        @Final
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
        @Index
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

        @Column("g")
        Integer getG();
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
        @Index(1)
        String getT();

        @Column("u")
        @Index(2)
        String getU();
    }

    @Table("J")
    public interface J {
        @Column("i")
        @PrimaryKey
        @ForeignKey(I.class)
        Integer getI();

        @Column("v")
        String getV();
    }

    @Table("K")
    public interface K {
        @Column("i")
        @PrimaryKey
        @ForeignKey(I.class)
        Integer getI();

        @Column("w")
        String getW();
    }

    public interface L extends K {
        @Column("z")
        String getZ();
    }

    @Table("M")
    public static class M {
        private Integer m;
        private Integer n;
        private String x;
        private String y;

        @Column("m")
        @PrimaryKey
        public Integer getM() {
            return m;
        }

        public void setM(Integer m) {
            this.m = m;
        }

        @Column("n")
        @ForeignKey(M.class)
        public Integer getN() {
            return n;
        }

        public void setN(Integer n) {
            this.n = n;
        }

        @Column("x")
        public String getX() {
            return x;
        }

        public void setX(String x) {
            this.x = x;
        }

        @Column("y")
        public String getY() {
            return y;
        }

        public void setY(String y) {
            this.y = y;
        }
    }

    public interface P {
        String getR();
    }

    @Table("Q")
    public interface Q extends P {
        @Column("a")
        @PrimaryKey
        @ForeignKey(A.class)
        String getA();

        @Column("r")
        @Override
        String getR();
    }

    @Table("whitespace_test")
    public interface WhitespaceTest {
        @Column("value")
        String getValue();
    }

    @Test
    public void testSelectA() {
        var queryBuilder = QueryBuilder.select(A.class).filterByPrimaryKey("a");

        assertEquals("select A.a, A.b, A.c, A.d as x from A where A.a = ?", queryBuilder.toString());
        assertEquals(listOf("a"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectB() {
        var queryBuilder = QueryBuilder.select(B.class).filterByPrimaryKey("a").limit(10);

        assertEquals("select B.a, B.b, B.c, B.e, B.d as x, B.f as y from B where B.a = ? limit 10", queryBuilder.toString());
        assertEquals(listOf("a"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectC() {
        var queryBuilder = QueryBuilder.select(C.class).filterByForeignKey(A.class, "a").forUpdate();

        assertEquals("select C.a, C.b, C.c from C where C.a = ? for update", queryBuilder.toString());
        assertEquals(listOf("a"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectAtoD() {
        var queryBuilder = QueryBuilder.select(A.class, E.class)
            .join(E.class, A.class)
            .filterByPrimaryKey("a")
            .filterByForeignKey(E.class, D.class, "d");

        assertEquals("select A.a, A.b, A.c, A.d as x, E.z from A "
            + "join E on A.a = E.a "
            + "where A.a = ? "
            + "and E.d = ?", queryBuilder.toString());

        assertEquals(listOf("a", "d"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectBtoD() {
        var queryBuilder = QueryBuilder.select(B.class, E.class)
            .join(E.class, B.class)
            .filterByPrimaryKey("a")
            .filterByForeignKey(E.class, D.class, "d");

        assertEquals("select B.a, B.b, B.c, B.e, B.d as x, B.f as y, E.z from B "
            + "join E on B.a = E.a "
            + "where B.a = ? "
            + "and E.d = ?", queryBuilder.toString());

        assertEquals(listOf("a", "d"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectCtoD() {
        var queryBuilder = QueryBuilder.select(C.class)
            .join(E.class, A.class)
            .filterByForeignKey(A.class, "a")
            .filterByForeignKey(E.class, D.class, "d");

        assertEquals("select C.a, C.b, C.c from C "
            + "join E on C.a = E.a "
            + "where C.a = ? "
            + "and E.d = ?", queryBuilder.toString());

        assertEquals(listOf("a", "d"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectDtoA() {
        var queryBuilder = QueryBuilder.select(D.class)
            .join(E.class, D.class)
            .filterByForeignKey(E.class, A.class, "a")
            .filterByPrimaryKey("d");

        assertEquals("select D.d, D.g from D "
            + "join E on D.d = E.d "
            + "where E.a = ? "
            + "and D.d = ?", queryBuilder.toString());

        assertEquals(listOf("a", "d"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectIHGF() {
        var queryBuilder = QueryBuilder.select(I.class, H.class, G.class)
            .join(H.class)
            .join(G.class)
            .filterByForeignKey(G.class, F.class, "f")
            .ordered(true);

        assertEquals("select I.h, I.i, I.t, I.u, H.h, H.s, G.g, G.r from I "
            + "join H on I.h = H.h "
            + "join G on H.g = G.g "
            + "where G.f = ? "
            + "order by t asc, u asc", queryBuilder.toString());

        assertEquals(listOf("f"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectIJK() {
        var queryBuilder = QueryBuilder.select(I.class, J.class, K.class)
            .join(J.class, I.class)
            .join(K.class, I.class)
            .filterByPrimaryKey("i")
            .ordered(false);

        assertEquals("select I.h, I.i, I.t, I.u, J.v, K.w from I "
            + "join J on I.i = J.i "
            + "join K on J.i = K.i "
            + "where I.i = ? "
            + "order by t desc, u desc", queryBuilder.toString());

        assertEquals(listOf("i"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectL() {
        var queryBuilder = QueryBuilder.select(L.class).filterByPrimaryKey("i");

        assertEquals("select K.i, K.w, K.z from K where K.i = ?", queryBuilder.toString());

        assertEquals(listOf("i"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectM() {
        var queryBuilder = QueryBuilder.select(M.class).filterByForeignKey(M.class, "m");

        assertEquals("select M.m, M.n, M.x, M.y from M where M.n = ?", queryBuilder.toString());
        assertEquals(listOf("m"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectAQ() {
        var queryBuilder = QueryBuilder.select(A.class, Q.class)
            .join(Q.class, A.class)
            .filterByPrimaryKey("a");

        assertEquals("select A.a, A.b, A.c, A.d as x, Q.r from A join Q on A.a = Q.a where A.a = ?", queryBuilder.toString());
        assertEquals(listOf("a"), getParameters(queryBuilder));
    }

    @Test
    public void testSelectDistinctIndex() {
        var queryBuilder = QueryBuilder.selectDistinctIndex(I.class);

        assertEquals("select distinct I.t, I.u from I", queryBuilder.toString());
    }

    @Test
    public void testInvalidJoin() {
        assertThrows(UnsupportedOperationException.class, () -> QueryBuilder.select(A.class).join(String.class));
        assertThrows(UnsupportedOperationException.class, () -> QueryBuilder.select(A.class).join(String.class));

        assertThrows(UnsupportedOperationException.class, () -> QueryBuilder.select(A.class).join(Runnable.class));
        assertThrows(UnsupportedOperationException.class, () -> QueryBuilder.select(A.class).join(Runnable.class));
    }

    @Test
    public void testIdentifier() {
        var queryBuilder = QueryBuilder.select(A.class).filterByIdentifier("b", "c");

        assertEquals("select A.a, A.b, A.c, A.d as x from A where A.b = ? and A.c = ?", queryBuilder.toString());
        assertEquals(listOf("b", "c"), getParameters(queryBuilder));
    }

    @Test
    public void testGreaterThan() {
        var queryBuilder = QueryBuilder.select(I.class).filterByIndexGreaterThan("t");

        assertEquals("select I.h, I.i, I.t, I.u from I where I.t > ?", queryBuilder.toString());
        assertEquals(listOf("t"), getParameters(queryBuilder));
    }

    @Test
    public void testGreaterThanOrEqualTo() {
        var queryBuilder = QueryBuilder.select(I.class).filterByIndexGreaterThanOrEqualTo("t");

        assertEquals("select I.h, I.i, I.t, I.u from I where I.t >= ?", queryBuilder.toString());
        assertEquals(listOf("t"), getParameters(queryBuilder));
    }

    @Test
    public void testLessThan() {
        var queryBuilder = QueryBuilder.select(I.class).filterByIndexLessThan("t");

        assertEquals("select I.h, I.i, I.t, I.u from I where I.t < ?", queryBuilder.toString());
        assertEquals(listOf("t"), getParameters(queryBuilder));
    }

    @Test
    public void testLessThanOrEqualTo() {
        var queryBuilder = QueryBuilder.select(I.class).filterByIndexLessThanOrEqualTo("t");

        assertEquals("select I.h, I.i, I.t, I.u from I where I.t <= ?", queryBuilder.toString());
        assertEquals(listOf("t"), getParameters(queryBuilder));
    }

    @Test
    public void testEqualTo() {
        var queryBuilder = QueryBuilder.select(I.class).filterByIndexEqualTo("t", "u");

        assertEquals("select I.h, I.i, I.t, I.u from I where I.t = ? and I.u = ?", queryBuilder.toString());
        assertEquals(listOf("t", "u"), getParameters(queryBuilder));
    }

    @Test
    public void testLike() {
        var queryBuilder = QueryBuilder.select(I.class).filterByIndexLike("t", "u");

        assertEquals("select I.h, I.i, I.t, I.u from I where I.t like ? and I.u like ?", queryBuilder.toString());
        assertEquals(listOf("t", "u"), getParameters(queryBuilder));
    }

    @Test
    public void testExists() {
        var queryBuilder = QueryBuilder.select(A.class)
            .filterByExists(QueryBuilder.selectAll(C.class)
                .filterByForeignKey(A.class)
                .filterByIndexLessThan("b"));

        assertEquals("select A.a, A.b, A.c, A.d as x from A where exists (select C.* from C where C.a = A.a and C.b < ?)", queryBuilder.toString());
        assertEquals(listOf("b"), getParameters(queryBuilder));
    }

    @Test
    public void testNotExists() {
        var queryBuilder = QueryBuilder.select(A.class)
            .filterByNotExists(QueryBuilder.selectAll(C.class)
                .filterByForeignKey(A.class)
                .filterByIndexGreaterThan("b"));

        assertEquals("select A.a, A.b, A.c, A.d as x from A where not exists (select C.* from C where C.a = A.a and C.b > ?)", queryBuilder.toString());
        assertEquals(listOf("b"), getParameters(queryBuilder));
    }

    @Test
    public void testUnion() {
        var queryBuilder = QueryBuilder.select(A.class).filterByPrimaryKey("a1").union(QueryBuilder.select(A.class).filterByPrimaryKey("a2"));

        assertEquals("select A.a, A.b, A.c, A.d as x from A where A.a = ? union select A.a, A.b, A.c, A.d as x from A where A.a = ?", queryBuilder.toString());
        assertEquals(listOf("a1", "a2"), getParameters(queryBuilder));
    }

    @Test
    public void testInsert() {
        var queryBuilder = QueryBuilder.insert(A.class);

        assertEquals("insert into A (b, c, d) values (?, ?, ?)", queryBuilder.toString());
        assertEquals(listOf("b", "c", "x"), getParameters(queryBuilder));
    }

    @Test
    public void testOnDuplicateKeyUpdate() {
        var queryBuilder = QueryBuilder.insert(A.class).onDuplicateKeyUpdate();

        assertEquals("insert into A (b, c, d) values (?, ?, ?) on duplicate key update b = values(b), c = values(c)", queryBuilder.toString());
        assertEquals(listOf("b", "c", "x"), getParameters(queryBuilder));
    }

    @Test
    public void testUpdate() {
        var queryBuilder = QueryBuilder.update(A.class).filterByPrimaryKey("a");

        assertEquals("update A set b = ?, c = ? where A.a = ?", queryBuilder.toString());
        assertEquals(listOf("b", "c", "a"), getParameters(queryBuilder));
    }

    @Test
    public void testUpdateParent() {
        var queryBuilder = QueryBuilder.updateParent(I.class, H.class, "h").filterByPrimaryKey("i");

        assertEquals("update I set h = ? where I.i = ?", queryBuilder.toString());
        assertEquals(listOf("h", "i"), getParameters(queryBuilder));
    }

    @Test
    public void testDelete() {
        var queryBuilder = QueryBuilder.delete(A.class).filterByPrimaryKey("a");

        assertEquals("delete from A where A.a = ?", queryBuilder.toString());
        assertEquals(listOf("a"), getParameters(queryBuilder));
    }

    @Test
    public void testAppendLine() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("select a, 'b''c:d' as b from foo");
        queryBuilder.appendLine("where foo = :x and bar = :y");

        assertEquals("select a, 'b''c:d' as b from foo\nwhere foo = ? and bar = ?\n", queryBuilder.toString());

        assertEquals(listOf("x", "y"), getParameters(queryBuilder));

        assertThrows(NoSuchElementException.class, () -> queryBuilder.join(A.class));
    }

    @Test
    public void testMissingParameterName() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.appendLine("select * from xyz where foo = : and bar is null"));
    }

    @Test
    public void testInvalidParameterName() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.appendLine("select * from xyz where foo = :@ and bar is null"));
    }

    @Test
    public void testWhitespaceNotAllowed() throws SQLException {
        var queryBuilder = QueryBuilder.insert(WhitespaceTest.class);

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertThrows(IllegalArgumentException.class, () -> queryBuilder.executeUpdate(statement, mapOf(
                entry("value", "abc ")
            )));
        }

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertThrows(IllegalArgumentException.class, () -> queryBuilder.executeUpdate(statement, mapOf(
                entry("value", " abc")
            )));
        }

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertThrows(IllegalArgumentException.class, () -> queryBuilder.executeUpdate(statement, mapOf(
                entry("value", " abc ")
            )));
        }

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertThrows(IllegalArgumentException.class, () -> queryBuilder.executeUpdate(statement, mapOf(
                entry("value", "  ")
            )));
        }

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertThrows(IllegalArgumentException.class, () -> queryBuilder.executeUpdate(statement, mapOf(
                entry("value", " ")
            )));
        }

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertEquals(1, queryBuilder.executeUpdate(statement, mapOf(
                entry("value", "")
            )));
        }

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertEquals(1, queryBuilder.executeUpdate(statement, mapOf(
                entry("value", "a b c")
            )));
        }
    }

    @Test
    public void testWhitespaceAllowed() throws SQLException {
        var queryBuilder = QueryBuilder.insert(WhitespaceTest.class);

        queryBuilder.setWhitespaceAllowed(true);

        try (var statement = queryBuilder.prepare(getConnection())) {
            assertEquals(1, queryBuilder.executeUpdate(statement, mapOf(
                entry("value", " abc ")
            )));
        }
    }

    private static List<String> getParameters(QueryBuilder queryBuilder) {
        var n = queryBuilder.getParameterCount();

        var parameters = new ArrayList<String>(n);

        for (var i = 0; i < n; i++) {
            parameters.add(queryBuilder.getParameter(i));
        }

        return parameters;
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mariadb://db.local:3306/demo", "demo", "demo123!");
    }
}
