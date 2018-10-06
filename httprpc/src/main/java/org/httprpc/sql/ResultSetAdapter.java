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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

import org.httprpc.beans.BeanAdapter;

/**
 * Class that presents the contents of a result set as an iterable sequence
 * of maps.
 */
public class ResultSetAdapter implements Iterable<Map<String, Object>> {
    private ResultSet resultSet;
    private ResultSetMetaData resultSetMetaData;

    private LinkedHashMap<String, String> subqueries = new LinkedHashMap<>();

    private LinkedHashMap<String, Object> row = new LinkedHashMap<>();

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

            return hasNext.booleanValue();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            row.clear();

            try {
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

            for (Map.Entry<String, String> entry : subqueries.entrySet()) {
                Parameters parameters = Parameters.parse(entry.getValue());

                parameters.putAll(row);

                LinkedList<Map<String, Object>> results = new LinkedList<>();

                try {
                    Connection connection = resultSet.getStatement().getConnection();

                    try (PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
                        parameters.apply(statement);

                        try (ResultSet resultSet = statement.executeQuery()) {
                            for (Map<String, Object> result : new ResultSetAdapter(resultSet)) {
                                results.add(result);
                            }
                        }
                    }
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }

                row.put(entry.getKey(), results);
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
     * Attaches a subquery to the result set.
     *
     * @param key
     * The key to associate with the subquery results.
     *
     * @param subquery
     * The subquery to attach.
     */
    public void attach(String key, String subquery) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        if (subquery == null) {
            throw new IllegalArgumentException();
        }

        subqueries.put(key, subquery);
    }

    /**
     * Returns the next result.
     *
     * @return
     * The next result, or <tt>null</tt> if there are no more results.
     */
    public Map<String, Object> next() {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return iterator;
    }

    /**
     * Adapts the result set adapter for typed access.
     *
     * @param <T>
     * The element type.
     *
     * @param elementType
     * The element type.
     *
     * @return
     * An iterable sequence of the given type.
     */
    public <T> Iterable<T> adapt(Class<T> elementType) {
        if (!elementType.isInterface()) {
            throw new IllegalArgumentException();
        }

        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private Iterator<Map<String, Object>> iterator = ResultSetAdapter.this.iterator();

                    private T proxy = BeanAdapter.adapt(row, elementType);

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        iterator.next();

                        return proxy;
                    }
                };
            }
        };
    }
}
