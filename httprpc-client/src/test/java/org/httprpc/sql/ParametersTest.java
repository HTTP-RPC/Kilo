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

import javax.management.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.mapOf;

public class ParametersTest {
    @Test
    public void testParameters() {
        String sql = QueryBuilder.insertInto("xyz").values(mapOf(
            entry("foo", ":foo"),
            entry("bar", ":bar"),
            entry("baz", QueryBuilder.select("x").from("y").where("z = :z")),
            entry("quux", "?")
        )).toString();

        Parameters parameters = Parameters.parse(sql);

        assertEquals("insert into xyz (foo, bar, baz, quux) values (?, ?, (select x from y where z = ?), ?)", parameters.getSQL());
    }

    @Test
    public void testColon() {
        String sql = QueryBuilder.select("*").from("xyz").where("foo = 'a:b:c'").toString();

        Parameters parameters = Parameters.parse(sql);

        assertEquals("select * from xyz where foo = 'a:b:c'", parameters.getSQL());
    }

    @Test
    public void testDoubleColon() {
        String sql = QueryBuilder.select("'ab:c'::varchar(16) as abc").toString();

        Parameters parameters = Parameters.parse(sql);

        assertEquals("select 'ab:c'::varchar(16) as abc", parameters.getSQL());
    }

    @Test
    public void testSingleLineComment() {
        Parameters parameters = Parameters.parse("-- this is a comment: hello\r\nselect * from xyz where foo = :foo");

        assertEquals("-- this is a comment: hello\r\nselect * from xyz where foo = ?",
            parameters.getSQL());
    }

    @Test
    public void testMultiLineComment() {
        Parameters parameters = Parameters.parse("/* this is a comment: hello\r\nand so is this: goodbye */ select * from xyz where foo = :foo");

        assertEquals("/* this is a comment: hello\r\nand so is this: goodbye */ select * from xyz where foo = ?",
            parameters.getSQL());
    }

    @Test
    public void testSingleAndMultiLineComment() {
        Parameters parameters = Parameters.parse("/* this is a comment: hello -- and so is this: goodbye */ select * from xyz where foo = :foo");

        assertEquals("/* this is a comment: hello -- and so is this: goodbye */ select * from xyz where foo = ?",
            parameters.getSQL());
    }
}
