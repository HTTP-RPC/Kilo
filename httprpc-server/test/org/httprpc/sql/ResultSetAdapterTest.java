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

import java.sql.Date;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;

import org.httprpc.AbstractTest;
import org.httprpc.sql.ResultSetAdapter;
import org.junit.Assert;
import org.junit.Test;

public class ResultSetAdapterTest extends AbstractTest {
    @Test
    public void testResultSetAdapter() throws SQLException {
        LinkedList<Map<String, Object>> list = new LinkedList<>();

        try (TestResultSet resultSet = new TestResultSet()) {
            ResultSetAdapter adapter = new ResultSetAdapter(resultSet);

            for (Map<String, Object> row : adapter) {
                list.add(row);
            }
        }

        Assert.assertEquals(listOf(mapOf(
            entry("a", 2L),
            entry("b", 4.0),
            entry("c", "abc"),
            entry("d", true),
            entry("e", new Date(0))
        )), list);
    }
}
