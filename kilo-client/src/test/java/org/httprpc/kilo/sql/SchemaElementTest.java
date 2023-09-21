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
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaElementTest {
    @Table("test1")
    public enum TestSchema1 implements SchemaElement {
        @Column("a")
        A,
        @Column("b")
        B
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

    @Test
    public void testCount() {
        testSchemaElementLabel("count(a)", A.count());
    }

    @Test
    public void testAvg() {
        testSchemaElementLabel("avg(a)", A.avg());
    }

    @Test
    public void testSum() {
        testSchemaElementLabel("sum(a)", A.sum());
    }

    @Test
    public void testMin() {
        testSchemaElementLabel("min(a)", A.min());
    }

    @Test
    public void testMax() {
        testSchemaElementLabel("max(a)", A.max());
    }

    @Test
    public void testEQ() {
        testPredicateComponents("a = :a", A.eq("a"));
        testPredicateComponents("test2.c = test3.c", TestSchema2.C.eq(TestSchema3.C));
    }

    @Test
    public void testNE() {
        testPredicateComponents("a != :a", A.ne("a"));
        testPredicateComponents("test2.c != test3.c", TestSchema2.C.ne(TestSchema3.C));
    }

    @Test
    public void testGT() {
        testPredicateComponents("a > :a", A.gt("a"));
        testPredicateComponents("test2.c > test3.c", TestSchema2.C.gt(TestSchema3.C));
    }

    @Test
    public void testGE() {
        testPredicateComponents("a >= :a", A.ge("a"));
        testPredicateComponents("test2.c >= test3.c", TestSchema2.C.ge(TestSchema3.C));
    }

    @Test
    public void testLT() {
        testPredicateComponents("a < :a", A.lt("a"));
        testPredicateComponents("test2.c < test3.c", TestSchema2.C.lt(TestSchema3.C));
    }

    @Test
    public void testLE() {
        testPredicateComponents("a <= :a", A.le("a"));
        testPredicateComponents("test2.c <= test3.c", TestSchema2.C.le(TestSchema3.C));
    }

    @Test
    public void testLike() {
        testPredicateComponents("a like :a", A.like("a"));
    }

    @Test
    public void testIn() {
        testPredicateComponents("a in (:a, :b, :c)", A.in("a", "b", "c"));
    }

    @Test
    public void testNotIn() {
        testPredicateComponents("a not in (:a, :b, :c)", A.notIn("a", "b", "c"));
    }

    @Test
    public void testIsNull() {
        testPredicateComponents("a is null", A.isNull());
    }

    @Test
    public void testIsNotNull() {
        testPredicateComponents("a is not null", A.isNotNull());
    }

    @Test
    public void testAnd() {
        testPredicateComponents("a = :a and b = :b", A.eq("a"), and(B.eq("b")));
    }

    @Test
    public void testOr() {
        testPredicateComponents("a = :a or b = :b", A.eq("a"), or(B.eq("b")));
    }

    @Test
    public void testAllOf() {
        testPredicateComponents("(a = :a and b = :b)", allOf(A.eq("a"), B.eq("b")));
    }

    @Test
    public void testAnyOf() {
        testPredicateComponents("(a = :a or b = :b)", anyOf(A.eq("a"), B.eq("b")));
    }

    @Test
    public void testAlias() {
        testSchemaElementLabel("count(a)", A.count().as("x"));
        testPredicateComponents("a = :a", A.as("x").eq("a"));
    }

    private static void testSchemaElementLabel(String expected, SchemaElement schemaElement) {
        assertEquals(expected, schemaElement.getColumnName());
    }

    private static void testPredicateComponents(String expected, PredicateComponent... predicateComponents) {
        var actual = Arrays.stream(predicateComponents).map(Object::toString).collect(Collectors.joining(" "));

        assertEquals(expected, actual);
    }
}
