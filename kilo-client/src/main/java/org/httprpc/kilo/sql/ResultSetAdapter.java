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

import org.httprpc.kilo.io.JSONDecoder;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.httprpc.kilo.util.Collections.setOf;

/**
 * Provides access to the contents of a JDBC result set via the
 * {@link Iterable} interface. Individual rows are represented by mutable map
 * instances produced by the adapter's iterator.
 */
public class ResultSetAdapter implements Iterable<Map<String, Object>>, AutoCloseable {
    private ResultSet resultSet;
    private Set<String> jsonKeys;

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
                    var value = resultSet.getObject(i);

                    var columnLabel = resultSetMetaData.getColumnLabel(i);

                    if (jsonKeys.contains(columnLabel)) {
                        if (!(value instanceof String string)) {
                            throw new UnsupportedOperationException("Value is not a string.");

                        }

                        var jsonDecoder = new JSONDecoder();

                        var valueReader = new StringReader(string);

                        try {
                            value = jsonDecoder.read(valueReader);
                        } catch (IOException exception) {
                            throw new UnsupportedOperationException(exception);
                        }
                    }

                    row.put(columnLabel, value);
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
        this(resultSet, setOf());
    }

    /**
     * Constructs a new result set adapter.
     *
     * @param resultSet
     * The source result set.
     *
     * @param jsonKeys
     * A set of keys whose associated values are encoded as JSON.
     */
    ResultSetAdapter(ResultSet resultSet, Set<String> jsonKeys) {
        if (resultSet == null || jsonKeys == null) {
            throw new IllegalArgumentException();
        }

        this.resultSet = resultSet;
        this.jsonKeys = jsonKeys;

        try {
            resultSetMetaData = resultSet.getMetaData();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Returns an iterator over the results.
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
