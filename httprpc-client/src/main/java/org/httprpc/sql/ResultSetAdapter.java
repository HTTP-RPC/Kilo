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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link Iterable} adapter for JDBC result sets.
 */
public class ResultSetAdapter implements Iterable<Map<String, Object>>, AutoCloseable {
    private ResultSet resultSet;
    private ResultSetMetaData resultSetMetaData;

    private Iterator<Map<String, Object>> iterator = new Iterator<Map<String, Object>>() {
        private Boolean hasNext = null;

        @Override
        public boolean hasNext() {
            if (hasNext == null) {
                try {
                    hasNext = resultSet.next() ? Boolean.TRUE : Boolean.FALSE;
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }
            }

            return hasNext;
        }

        @Override
        public Map<String, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Map<String, Object> row = new LinkedHashMap<>();

            try {
                for (int i = 1, n = resultSetMetaData.getColumnCount(); i <= n; i++) {
                    row.put(resultSetMetaData.getColumnLabel(i), resultSet.getObject(i));
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            hasNext = null;

            return row;
        }
    };

    /**
     * Constructs a new result set adapter.
     *
     * @param resultSet
     * The source result set.
     */
    public ResultSetAdapter(ResultSet resultSet) {
        if (resultSet == null) {
            throw new IllegalArgumentException();
        }

        this.resultSet = resultSet;

        try {
            resultSetMetaData = resultSet.getMetaData();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Returns the next result.
     *
     * @return
     * The next result, or <code>null</code> if there are no more results.
     *
     * @deprecated
     * Use {@link QueryBuilder#getResult()} instead.
     */
    @Deprecated
    public Map<String, Object> next() {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return iterator;
    }

    @Override
    public void close() throws SQLException {
        resultSet.close();
    }

    /**
     * Returns a stream over the results. Closing the returned stream does not
     * close the underlying result set.
     *
     * @return
     * A stream over the results.
     */
    public Stream<Map<String, Object>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
