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
import static org.httprpc.kilo.sql.SchemaElementTest.TestSchema1.A;
import static org.httprpc.kilo.sql.SchemaElementTest.TestSchema1.B;
import static org.httprpc.kilo.sql.SchemaElementTest.TestSchema1.C;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaElementTest {
    @Table("test1")
    public enum TestSchema1 implements SchemaElement {
        @Column("a")
        A,
        @Column("b")
        B,
        @Column("c")
        C
    }

    @Table("test2")
    public enum TestSchema2 implements SchemaElement {
        @Column("c")
        C
    }

    @Table("test3")
    public enum TestSchema3 implements SchemaElement {
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
        testSchemaElementColumnName("count(test1.a)", A.count());
    }

    @Test
    public void testAvg() {
        testSchemaElementColumnName("avg(test1.a)", A.avg());
    }

    @Test
    public void testSum() {
        testSchemaElementColumnName("sum(test1.a)", A.sum());
    }

    @Test
    public void testMin() {
        testSchemaElementColumnName("min(test1.a)", A.min());
    }

    @Test
    public void testMax() {
        testSchemaElementColumnName("max(test1.a)", A.max());
    }

    @Test
    public void testEQ() {
        testPredicateComponents("test1.a = :a", A.eq("a"));
        testPredicateComponents("test2.c = test3.c", TestSchema2.C.eq(TestSchema3.C));
        testPredicateComponents("test4.d = 10", TestSchema4.D.eq(10));
        testPredicateComponents("test4.e = true", TestSchema4.E.eq(true));
    }

    @Test
    public void testNE() {
        testPredicateComponents("test1.a != :a", A.ne("a"));
        testPredicateComponents("test2.c != test3.c", TestSchema2.C.ne(TestSchema3.C));
        testPredicateComponents("test4.d != 10", TestSchema4.D.ne(10));
        testPredicateComponents("test4.e != true", TestSchema4.E.ne(true));
    }

    @Test
    public void testGT() {
        testPredicateComponents("test1.a > :a", A.gt("a"));
        testPredicateComponents("test2.c > test3.c", TestSchema2.C.gt(TestSchema3.C));
    }

    @Test
    public void testGE() {
        testPredicateComponents("test1.a >= :a", A.ge("a"));
        testPredicateComponents("test2.c >= test3.c", TestSchema2.C.ge(TestSchema3.C));
    }

    @Test
    public void testLT() {
        testPredicateComponents("test1.a < :a", A.lt("a"));
        testPredicateComponents("test2.c < test3.c", TestSchema2.C.lt(TestSchema3.C));
    }

    @Test
    public void testLE() {
        testPredicateComponents("test1.a <= :a", A.le("a"));
        testPredicateComponents("test2.c <= test3.c", TestSchema2.C.le(TestSchema3.C));
    }

    @Test
    public void testLike() {
        testPredicateComponents("test1.a like :a", A.like("a"));
    }

    @Test
    public void testIn() {
        testPredicateComponents("test1.a in (:a, :b, :c)", A.in("a", "b", "c"));
        testPredicateComponents("test4.d in (10, 11, 12)", TestSchema4.D.in(10, 11, 12));
    }

    @Test
    public void testNotIn() {
        testPredicateComponents("test1.a not in (:a, :b, :c)", A.notIn("a", "b", "c"));
        testPredicateComponents("test4.d not in (10, 11, 12)", TestSchema4.D.notIn(10, 11, 12));
    }

    @Test
    public void testIsNull() {
        testPredicateComponents("test1.a is null", A.isNull());
    }

    @Test
    public void testIsNotNull() {
        testPredicateComponents("test1.a is not null", A.isNotNull());
    }

    @Test
    public void testPlus() {
        testSchemaElementColumnName("(test1.a + test1.b)", A.plus(B));
    }

    @Test
    public void testMinus() {
        testSchemaElementColumnName("(test1.a - test1.b)", A.minus(B));
    }

    @Test
    public void testMultipliedBy() {
        testSchemaElementColumnName("(test1.a * (test1.b + test1.c))", A.multipliedBy(B.plus(C)));
    }

    @Test
    public void testDividedBy() {
        testSchemaElementColumnName("(test1.a / (test1.b - test1.c))", A.dividedBy(B.minus(C)));
    }

    @Test
    public void testAnd() {
        testPredicateComponents("test1.a = :a and test1.b = :b", A.eq("a"), and(B.eq("b")));
    }

    @Test
    public void testOr() {
        testPredicateComponents("test1.a = :a or test1.b = :b", A.eq("a"), or(B.eq("b")));
    }

    @Test
    public void testAllOf() {
        testPredicateComponents("(test1.a = :a and test1.b = :b)", allOf(A.eq("a"), B.eq("b")));
    }

    @Test
    public void testAnyOf() {
        testPredicateComponents("(test1.a = :a or test1.b = :b)", anyOf(A.eq("a"), B.eq("b")));
    }

    @Test
    public void testAlias() {
        testSchemaElementColumnName("count(test1.a)", A.count().as("x"));
        testPredicateComponents("test1.a = :a", A.as("x").eq("a"));
    }

    private static void testSchemaElementColumnName(String expected, SchemaElement schemaElement) {
        assertEquals(expected, schemaElement.getColumnName());
    }

    private static void testPredicateComponents(String expected, PredicateComponent... predicateComponents) {
        var actual = Arrays.stream(predicateComponents).map(Object::toString).collect(Collectors.joining(" "));

        assertEquals(expected, actual);
    }
}
