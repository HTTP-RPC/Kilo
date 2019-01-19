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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.httprpc.AbstractTest;
import org.httprpc.sql.ResultSetAdapter;
import org.junit.Assert;
import org.junit.Test;

public class ResultSetAdapterTest extends AbstractTest {
    public interface TestRow {
        public interface NestedValue {
            public String getLower();
            public String getUpper();
        }

        public long getLong();
        public double getDouble();
        public boolean getBoolean();
        public NestedValue getNestedValue();
        public Date getDate();
    }

    private List<?> expected = listOf(
        mapOf(
            entry("long", 2L),
            entry("double", 3.0),
            entry("boolean", true),
            entry("nestedValue", mapOf(
                entry("lower", "abc"),
                entry("upper", "ABC")
            )),
            entry("date", new Date(0))
        ),
        mapOf(
            entry("long", 4L),
            entry("double", 6.0),
            entry("boolean", false),
            entry("nestedValue", mapOf(
                entry("lower", "def"),
                entry("upper", "DEF")
            )),
            entry("date", new Date(0))
        ),
        mapOf(
            entry("long", 8L),
            entry("double", 9.0),
            entry("boolean", false),
            entry("nestedValue", mapOf(
                entry("lower", null),
                entry("upper", null)
            )),
            entry("date", null)
        )
    );

    @Test
    public void testResultSetAdapter1() throws SQLException {
        List<Map<String, Object>> actual;
        try (TestResultSet resultSet = new TestResultSet()) {
            ResultSetAdapter adapter = new ResultSetAdapter(resultSet);

            actual = StreamSupport.stream(adapter.spliterator(), false).collect(Collectors.toList());
        }

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testResultSetAdapter2() throws SQLException {
        List<Map<String, Object>> actual;
        try (TestResultSet resultSet = new TestResultSet()) {
            ResultSetAdapter adapter = new ResultSetAdapter(resultSet);

            actual = StreamSupport.stream(adapter.spliterator(), false).collect(Collectors.toList());
        }

        Assert.assertEquals(expected, actual);
    }
}
