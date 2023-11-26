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
import java.util.stream.Collectors;

import static org.httprpc.kilo.sql.PredicateComponent.allOf;
import static org.httprpc.kilo.sql.PredicateComponent.and;
import static org.httprpc.kilo.sql.PredicateComponent.anyOf;
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
        testSchemaElementColumnName("count(test3.a)", TestSchema3.A.count());
    }

    @Test
    public void testAvg() {
        testSchemaElementColumnName("avg(test3.a)", TestSchema3.A.avg());
    }

    @Test
    public void testSum() {
        testSchemaElementColumnName("sum(test3.a)", TestSchema3.A.sum());
    }

    @Test
    public void testMin() {
        testSchemaElementColumnName("min(test3.a)", TestSchema3.A.min());
    }

    @Test
    public void testMax() {
        testSchemaElementColumnName("max(test3.a)", TestSchema3.A.max());
    }

    @Test
    public void testEQ() {
        testPredicateComponents("test1.a = test2.a", TestSchema1.A.eq(TestSchema2.A));
        testPredicateComponents("test3.a = :a", TestSchema3.A.eq("a"));
        testPredicateComponents("test4.d = 10", TestSchema4.D.eq(10));
        testPredicateComponents("test4.e = true", TestSchema4.E.eq(true));
    }

    @Test
    public void testNE() {
        testPredicateComponents("test1.a != test2.a", TestSchema1.A.ne(TestSchema2.A));
        testPredicateComponents("test3.a != :a", TestSchema3.A.ne("a"));
        testPredicateComponents("test4.d != 10", TestSchema4.D.ne(10));
        testPredicateComponents("test4.e != true", TestSchema4.E.ne(true));
    }

    @Test
    public void testGT() {
        testPredicateComponents("test1.a > test2.a", TestSchema1.A.gt(TestSchema2.A));
        testPredicateComponents("test3.a > :a", TestSchema3.A.gt("a"));
    }

    @Test
    public void testGE() {
        testPredicateComponents("test1.a >= test2.a", TestSchema1.A.ge(TestSchema2.A));
        testPredicateComponents("test3.a >= :a", TestSchema3.A.ge("a"));
    }

    @Test
    public void testLT() {
        testPredicateComponents("test1.a < test2.a", TestSchema1.A.lt(TestSchema2.A));
        testPredicateComponents("test3.a < :a", TestSchema3.A.lt("a"));
    }

    @Test
    public void testLE() {
        testPredicateComponents("test1.a <= test2.a", TestSchema1.A.le(TestSchema2.A));
        testPredicateComponents("test3.a <= :a", TestSchema3.A.le("a"));
    }

    @Test
    public void testLike() {
        testPredicateComponents("test3.a like :a", TestSchema3.A.like("a"));
    }

    @Test
    public void testIn() {
        testPredicateComponents("test3.a in (:a, :b, :c)", TestSchema3.A.in("a", "b", "c"));
        testPredicateComponents("test4.d in (10, 11, 12)", TestSchema4.D.in(10, 11, 12));
    }

    @Test
    public void testNotIn() {
        testPredicateComponents("test3.a not in (:a, :b, :c)", TestSchema3.A.notIn("a", "b", "c"));
        testPredicateComponents("test4.d not in (10, 11, 12)", TestSchema4.D.notIn(10, 11, 12));
    }

    @Test
    public void testIsNull() {
        testPredicateComponents("test3.a is null", TestSchema3.A.isNull());
    }

    @Test
    public void testIsNotNull() {
        testPredicateComponents("test3.a is not null", TestSchema3.A.isNotNull());
    }

    @Test
    public void testPlus() {
        testSchemaElementColumnName("(test3.a + test3.b)", TestSchema3.A.plus(B));
    }

    @Test
    public void testMinus() {
        testSchemaElementColumnName("(test3.a - test3.b)", TestSchema3.A.minus(B));
    }

    @Test
    public void testMultipliedBy() {
        testSchemaElementColumnName("(test3.a * (test3.b + test3.c))", TestSchema3.A.multipliedBy(B.plus(C)));
    }

    @Test
    public void testDividedBy() {
        testSchemaElementColumnName("(test3.a / (test3.b - test3.c))", TestSchema3.A.dividedBy(B.minus(C)));
    }

    @Test
    public void testAnd() {
        testPredicateComponents("test3.a = :a and test3.b = :b", TestSchema3.A.eq("a"), and(B.eq("b")));
    }

    @Test
    public void testOr() {
        testPredicateComponents("test3.a = :a or test3.b = :b", TestSchema3.A.eq("a"), or(B.eq("b")));
    }

    @Test
    public void testAllOf() {
        testPredicateComponents("(test3.a = :a and test3.b = :b)", allOf(TestSchema3.A.eq("a"), B.eq("b")));
    }

    @Test
    public void testAnyOf() {
        testPredicateComponents("(test3.a = :a or test3.b = :b)", anyOf(TestSchema3.A.eq("a"), B.eq("b")));
    }

    @Test
    public void testAlias() {
        testSchemaElementColumnName("count(test3.a)", TestSchema3.A.count().as("x"));
        testPredicateComponents("test3.a = :a", TestSchema3.A.as("x").eq("a"));
    }

    private static void testSchemaElementColumnName(String expected, SchemaElement schemaElement) {
        assertEquals(expected, schemaElement.getColumnName());
    }

    private static void testPredicateComponents(String expected, PredicateComponent... predicateComponents) {
        var actual = Arrays.stream(predicateComponents).map(Object::toString).collect(Collectors.joining(" "));

        assertEquals(expected, actual);
    }
}
