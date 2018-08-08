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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.httprpc.AbstractTest;
import org.httprpc.beans.BeanAdapter;
import org.httprpc.sql.ResultSetAdapter;
import org.junit.Assert;
import org.junit.Test;

public class ResultSetAdapterTest extends AbstractTest {
    public interface TestRow {
        public long getA();
        public double getB();
        public boolean getC();
        public String getD();
        public Date getE();
    }

    private List<?> expected = listOf(
        mapOf(
            entry("a", 2L),
            entry("b", 3.0),
            entry("c", true),
            entry("d", "abc"),
            entry("e", new Date(0))
        ),
        mapOf(
            entry("a", 4L),
            entry("b", 6.0),
            entry("c", false),
            entry("d", "def"),
            entry("e", new Date(0))
        ),
        mapOf(
            entry("a", 8L),
            entry("b", 9.0),
            entry("c", false),
            entry("d", "ghi"),
            entry("e", null)
        )
    );

    @Test
    public void testResultSetAdapter1() throws SQLException {
        LinkedList<Map<String, Object>> actual = new LinkedList<>();

        try (TestResultSet resultSet = new TestResultSet()) {
            ResultSetAdapter adapter = new ResultSetAdapter(resultSet);

            for (Map<String, Object> row : adapter) {
                HashMap<String, Object> map = new HashMap<>();

                map.putAll(row);

                actual.add(map);
            }
        }

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testResultSetAdapter2() throws SQLException {
        LinkedList<Map<String, Object>> actual = new LinkedList<>();

        try (TestResultSet resultSet = new TestResultSet()) {
            ResultSetAdapter resultSetAdapter = new ResultSetAdapter(resultSet);

            for (TestRow row : resultSetAdapter.adapt(TestRow.class)) {
                HashMap<String, Object> map = new HashMap<>();

                map.putAll(new BeanAdapter(row));

                actual.add(map);
            }
        }

        Assert.assertEquals(expected, actual);
    }
}
