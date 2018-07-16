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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Class that presents the contents of a result set as an iterable sequence
 * of maps.
 */
public class ResultSetAdapter implements Iterable<Map<String, Object>> {
    private ResultSet resultSet;
    private ResultSetMetaData resultSetMetaData;

    private LinkedHashMap<String, Object> row = new LinkedHashMap<>();

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

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

        try {
            resultSetMetaData = resultSet.getMetaData();
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return new Iterator<Map<String, Object>>() {
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
            public Map<String, Object> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                row.clear();

                try {
                    for (int i = 0, n = resultSetMetaData.getColumnCount(); i < n; i++) {
                        row.put(resultSetMetaData.getColumnLabel(i + 1), resultSet.getObject(i + 1));
                    }
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }

                hasNext = null;

                return row;
            }
        };
    }

    /**
     * Adapts a result set for typed iteration.
     *
     * @param resultSet
     * The result set to adapt.
     *
     * @param rowType
     * The row type.
     *
     * @return
     * A typed iterable over the result set.
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterable<T> adapt(ResultSet resultSet, Class<T> rowType) {
        ResultSetAdapter resultSetAdapter = new ResultSetAdapter(resultSet);

        return new Iterable<T>() {
            private Iterator<Map<String, Object>> iterator = resultSetAdapter.iterator();

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private Map<String, Object> row = null;

                    private T proxy = (T)Proxy.newProxyInstance(rowType.getClassLoader(), new Class[] {rowType}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            String methodName = method.getName();

                            String prefix;
                            if (methodName.startsWith(GET_PREFIX)) {
                                prefix = GET_PREFIX;
                            } else if (methodName.startsWith(IS_PREFIX)) {
                                prefix = IS_PREFIX;
                            } else {
                                throw new UnsupportedOperationException();
                            }

                            int j = prefix.length();
                            int n = methodName.length();

                            if (j == n || method.getParameterCount() > 0) {
                                throw new UnsupportedOperationException();
                            }

                            char c = methodName.charAt(j++);

                            if (j == n || Character.isLowerCase(methodName.charAt(j))) {
                                c = Character.toLowerCase(c);
                            }

                            return row.get(c + methodName.substring(j));
                        }
                    });

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        row = iterator.next();

                        return proxy;
                    }
                };
            }
        };
    }
}
