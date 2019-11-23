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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParametersTest {
    @Test
    public void testParameters() {
        Parameters parameters = Parameters.parse("insert into xyz (foo, bar) values :foo, :bar");

        assertEquals("insert into xyz (foo, bar) values ?, ?", parameters.getSQL());
    }

    @Test
    public void testColon() {
        Parameters parameters = Parameters.parse("select * from xyz where foo = 'a:b:c'");

        assertEquals("select * from xyz where foo = 'a:b:c'", parameters.getSQL());
    }

    @Test
    public void testDoubleColon() {
        Parameters parameters = Parameters.parse("select 'ab:c'::varchar(16) as abc");

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
