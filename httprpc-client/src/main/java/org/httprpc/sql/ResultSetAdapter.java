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
 * Class that presents the contents of a result set as an iterable sequence
 * of maps. Map instances are mutable, and column order is preserved.
 */
public class ResultSetAdapter implements Iterable<Map<String, Object>>, AutoCloseable {
    private ResultSet resultSet;

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
        @SuppressWarnings("unchecked")
        public Map<String, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            LinkedHashMap<String, Object> row = new LinkedHashMap<>();

            try {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

                for (int i = 0, n = resultSetMetaData.getColumnCount(); i < n; i++) {
                    String path = resultSetMetaData.getColumnLabel(i + 1);

                    String[] components = path.split("\\.");

                    Map<String, Object> map = row;

                    for (int j = 0; j < components.length - 1; j++) {
                        Object value = map.get(components[j]);

                        LinkedHashMap<String, Object> child;
                        if (value instanceof Map<?, ?>) {
                            child = (LinkedHashMap<String, Object>)value;
                        } else {
                            child = new LinkedHashMap<>();

                            map.put(components[j], child);
                        }

                        map = child;
                    }

                    map.put(components[components.length - 1], resultSet.getObject(i + 1));
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
    }

    /**
     * Returns the result set's fetch size.
     *
     * @return
     * The result set's fetch size.
     *
     * @throws SQLException
     * If an error occurs while retrieving the fetch size.
     */
    public int getFetchSize() throws SQLException {
        return resultSet.getFetchSize();
    }

    /**
     * Sets the result set's fetch size.
     *
     * @param fetchSize
     * The result set's fetch size.
     *
     * @throws SQLException
     * If an error occurs while setting the fetch size.
     */
    public void setFetchSize(int fetchSize) throws SQLException {
        resultSet.setFetchSize(fetchSize);
    }

    /**
     * Returns the next result.
     *
     * @return
     * The next result, or <code>null</code> if there are no more results.
     */
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
