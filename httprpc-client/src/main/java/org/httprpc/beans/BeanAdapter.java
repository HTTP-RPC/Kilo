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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
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
                    method.setAccessible(true);

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

        if (method.getAnnotation(Ignore.class) != null) {
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
     * <li>{@link URL}</li>
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
            || value instanceof LocalDateTime
            || value instanceof URL) {
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
     * Adapts a value for typed access. If the value is already an instance of
     * the given type, it is returned as is. Otherwise:
     *
     * <ul>
     * <li>If the target type is a number or boolean, the value is parsed or
     * coerced using the appropriate conversion method. Missing or <tt>null</tt>
     * values are automatically converted to <tt>0</tt> or <tt>false</tt> for
     * primitive argument types.</li>
     * <li>If the target type is {@link String}, the value is adapted via
     * {@link Object#toString()}.</li>
     * <li>If the target type is {@link Date}, the value is coerced to a long
     * value and passed to {@link Date#Date(long)}.</li>
     * <li>If the target type is {@link LocalDate}, the value's string
     * representation is parsed using {@link LocalDate#parse(CharSequence)}.</li>
     * <li>If the target type is {@link LocalTime}, the value's string
     * representation is parsed using {@link LocalTime#parse(CharSequence)}.</li>
     * <li>If the target type is {@link LocalDateTime}, the value's string
     * representation is parsed using {@link LocalDateTime#parse(CharSequence)}.</li>
     * </ul>
     *
     * If the target type is a {@link List}, the value is wrapped in an adapter
     * that will adapt the list's elements. If the target type is a {@link Map},
     * the value is wrapped in an adapter that will adapt the map's values.
     *
     * Otherwise, the target is assumed to be a bean interface, and the value is
     * assumed to be a map. The return value is an implementation of the given
     * interface that maps accessor methods to entries in the map. Property values
     * are adapted as described above.
     *
     * @param <T>
     * The target type.
     *
     * @param value
     * The value to adapt.
     *
     * @param type
     * The target type.
     *
     * @return
     * An instance of the given type that adapts the given value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T adapt(Object value, Type type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        if (type instanceof Class<?>) {
            return (T)adapt(value, (Class<?>)type);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType)type;

            return (T)adapt(value, wildcardType.getUpperBounds()[0]);
        } else if (type instanceof ParameterizedType) {
            if (value != null) {
                ParameterizedType parameterizedType = (ParameterizedType)type;

                Type rawType = parameterizedType.getRawType();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

                if (rawType == List.class) {
                    return (T)adaptList((List<?>)value, actualTypeArguments[0]);
                } else if (rawType == Map.class) {
                    return (T)adaptMap((Map<?, ?>)value, actualTypeArguments[1]);
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static Object adapt(Object value, Class<?> type) {
        if (type.isInstance(value)) {
            return value;
        } else if (type == Byte.TYPE || type == Byte.class) {
            if (value == null) {
                return (type == Byte.TYPE) ? Byte.valueOf((byte)0) : null;
            } else if (value instanceof Number) {
                return ((Number)value).byteValue();
            } else {
                return Byte.parseByte(value.toString());
            }
        } else if (type == Short.TYPE || type == Short.class) {
            if (value == null) {
                return (type == Short.TYPE) ? Short.valueOf((short)0) : null;
            } else if (value instanceof Number) {
                return ((Number)value).shortValue();
            } else {
                return Short.parseShort(value.toString());
            }
        } else if (type == Integer.TYPE || type == Integer.class) {
            if (value == null) {
                return (type == Integer.TYPE) ? Integer.valueOf(0) : null;
            } else if (value instanceof Number) {
                return ((Number)value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } else if (type == Long.TYPE || type == Long.class) {
            if (value == null) {
                return (type == Long.TYPE) ? Long.valueOf(0) : null;
            } else if (value instanceof Number) {
                return ((Number)value).longValue();
            } else {
                return Long.parseLong(value.toString());
            }
        } else if (type == Float.TYPE || type == Float.class) {
            if (value == null) {
                return (type == Float.TYPE) ? Float.valueOf(0) : null;
            } else if (value instanceof Number) {
                return ((Number)value).floatValue();
            } else {
                return Float.parseFloat(value.toString());
            }
        } else if (type == Double.TYPE || type == Double.class) {
            if (value == null) {
                return (type == Double.TYPE) ? Double.valueOf(0) : null;
            } else if (value instanceof Number) {
                return ((Number)value).doubleValue();
            } else {
                return Double.parseDouble(value.toString());
            }
        } else if (type == Boolean.TYPE) {
            if (value == null) {
                return false;
            } else if (value instanceof Boolean) {
                return value;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else if (value != null) {
            if (type == String.class) {
                return value.toString();
            } else if (type == Date.class) {
                if (value instanceof Number) {
                    return new Date(((Number)value).longValue());
                } else {
                    return new Date(Long.parseLong(value.toString()));
                }
            } else if (type == LocalDate.class) {
                return LocalDate.parse(value.toString());
            } else if (type == LocalTime.class) {
                return LocalTime.parse(value.toString());
            } else if (type == LocalDateTime.class) {
                return LocalDateTime.parse(value.toString());
            } else if (value instanceof Map<?, ?>) {
                return adaptBean((Map<?, ?>)value, type);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            return null;
        }
    }

    private static Object adaptBean(Map<?, ?> map, Class<?> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type}, (proxy, method, arguments) -> {
            String key = getKey(method);

            if (key == null) {
                throw new UnsupportedOperationException();
            }

            return adapt(map.get(key), method.getGenericReturnType());
        }));
    }

    /**
     * Adapts a list instance for typed access.
     *
     * @param <E>
     * The target element type.
     *
     * @param list
     * The list to adapt.
     *
     * @param elementType
     * The target element type.
     *
     * @return
     * An list implementation that will adapt the list's elements as documented for
     * {@link #adapt(Object, Type)}.
     */
    public static <E> List<E> adaptList(List<?> list, Type elementType) {
        if (list == null) {
            throw new IllegalArgumentException();
        }

        if (elementType == null) {
            throw new IllegalArgumentException();
        }

        return new AbstractList<E>() {
            @Override
            public E get(int index) {
                return adapt(list.get(index), elementType);
            }

            @Override
            public int size() {
                return list.size();
            }

            @Override
            public Iterator<E> iterator() {
                return new Iterator<E>() {
                    private Iterator<?> iterator = list.iterator();

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public E next() {
                        return adapt(iterator.next(), elementType);
                    }
                };
            }
        };
    }

    /**
     * Adapts a map instance for typed access.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The target value type.
     *
     * @param map
     * The map to adapt.
     *
     * @param valueType
     * The target value type.
     *
     * @return
     * An map implementation that will adapt the map's values as documented for
     * {@link #adapt(Object, Type)}.
     */
    public static <K, V> Map<K, V> adaptMap(Map<K, ?> map, Type valueType) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        if (valueType == null) {
            throw new IllegalArgumentException();
        }

        return new AbstractMap<K, V>() {
            @Override
            public V get(Object key) {
                return adapt(map.get(key), valueType);
            }

            @Override
            public Set<Entry<K, V>> entrySet() {
                return new AbstractSet<Entry<K, V>>() {
                    @Override
                    public int size() {
                        return map.size();
                    }

                    @Override
                    public Iterator<Entry<K, V>> iterator() {
                        return new Iterator<Entry<K, V>>() {
                            private Iterator<? extends Entry<K, ?>> iterator = map.entrySet().iterator();

                            @Override
                            public boolean hasNext() {
                                return iterator.hasNext();
                            }

                            @Override
                            public Entry<K, V> next() {
                                return new Entry<K, V>() {
                                    private Entry<K, ?> entry = iterator.next();

                                    @Override
                                    public K getKey() {
                                        return entry.getKey();
                                    }

                                    @Override
                                    public V getValue() {
                                        return adapt(entry.getValue(), valueType);
                                    }

                                    @Override
                                    public V setValue(V value) {
                                        throw new UnsupportedOperationException();
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
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
     * <li>Any other {@link Number}: "number"</li>
     * <li>{@link CharSequence}: "string"</li>
     * <li>{@link Enum}: "enum"</li>
     * <li>{@link Date}: "date"</li>
     * <li>{@link LocalDate}: "date-local"</li>
     * <li>{@link LocalTime}: "time-local"</li>
     * <li>{@link LocalDateTime}: "datetime-local"</li>
     * <li>{@link URL}: "url"</li>
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
        } else if (type == URL.class) {
            return "url";
        } else if (Iterable.class.isAssignableFrom(type)) {
            return describe(new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[] {Object.class};
                }

                @Override
                public Type getRawType() {
                    return Iterable.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            }, structures);
        } else if (Map.class.isAssignableFrom(type)) {
            return describe(new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[] {Object.class, Object.class};
                }

                @Override
                public Type getRawType() {
                    return Map.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            }, structures);
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
