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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Exposes the contents of a JDBC result set as an iterable list of maps.
 */
public class ResultSetAdapter extends AbstractList<Map<String, Object>> implements AutoCloseable {
    private ResultSet resultSet;

    private Map<String, Object> row = new AbstractMap<String, Object>() {
        private Set<Entry<String, Object>> entrySet = new AbstractSet<Entry<String, Object>>() {
            @Override
            public int size() {
                int size;
                try {
                    size = resultSet.getMetaData().getColumnCount();
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }

                return size;
            }

            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<Entry<String, Object>>() {
                    private int index = -1;

                    private Entry<String, Object> entry = new Entry<String, Object>() {
                        @Override
                        public String getKey() {
                            String key;
                            try {
                                key = resultSet.getMetaData().getColumnLabel(index + 1);
                            } catch (SQLException exception) {
                                throw new RuntimeException();
                            }

                            return key;
                        }

                        @Override
                        public Object getValue() {
                            Object value;
                            try {
                                value = resultSet.getObject(index + 1);
                            } catch (SQLException exception) {
                                throw new RuntimeException();
                            }

                            return value;
                        }

                        @Override
                        public Object setValue(Object value) {
                            throw new UnsupportedOperationException();
                        }
                    };

                    @Override
                    public boolean hasNext() {
                        return (index < size() - 1);
                    }

                    @Override
                    public Entry<String, Object> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }

                        index++;

                        return entry;
                    }
                };
            }
        };

        @Override
        public Object get(Object key) {
            if (key == null) {
                throw new IllegalArgumentException();
            }

            Object value;
            try {
                value = resultSet.getObject(key.toString());
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }

            return value;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return entrySet;
        }
    };

    private static final int UNKNOWN = -1;
    private static final int FALSE = 0;
    private static final int TRUE = 1;

    /**
     * Creates a new result set adapter.
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

    @Override
    public void close() throws Exception {
        Statement statement = resultSet.getStatement();

        try {
            resultSet.close();
        } finally {
            if (statement != null) {
                Connection connection = statement.getConnection();

                try {
                    statement.close();
                } finally {
                    connection.close();
                }

            }
        }
    }

    @Override
    public Map<String, Object> get(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return new Iterator<Map<String, Object>>() {
            private int next = UNKNOWN;

            @Override
            public boolean hasNext() {
                if (next == UNKNOWN) {
                    try {
                        next = resultSet.next() ? TRUE : FALSE;
                    } catch (SQLException exception) {
                        throw new RuntimeException(exception);
                    }
                }

                return (next == TRUE);
            }

            @Override
            public Map<String, Object> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                next = UNKNOWN;

                return row;
            }
        };
    }
}
