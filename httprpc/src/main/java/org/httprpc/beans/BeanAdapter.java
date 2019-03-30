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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class that presents the properties of a Java bean object as a map. Property
 * values are adapted as described for {@link #adapt(Object)}.
 */
public class BeanAdapter extends AbstractMap<String, Object> {
    // Iterable adapter
    private static class IterableAdapter extends AbstractList<Object> {
        private Iterable<?> iterable;
        private HashMap<Class<?>, HashMap<String, Method>> accessorCache;

        public IterableAdapter(Iterable<?> iterable, HashMap<Class<?>, HashMap<String, Method>> accessorCache) {
            this.iterable = iterable;
            this.accessorCache = accessorCache;
        }

        @Override
        public Object get(int index) {
            return adapt(getList().get(index), accessorCache);
        }

        @Override
        public int size() {
            return getList().size();
        }

        private List<?> getList() {
            if (!(iterable instanceof List<?>)) {
                throw new UnsupportedOperationException();
            }

            return (List<?>)iterable;
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                private Iterator<?> iterator = iterable.iterator();

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
     * Constructs a new bean adapter.
     *
     * @param bean
     * The source bean.
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

        accessors = accessorCache.get(type);

        if (accessors == null) {
            accessors = new HashMap<>();

            Method[] methods = type.getMethods();

            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }

                String key = getKey(method);

                if (key != null) {
                    accessors.put(key, method);
                }
            }

            accessorCache.put(type, accessors);
        }
    }

    private static String getKey(Method method) {
        if (method.isBridge()) {
            return null;
        }

        String methodName = method.getName();

        String prefix;
        if (methodName.startsWith(GET_PREFIX)) {
            prefix = GET_PREFIX;
        } else if (methodName.startsWith(IS_PREFIX)) {
            prefix = IS_PREFIX;
        } else {
            return null;
        }

        if (method.getParameterCount() > 0) {
            return null;
        }

        Key key = method.getAnnotation(Key.class);

        if (key == null) {
            int j = prefix.length();
            int n = methodName.length();

            if (j == n) {
                return null;
            }

            char c = methodName.charAt(j++);

            if (j == n || Character.isLowerCase(methodName.charAt(j))) {
                c = Character.toLowerCase(c);
            }

            return c + methodName.substring(j);
        } else {
            return key.value();
        }
    }

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
     * Adapts a value. If the value is <tt>null</tt> or an instance of one of
     * the following types, it is returned as is:
     *
     * <ul>
     * <li>{@link CharSequence}</li>
     * <li>{@link Number}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Enum}</li>
     * <li>{@link Date}</li>
     * <li>{@link LocalDate}</li>
     * <li>{@link LocalTime}</li>
     * <li>{@link LocalDateTime}</li>
     * </ul>
     *
     * If the value is an instance of {@link Iterable}, it is wrapped in an
     * adapter that will adapt the sequences's elements. If the value is a
     * {@link Map}, it is wrapped in an adapter that will adapt the map's
     * values. Otherwise, the value is assumed to be a bean and is wrapped in a
     * {@link BeanAdapter}.
     *
     * @param <T>
     * The target type.
     *
     * @param value
     * The value to adapt.
     *
     * @return
     * The adapted value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T adapt(Object value) {
        return (T)adapt(value, new HashMap<>());
    }

    private static Object adapt(Object value, HashMap<Class<?>, HashMap<String, Method>> accessorCache) {
        if (value == null
            || value instanceof CharSequence
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Enum<?>
            || value instanceof Date
            || value instanceof LocalDate
            || value instanceof LocalTime
            || value instanceof LocalDateTime) {
            return value;
        } else if (value instanceof Iterable<?>) {
            return new IterableAdapter((Iterable<?>)value, accessorCache);
        } else if (value instanceof Map<?, ?>) {
            return new MapAdapter((Map<?, ?>)value, accessorCache);
        } else {
            return new BeanAdapter(value, accessorCache);
        }
    }

    /**
     * Returns the value at a given key path.
     *
     * @param <V>
     * The type of the value to return.
     *
     * @param root
     * The root object.
     *
     * @param path
     * The path to the value.
     *
     * @return
     * The value at the given path, or <tt>null</tt> if the value does not exist.
     */
    @SuppressWarnings("unchecked")
    public static <V> V valueAt(Map<String, ?> root, String path) {
        Object value = root;

        String[] components = path.split("\\.");

        for (int i = 0; i < components.length; i++) {
            String component = components[i];

            if (value instanceof Map<?, ?>) {
                value = ((Map<?, ?>)value).get(component);
            } else {
                value = null;

                break;
            }
        }

        return (V)value;
    }

    /**
     * Describes a type. Types are encoded as follows:
     *
     * <ul>
     * <li>{@link Object}: "any"</li>
     * <li>{@link Void} or <tt>void</tt>: "void"</li>
     * <li>{@link Byte} or <tt>byte</tt>: "byte"</li>
     * <li>{@link Short} or <tt>short</tt>: "short"</li>
     * <li>{@link Integer} or <tt>int</tt>: "integer"</li>
     * <li>{@link Long} or <tt>long</tt>: "long"</li>
     * <li>{@link Float} or <tt>float</tt>: "float"</li>
     * <li>{@link Double} or <tt>double</tt>: "double"</li>
     * <li>Any other type that extends {@link Number}: "number"</li>
     * <li>Any type that implements {@link CharSequence}: "string"</li>
     * <li>Any {@link Enum} type: "enum"</li>
     * <li>Any type that extends {@link Date}: "date"</li>
     * <li>{@link LocalDate}: "date-local"</li>
     * <li>{@link LocalTime}: "time-local"</li>
     * <li>{@link LocalDateTime}: "datetime-local"</li>
     * <li>{@link Iterable}, {@link Collection}, or {@link List}: "[<i>element type</i>]"</li>
     * <li>{@link Map}: "[<i>key type</i>: <i>value type</i>]"</li>
     * <li>Any other type: "{property1: <i>property 1 type</i>, property2: <i>property 2 type</i>, ...}"</li>
     * </ul>
     *
     * @param type
     * The type to describe.
     *
     * @param structures
     * A map that will be populated with descriptions of all bean types
     * referenced by this type.
     *
     * @return
     * The type's description.
     */
    public static String describe(Type type, Map<Class<?>, String> structures) {
        if (type instanceof Class<?>) {
            return describe((Class<?>)type, structures);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType)type;

            return describe(wildcardType.getUpperBounds()[0], structures);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;

            Type rawType = parameterizedType.getRawType();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (rawType == Iterable.class || rawType == Collection.class || rawType == List.class) {
                return "[" + describe(actualTypeArguments[0], structures) + "]";
            } else if (rawType == Map.class) {
                return "[" + describe(actualTypeArguments[0], structures) + ": " + describe(actualTypeArguments[1], structures) + "]";
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static String describe(Class<?> type, Map<Class<?>, String> structures) {
        if (type == Object.class) {
            return "any";
        } else if (type == Void.TYPE || type == Void.class) {
            return "void";
        } else if (type == Byte.TYPE || type == Byte.class) {
            return "byte";
        } else if (type == Short.TYPE || type == Short.class) {
            return "short";
        } else if (type == Integer.TYPE || type == Integer.class) {
            return "integer";
        } else if (type == Long.TYPE || type == Long.class) {
            return "long";
        } else if (type == Float.TYPE || type == Float.class) {
            return "float";
        } else if (type == Double.TYPE || type == Double.class) {
            return "double";
        } else if (Number.class.isAssignableFrom(type)) {
            return "number";
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return "boolean";
        } else if (CharSequence.class.isAssignableFrom(type)) {
            return "string";
        } else if (Enum.class.isAssignableFrom(type)) {
            return "enum";
        } else if (Date.class.isAssignableFrom(type)) {
            return "date";
        } else if (type == LocalDate.class) {
            return "date-local";
        } else if (type == LocalTime.class) {
            return "time-local";
        } else if (type == LocalDateTime.class) {
            return "datetime-local";
        } else {
            if (!structures.containsKey(type)) {
                structures.put(type, null);

                Method[] methods = type.getMethods();

                TreeMap<String, String> properties = new TreeMap<>();

                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];

                    if (method.getDeclaringClass() == Object.class) {
                        continue;
                    }

                    String key = getKey(method);

                    if (key != null) {
                        properties.put(key, describe(method.getGenericReturnType(), structures));
                    }
                }

                int j = 0;

                StringBuilder descriptionBuilder = new StringBuilder();

                descriptionBuilder.append("{\n");

                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    if (j > 0) {
                        descriptionBuilder.append(",\n");
                    }

                    descriptionBuilder.append("  " + entry.getKey() + ": " + entry.getValue());

                    j++;
                }

                descriptionBuilder.append("\n}");

                structures.put(type,  descriptionBuilder.toString());
            }

            return type.getSimpleName();
        }
    }
}
