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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Provides access to the contents of a JDBC result set via the
 * {@link Iterable} interface. Individual rows are represented by mutable map
 * instances produced by the adapter's iterator.
 */
public class ResultSetAdapter implements Iterable<Map<String, Object>>, AutoCloseable {
    private ResultSet resultSet;
    private Map<String, Function<Object, Object>> transforms;

    private ResultSetMetaData resultSetMetaData;

    private Iterator<Map<String, Object>> iterator = new Iterator<>() {
        Boolean hasNext = null;

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
                    var key = resultSetMetaData.getColumnLabel(i);

                    var value = resultSet.getObject(i);

                    if (value != null) {
                        switch (value) {
                            case java.sql.Date date -> value = date.toLocalDate();
                            case java.sql.Time time -> value = time.toLocalTime();
                            case java.sql.Timestamp timestamp -> value = timestamp.toInstant();
                            default -> {
                                var transform = transforms.get(key);

                                if (transform != null) {
                                    value = transform.apply(value);
                                }
                            }
                        }
                    }

                    row.put(key, value);
                }
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            hasNext = null;

            return row;
        }
    };

    ResultSetAdapter(ResultSet resultSet, Map<String, Function<Object, Object>> transforms) {
        this.resultSet = resultSet;
        this.transforms = transforms;

        try {
            resultSetMetaData = resultSet.getMetaData();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Returns the result set.
     *
     * @return
     * The result set.
     */
    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * <p>Returns an iterator over the results.</p>
     *
     * <p>Temporal values are converted as follows:</p>
     *
     * <ul>
     * <li>{@link java.sql.Date} - {@link java.time.LocalDate}</li>
     * <li>{@link java.sql.Time} - {@link java.time.LocalTime}</li>
     * <li>{@link java.sql.Timestamp} - {@link java.time.Instant}</li>
     * </ul>
     *
     * <p>All other values are returned as is, or transformed as specified by
     * {@link QueryBuilder#select(Class[])}.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public Iterator<Map<String, Object>> iterator() {
        return iterator;
    }

    /**
     * Closes the underlying result set.
     * {@inheritDoc}
     */
    @Override
    public void close() throws SQLException {
        resultSet.close();
    }

    /**
     * Returns a stream over the results. Closing the returned stream closes
     * the adapter along with the underlying result set.
     *
     * @return
     * A stream over the results.
     */
    public Stream<Map<String, Object>> stream() {
        return StreamSupport.stream(spliterator(), false).onClose(() -> {
            try {
                close();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }
}
