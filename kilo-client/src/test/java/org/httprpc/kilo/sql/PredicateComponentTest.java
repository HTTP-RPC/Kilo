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
import static org.httprpc.kilo.sql.PredicateComponentTest.TestSchema.A;
import static org.httprpc.kilo.sql.PredicateComponentTest.TestSchema.B;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PredicateComponentTest {
    @Table("test")
    public enum TestSchema implements SchemaElement {
        @Column("a")
        A,
        @Column("b")
        B
    }

    @Test
    public void testEQ() {
        testPredicateComponents("a = :a", A.eq("a"));
    }

    @Test
    public void testNE() {
        testPredicateComponents("a != :a", A.ne("a"));
    }

    @Test
    public void testGT() {
        testPredicateComponents("a > :a", A.gt("a"));
    }

    @Test
    public void testGE() {
        testPredicateComponents("a >= :a", A.ge("a"));
    }

    @Test
    public void testLT() {
        testPredicateComponents("a < :a", A.lt("a"));
    }

    @Test
    public void testLE() {
        testPredicateComponents("a <= :a", A.le("a"));
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
    public void testAlias() {
        testPredicateComponents("a = :a", A.as("x").eq("a"));
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

    private static void testPredicateComponents(String expected, PredicateComponent... predicateComponents) {
        var actual = Arrays.stream(predicateComponents).map(Object::toString).collect(Collectors.joining(" "));

        assertEquals(expected, actual);
    }
}
