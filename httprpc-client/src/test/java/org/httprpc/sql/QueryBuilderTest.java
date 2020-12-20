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

package org.httprpc.sql;

import org.junit.jupiter.api.Test;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryBuilderTest {
    @Test
    public void testSelect() {
        String sql = QueryBuilder.select("a", "b", "c")
            .from("A")
            .join("B").on("A.id = B.id and x = 50")
            .leftJoin("C").on("B.id = C.id")
            .rightJoin("D").on("C.id = D.id")
            .where("(a > 10 or b < 200) and c = :c")
            .orderBy("a", "b")
            .limit(10)
            .forUpdate().toString();

        assertEquals("select a, b, c from A "
            + "join B on A.id = B.id and x = 50 "
            + "left join C on B.id = C.id "
            + "right join D on C.id = D.id "
            + "where (a > 10 or b < 200) and c = :c "
            + "order by a, b "
            + "limit 10 "
            + "for update", sql);
    }

    @Test
    public void testInsertInto() {
        String sql = QueryBuilder.insertInto("A", mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", "hello"),
            entry("d", ":d"),
            entry("e", "?"),
            entry("f", QueryBuilder.select("f").from("F"))
        )).toString();

        assertEquals("insert into A (a, b, c, d, e, f) values (1, true, 'hello', :d, ?, (select f from F))", sql);
    }

    @Test
    public void testUpdate() {
        String sql = QueryBuilder.update("A").set(mapOf(
            entry("a", 1),
            entry("b", true),
            entry("c", "hello"),
            entry("d", ":d"),
            entry("e", "?"),
            entry("f", QueryBuilder.select("f").from("F"))
        )).where("a is not null").toString();

        assertEquals("update A set a = 1, b = true, c = 'hello', d = :d, e = ?, f = (select f from F) where a is not null", sql);
    }

    @Test
    public void testDelete() {
        String sql = QueryBuilder.deleteFrom("A")
            .where("a < 150")
            .toString();

        assertEquals("delete from A where a < 150", sql);
    }
}
