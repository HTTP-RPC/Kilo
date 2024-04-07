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

import static org.httprpc.kilo.util.Collections.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryBuilderTest {
    @Test
    public void testAppend() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select a, 'b''c:d' as b from foo\n");
        queryBuilder.append("where bar = :x\n");

        assertEquals(listOf("x"), queryBuilder.getParameters());

        assertEquals("select a, 'b''c:d' as b from foo\nwhere bar = ?\n", queryBuilder.toString());
    }

    @Test
    public void testQuotedQuestionMark() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select '?' as q from xyz");

        assertTrue(queryBuilder.getParameters().isEmpty());

        assertEquals("select '?' as q from xyz", queryBuilder.toString());
    }

    @Test
    public void testEscapedQuotes() {
        var queryBuilder = new QueryBuilder();

        queryBuilder.append("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''");

        assertTrue(queryBuilder.getParameters().isEmpty());

        assertEquals("select xyz.*, ''':z' as z from xyz where foo = 'a''b'':c''' and bar = ''''", queryBuilder.toString());
    }

    @Test
    public void testDoubleColon() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.append("select 'ab:c'::varchar(16) as abc"));
    }

    @Test
    public void testMissingParameterName() {
        var queryBuilder = new QueryBuilder();

        assertThrows(IllegalArgumentException.class, () -> queryBuilder.append("select * from xyz where foo = : and bar is null"));
    }
}
