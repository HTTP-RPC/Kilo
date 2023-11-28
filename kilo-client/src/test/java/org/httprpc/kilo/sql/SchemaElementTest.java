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

import java.util.Arrays;

import static org.httprpc.kilo.sql.PredicateComponent.allOf;
import static org.httprpc.kilo.sql.PredicateComponent.and;
import static org.httprpc.kilo.sql.PredicateComponent.anyOf;
import static org.httprpc.kilo.sql.PredicateComponent.exists;
import static org.httprpc.kilo.sql.PredicateComponent.notExists;
import static org.httprpc.kilo.sql.PredicateComponent.or;
import static org.httprpc.kilo.sql.SchemaElementTest.TestSchema3.B;
import static org.httprpc.kilo.sql.SchemaElementTest.TestSchema3.C;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaElementTest {
    @Table("test1")
    public enum TestSchema1 implements SchemaElement {
        @Column("a")
        A
    }

    @Table("test2")
    public enum TestSchema2 implements SchemaElement {
        @Column("a")
        A
    }

    @Table("test3")
    public enum TestSchema3 implements SchemaElement {
        @Column("a")
        A,
        @Column("b")
        B,
        @Column("c")
        C
    }

    @Table("test4")
    public enum TestSchema4 implements SchemaElement {
        @Column("d")
        D,
        @Column("e")
        E
    }

    @Test
    public void testCount() {
        testColumnName("count(test3.a)", TestSchema3.A.count());
    }

    @Test
    public void testAvg() {
        testColumnName("avg(test3.a)", TestSchema3.A.avg());
    }

    @Test
    public void testSum() {
        testColumnName("sum(test3.a)", TestSchema3.A.sum());
    }

    @Test
    public void testMin() {
        testColumnName("min(test3.a)", TestSchema3.A.min());
    }

    @Test
    public void testMax() {
        testColumnName("max(test3.a)", TestSchema3.A.max());
    }

    @Test
    public void testEQ() {
        testPredicateComponent("test1.a = test2.a", TestSchema1.A.eq(TestSchema2.A));
        testPredicateComponent("test3.a = ?", TestSchema3.A.eq("a"), "a");
        testPredicateComponent("test4.d = 10", TestSchema4.D.eq(10));
        testPredicateComponent("test4.e = true", TestSchema4.E.eq(true));
    }

    @Test
    public void testNE() {
        testPredicateComponent("test1.a != test2.a", TestSchema1.A.ne(TestSchema2.A));
        testPredicateComponent("test3.a != ?", TestSchema3.A.ne("a"), "a");
        testPredicateComponent("test4.d != 10", TestSchema4.D.ne(10));
        testPredicateComponent("test4.e != true", TestSchema4.E.ne(true));
    }

    @Test
    public void testGT() {
        testPredicateComponent("test1.a > test2.a", TestSchema1.A.gt(TestSchema2.A));
        testPredicateComponent("test3.a > ?", TestSchema3.A.gt("a"), "a");
        testPredicateComponent("test4.d > 10", TestSchema4.D.gt(10));
    }

    @Test
    public void testGE() {
        testPredicateComponent("test1.a >= test2.a", TestSchema1.A.ge(TestSchema2.A));
        testPredicateComponent("test3.a >= ?", TestSchema3.A.ge("a"), "a");
        testPredicateComponent("test4.d >= 10", TestSchema4.D.ge(10));
    }

    @Test
    public void testLT() {
        testPredicateComponent("test1.a < test2.a", TestSchema1.A.lt(TestSchema2.A));
        testPredicateComponent("test3.a < ?", TestSchema3.A.lt("a"), "a");
        testPredicateComponent("test4.d < 10", TestSchema4.D.lt(10));
    }

    @Test
    public void testLE() {
        testPredicateComponent("test1.a <= test2.a", TestSchema1.A.le(TestSchema2.A));
        testPredicateComponent("test3.a <= ?", TestSchema3.A.le("a"), "a");
        testPredicateComponent("test4.d <= 10", TestSchema4.D.le(10));
    }

    @Test
    public void testLike() {
        testPredicateComponent("test3.a like ?", TestSchema3.A.like("a"), "a");
    }

    @Test
    public void testIn() {
        testPredicateComponent("test3.a in (?, ?, ?)", TestSchema3.A.in("a", "b", "c"), "a", "b", "c");
        testPredicateComponent("test4.d in (10, 11, 12)", TestSchema4.D.in(10, 11, 12));
    }

    @Test
    public void testNotIn() {
        testPredicateComponent("test3.a not in (?, ?, ?)", TestSchema3.A.notIn("a", "b", "c"), "a", "b", "c");
        testPredicateComponent("test4.d not in (10, 11, 12)", TestSchema4.D.notIn(10, 11, 12));
    }

    @Test
    public void testIsNull() {
        testPredicateComponent("test3.a is null", TestSchema3.A.isNull());
    }

    @Test
    public void testIsNotNull() {
        testPredicateComponent("test3.a is not null", TestSchema3.A.isNotNull());
    }

    @Test
    public void testPlus() {
        testColumnName("(test3.a + test3.b)", TestSchema3.A.plus(B));
    }

    @Test
    public void testMinus() {
        testColumnName("(test3.a - test3.b)", TestSchema3.A.minus(B));
    }

    @Test
    public void testMultipliedBy() {
        testColumnName("(test3.a * (test3.b + test3.c))", TestSchema3.A.multipliedBy(B.plus(C)));
    }

    @Test
    public void testDividedBy() {
        testColumnName("(test3.a / (test3.b - test3.c))", TestSchema3.A.dividedBy(B.minus(C)));
    }

    @Test
    public void testAnd() {
        testPredicateComponent("and test3.b = ?", and(B.eq("b")), "b");
    }

    @Test
    public void testOr() {
        testPredicateComponent("or test3.b = ?", or(B.eq("b")), "b");
    }

    @Test
    public void testAllOf() {
        testPredicateComponent("(test3.a = ? and test3.b = ?)", allOf(TestSchema3.A.eq("a"), B.eq("b")), "a", "b");
    }

    @Test
    public void testAnyOf() {
        testPredicateComponent("(test3.a = ? or test3.b = ?)", anyOf(TestSchema3.A.eq("a"), B.eq("b")), "a", "b");
    }

    @Test
    public void testExists() {
        testPredicateComponent("exists (select * from test1 where test1.a = ?)", exists(
            QueryBuilder.selectAll().from(TestSchema1.class).where(TestSchema1.A.eq("a"))
        ), "a");
    }

    @Test
    public void testNotExists() {
        testPredicateComponent("not exists (select * from test1 where test1.a = ?)", notExists(
            QueryBuilder.selectAll().from(TestSchema1.class).where(TestSchema1.A.eq("a"))
        ), "a");
    }

    @Test
    public void testAlias() {
        testColumnName("count(test3.a)", TestSchema3.A.count().as("x"));
        testPredicateComponent("test3.a = ?", TestSchema3.A.as("x").eq("a"), "a");
    }

    private static void testColumnName(String expected, SchemaElement schemaElement) {
        assertEquals(expected, schemaElement.getColumnName());
    }

    private static void testPredicateComponent(String expected, PredicateComponent predicateComponent, String... parameters) {
        assertEquals(expected, predicateComponent.toString());
        assertEquals(Arrays.asList(parameters), predicateComponent.getParameters());
    }
}
