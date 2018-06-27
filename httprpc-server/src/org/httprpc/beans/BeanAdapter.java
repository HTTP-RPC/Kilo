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

package org.httprpc.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that presents the properties of a Java Bean object as a map.
 */
public class BeanAdapter extends AbstractMap<String, Object> {
    // List adapter
    private static class ListAdapter extends AbstractList<Object> {
        private List<Object> list;

        public ListAdapter(List<Object> list) {
            this.list = list;
        }

        @Override
        public Object get(int index) {
            return adapt(list.get(index));
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                private Iterator<Object> iterator = list.iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Object next() {
                    return adapt(iterator.next());
                }
            };
        }
    }

    // Map adapter
    private static class MapAdapter extends AbstractMap<Object, Object> {
        private Map<Object, Object> map;

        private Set<Entry<Object, Object>> entrySet = new AbstractSet<Entry<Object, Object>>() {
            @Override
            public int size() {
                return map.size();
            }

            @Override
            public Iterator<Entry<Object, Object>> iterator() {
                return new Iterator<Entry<Object, Object>>() {
                    private Iterator<Entry<Object, Object>> iterator = map.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<Object, Object> next() {
                        return new Entry<Object, Object>() {
                            private Entry<Object, Object> entry = iterator.next();

                            @Override
                            public Object getKey() {
                                return entry.getKey();
                            }

                            @Override
                            public Object getValue() {
                                return adapt(entry.getValue());
                            }

                            @Override
                            public Object setValue(Object value) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        };

        public MapAdapter(Map<Object, Object> map) {
            this.map = map;
        }

        @Override
        public Object get(Object key) {
            return adapt(map.get(key));
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return entrySet;
        }
    }

    private Object bean;

    private HashMap<String, Method> accessors = new HashMap<>();

    private Set<Entry<String, Object>> entrySet = new AbstractSet<Entry<String, Object>>() {
        @Override
        public int size() {
            return accessors.size();
        }

        @Override
        public Iterator<Entry<String, Object>> iterator() {
            return new Iterator<Entry<String, Object>>() {
                private Iterator<String> keys = accessors.keySet().iterator();

                @Override
                public boolean hasNext() {
                    return keys.hasNext();
                }

                @Override
                public Entry<String, Object> next() {
                    String key = keys.next();

                    return new SimpleImmutableEntry<>(key, get(key));
                }
            };
        }
    };

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    /**
     * Constructs a new Bean adapter.
     *
     * @param bean
     * The source Bean.
     */
    public BeanAdapter(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException();
        }

        this.bean = bean;

        Class<?> type = bean.getClass();

        Method[] methods = type.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            if (method.getDeclaringClass() != Object.class) {
                String methodName = method.getName();

                String prefix;
                if (methodName.startsWith(GET_PREFIX)) {
                    prefix = GET_PREFIX;
                } else if (methodName.startsWith(IS_PREFIX)) {
                    prefix = IS_PREFIX;
                } else {
                    prefix = null;
                }

                if (prefix != null)  {
                    int j = prefix.length();
                    int n = methodName.length();

                    if (j < n && method.getParameterCount() == 0) {
                        char c = methodName.charAt(j++);

                        if (j == n || Character.isLowerCase(methodName.charAt(j))) {
                            c = Character.toLowerCase(c);
                        }

                        String key = c + methodName.substring(j);

                        accessors.put(key, method);
                    }
                }
            }
        }
    }

    /**
     * Retrieves a Bean property value. If the value is <tt>null</tt> or an
     * instance of one of the following types, it is returned as-is:
     * <ul>
     * <li>{@link String}</li>
     * <li>{@link Number}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Enum}</li>
     * <li>{@link Date}</li>
     * <li>{@link TemporalAccessor}</li>
     * </ul>
     * If the value is a {@link List}, it is wrapped in an adapter that will
     * adapt the list's elements. If the value is a {@link Map}, it is wrapped
     * in an adapter that will adapt the map's values. Otherwise, the value is
     * considered a nested Bean and is wrapped in a {@link BeanAdapter}.
     *
     * @param key
     * The property name.
     *
     * @return
     * The property value.
     */
    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        Method method = accessors.get(key);

        Object value;
        if (method != null) {
            try {
                value = adapt(method.invoke(bean));
            } catch (InvocationTargetException | IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            value = null;
        }

        return value;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return entrySet;
    }

    @SuppressWarnings("unchecked")
    private static Object adapt(Object value) {
        if (!(value == null
            || value instanceof String
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Enum<?>
            || value instanceof Date
            || value instanceof TemporalAccessor)) {
            if (value instanceof List<?>) {
                value = new ListAdapter((List<Object>)value);
            } else if (value instanceof Map<?, ?>) {
                value = new MapAdapter((Map<Object, Object>)value);
            } else {
                value = new BeanAdapter(value);
            }
        }

        return value;
    }
}
