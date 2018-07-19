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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        private List<?> list;
        private HashMap<Class<?>, HashMap<String, Method>> accessorCache;

        public ListAdapter(List<?> list, HashMap<Class<?>, HashMap<String, Method>> accessorCache) {
            this.list = list;
            this.accessorCache = accessorCache;
        }

        @Override
        public Object get(int index) {
            return adapt(list.get(index), accessorCache);
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                private Iterator<?> iterator = list.iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Object next() {
                    return adapt(iterator.next(), accessorCache);
                }
            };
        }
    }

    // Map adapter
    private static class MapAdapter extends AbstractMap<Object, Object> {
        private Map<?, ?> map;
        private HashMap<Class<?>, HashMap<String, Method>> accessorCache;

        public MapAdapter(Map<?, ?> map, HashMap<Class<?>, HashMap<String, Method>> accessorCache) {
            this.map = map;
            this.accessorCache = accessorCache;
        }

        @Override
        public Object get(Object key) {
            return adapt(map.get(key), accessorCache);
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return new AbstractSet<Entry<Object, Object>>() {
                @Override
                public int size() {
                    return map.size();
                }

                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    return new Iterator<Entry<Object, Object>>() {
                        private Iterator<? extends Entry<?, ?>> iterator = map.entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Entry<Object, Object> next() {
                            return new Entry<Object, Object>() {
                                private Entry<?, ?> entry = iterator.next();

                                @Override
                                public Object getKey() {
                                    return entry.getKey();
                                }

                                @Override
                                public Object getValue() {
                                    return adapt(entry.getValue(), accessorCache);
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
        }
    }

    private Object bean;
    private HashMap<Class<?>, HashMap<String, Method>> accessorCache;

    private HashMap<String, Method> accessors;

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    /**
     * Constructs a new Bean adapter.
     *
     * @param bean
     * The source Bean.
     */
    public BeanAdapter(Object bean) {
        this(bean, new HashMap<>());
    }

    private BeanAdapter(Object bean, HashMap<Class<?>, HashMap<String, Method>> accessorCache) {
        if (bean == null) {
            throw new IllegalArgumentException();
        }

        this.bean = bean;
        this.accessorCache = accessorCache;

        Class<?> type = bean.getClass();

        if (accessors == null) {
            accessors = new HashMap<>();

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

            accessorCache.put(type, accessors);
        }
    }

    /**
     * Retrieves a Bean property value. If the value is <tt>null</tt> or an
     * instance of one of the following types, it is returned as-is:
     * <ul>
     * <li>{@link String}</li>
     * <li>{@link Number}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Date}</li>
     * <li>{@link LocalDate}</li>
     * <li>{@link LocalTime}</li>
     * <li>{@link LocalDateTime}</li>
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
                value = adapt(method.invoke(bean), accessorCache);
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
        return new AbstractSet<Entry<String, Object>>() {
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
    }

    /**
     * Adapts a list instance.
     *
     * @param list
     * The list to adapt.
     *
     * @return
     * An adapter that will adapt the list's elements.
     */
    public static List<?> adapt(List<?> list) {
        return new ListAdapter(list, new HashMap<>());
    }

    /**
     * Adapts a map instance.
     *
     * @param map
     * The map to adapt.
     *
     * @return
     * An adapter that will adapt the map's values.
     */
    public static Map<?, ?> adapt(Map<?, ?> map) {
        return new MapAdapter(map, new HashMap<>());
    }

    private static Object adapt(Object value, HashMap<Class<?>, HashMap<String, Method>> accessorCache) {
        if (value == null
            || value instanceof String
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Date
            || value instanceof LocalDate
            || value instanceof LocalTime
            || value instanceof LocalDateTime) {
            return value;
        } else if (value instanceof List<?>) {
            return new ListAdapter((List<?>)value, accessorCache);
        } else if (value instanceof Map<?, ?>) {
            return new MapAdapter((Map<?, ?>)value, accessorCache);
        } else {
            return new BeanAdapter(value, accessorCache);
        }
    }

    /**
     * Adapts a map for typed access.
     *
     * @param map
     * The map to adapt.
     *
     * @param type
     * The result type.
     *
     * @return
     * An instance of the given type that provides typed access to the entries
     * in the map.
     */
    public static <T> T adapt(Map<String, ?> map, Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, new InvocationHandler() {
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

                return map.get(c + methodName.substring(j));
            }
        }));
    }
}
