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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * {@link Map} adapter for Java bean types.
 */
public class BeanAdapter extends AbstractMap<String, Object> {
    /**
     * Class representing a bean property.
     */
    public static class Property {
        private Method accessor = null;
        private List<Method> mutators = new LinkedList<>();

        private Property() {
        }

        /**
         * Returns the property's accessor.
         *
         * @return
         * The property's accessor, or <code>null</code> if no accessor is defined.
         */
        public Method getAccessor() {
            return accessor;
        }

        /**
         * Returns the property's mutators.
         *
         * @return
         * The property's mutators.
         */
        public List<Method> getMutators() {
            return Collections.unmodifiableList(mutators);
        }
    }

    // Iterable adapter
    private static class IterableAdapter extends AbstractList<Object> {
        Iterable<?> iterable;
        Map<Class<?>, Map<String, Property>> propertyCache;

        IterableAdapter(Iterable<?> iterable, Map<Class<?>, Map<String, Property>> propertyCache) {
            this.iterable = iterable;
            this.propertyCache = propertyCache;
        }

        @Override
        public Object get(int index) {
            return adapt(getList().get(index), propertyCache);
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
                    return adapt(iterator.next(), propertyCache);
                }
            };
        }
    }

    // Map adapter
    private static class MapAdapter extends AbstractMap<Object, Object> {
        Map<?, ?> map;
        Map<Class<?>, Map<String, Property>> propertyCache;

        MapAdapter(Map<?, ?> map, Map<Class<?>, Map<String, Property>> propertyCache) {
            this.map = map;
            this.propertyCache = propertyCache;
        }

        @Override
        public Object get(Object key) {
            return adapt(map.get(key), propertyCache);
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
                                    return adapt(entry.getValue(), propertyCache);
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

    // Typed invocation handler
    private static class TypedInvocationHandler implements InvocationHandler {
        Map<?, ?> map;

        TypedInvocationHandler(Map<?, ?> map) {
            this.map = map;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, arguments);
            } else {
                String key = getKey(method);

                if (key == null || method.getParameterCount() > 0) {
                    throw new UnsupportedOperationException();
                }

                return adapt(map.get(key), method.getGenericReturnType());
            }
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Proxy) {
                object = Proxy.getInvocationHandler(object);
            }

            if (!(object instanceof TypedInvocationHandler)) {
                return false;
            }

            return map.equals(((TypedInvocationHandler)object).map);
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    private Object bean;
    private Map<Class<?>, Map<String, Property>> propertyCache;

    private Map<String, Property> properties;

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    private static final String SET_PREFIX = "set";

    /**
     * Constructs a new bean adapter.
     *
     * @param bean
     * The source bean.
     */
    public BeanAdapter(Object bean) {
        this(bean, new HashMap<>());
    }

    private BeanAdapter(Object bean, Map<Class<?>, Map<String, Property>> propertyCache) {
        if (bean == null) {
            throw new IllegalArgumentException();
        }

        this.bean = bean;
        this.propertyCache = propertyCache;

        Class<?> type = bean.getClass();

        if (Proxy.class.isAssignableFrom(type)) {
            Class<?>[] interfaces = type.getInterfaces();

            if (interfaces.length == 0) {
                throw new IllegalArgumentException();
            }

            type = interfaces[0];
        }

        properties = propertyCache.get(type);

        if (properties == null) {
            properties = getProperties(type);

            propertyCache.put(type, properties);
        }
    }

    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        Property property = properties.get(key);

        Object value;
        if (property != null && property.accessor != null) {
            try {
                value = adapt(property.accessor.invoke(bean), propertyCache);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            value = null;
        }

        return value;
    }

    @Override
    public Object put(String key, Object value) {
        Property property = properties.get(key);

        if (property == null) {
            throw new UnsupportedOperationException();
        }

        int i = 0;

        for (Method mutator : property.mutators) {
            try {
                mutator.invoke(bean, (Object)adapt(value, mutator.getGenericParameterTypes()[0]));
            } catch (Exception exception) {
                i++;
            }
        }

        if (i == property.mutators.size()) {
            throw new UnsupportedOperationException();
        }

        return null;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<Entry<String, Object>>() {
            @Override
            public int size() {
                return properties.size();
            }

            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<Entry<String, Object>>() {
                    private Iterator<Entry<String, Property>> iterator = properties.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<String, Object> next() {
                        Entry<String, Property> entry = iterator.next();

                        Object value;
                        try {
                            Property property = entry.getValue();

                            if (property.accessor != null) {
                                value = adapt(property.accessor.invoke(bean), propertyCache);
                            } else {
                                value = null;
                            }
                        } catch (IllegalAccessException | InvocationTargetException exception) {
                            throw new RuntimeException(exception);
                        }

                        return new SimpleImmutableEntry<>(entry.getKey(), value);
                    }
                };
            }
        };
    }

    /**
     * Adapts a value for loosely typed access. If the value is <code>null</code>
     * or an instance of one of the following types, it is returned as is:
     *
     * <ul>
     * <li>{@link CharSequence}</li>
     * <li>{@link Number}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Enum}</li>
     * <li>{@link Date}</li>
     * <li>{@link TemporalAccessor}</li>
     * <li>{@link UUID}</li>
     * <li>{@link URL}</li>
     * </ul>
     *
     * If the value is an {@link Iterable}, it is wrapped in an adapter that will
     * recursively adapt the iterable's elements. If the value is a {@link Map},
     * it is wrapped in an adapter that will recursively adapt the map's values.
     * Otherwise, the value is assumed to be a bean and is wrapped in a
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

    private static Object adapt(Object value, Map<Class<?>, Map<String, Property>> propertyCache) {
        if (value == null
            || value instanceof CharSequence
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Enum<?>
            || value instanceof Date
            || value instanceof TemporalAccessor
            || value instanceof UUID
            || value instanceof URL) {
            return value;
        } else if (value instanceof Iterable<?>) {
            return new IterableAdapter((Iterable<?>)value, propertyCache);
        } else if (value instanceof Map<?, ?>) {
            return new MapAdapter((Map<?, ?>)value, propertyCache);
        } else {
            return new BeanAdapter(value, propertyCache);
        }
    }

    /**
     * Adapts a value for strongly typed access.
     *
     * <ul>
     * <li>If the value is already an instance of the given type, it is returned
     * as is.</li>
     * <li>If the target type is a number or boolean, the value is parsed or
     * coerced using the appropriate conversion method. Missing or <code>null</code>
     * values are automatically converted to <code>0</code> or <code>false</code>
     * for primitive argument types.</li>
     * <li>If the target type is {@link String}, the value is adapted via
     * {@link Object#toString()}.</li>
     * <li>If the target type is an {@link Enum}, the adapted value is the first
     * constant whose string representation matches the value's string
     * representation.</li>
     * <li>If the target type is {@link Date}, the value is coerced to a long
     * value and passed to {@link Date#Date(long)}.</li>
     * <li>If the target type is {@link Instant} and the value is an instance of
     * {@link Date}, the value is adapted via {@link Date#toInstant()}. Otherwise,
     * the value's string representation is parsed using
     * {@link Instant#parse(CharSequence)}.</li>
     * <li>If the target type is {@link LocalDate}, the value's string
     * representation is parsed using {@link LocalDate#parse(CharSequence)}.</li>
     * <li>If the target type is {@link LocalTime}, the value's string
     * representation is parsed using {@link LocalTime#parse(CharSequence)}.</li>
     * <li>If the target type is {@link LocalDateTime}, the value's string
     * representation is parsed using {@link LocalDateTime#parse(CharSequence)}.</li>
     * <li>If the target type is {@link UUID}, the value's string representation is
     * adapted via {@link UUID#fromString(String)}.</li>
     * <li>If the target type is {@link URL}, the value's string representation is
     * adapted via {@link URL#URL(String)}.</li>
     * <li>If the target type is {@link List} or {@link Map}, the value is wrapped
     * in an adapter of the same type that automatically adapts its elements.</li>
     * </ul>
     *
     * Otherwise, the value is assumed to be a map and is adapted as follows:
     *
     * <ul>
     * <li>If the target type is an interface, the return value is a proxy
     * implementation of the interface that maps accessor methods to entries in
     * the map. `Object` methods are delegated to the underlying map.</li>
     * <li>If the target type is a concrete class, an instance of the type is
     * dynamically created and populated using the entries in the map.</li>
     * </ul>
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
     * The adapted value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T adapt(Object value, Type type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        if (type instanceof Class<?>) {
            return (T)adaptValue(value, (Class<?>)type);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType)type;

            return adapt(value, wildcardType.getUpperBounds()[0]);
        } else if (type instanceof ParameterizedType) {
            if (value != null) {
                ParameterizedType parameterizedType = (ParameterizedType)type;

                Type rawType = parameterizedType.getRawType();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

                if (rawType == List.class) {
                    return (T)adaptGenericList((List<?>)value, actualTypeArguments[0]);
                } else if (rawType == Map.class) {
                    return (T)adaptGenericMap((Map<?, ?>)value, actualTypeArguments[1]);
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

    private static Object adaptValue(Object value, Class<?> type) {
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
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            if (value == null) {
                return (type == Boolean.TYPE) ? Boolean.FALSE : null;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else if (value != null) {
            if (type == String.class) {
                return value.toString();
            } else if (Enum.class.isAssignableFrom(type)) {
                String name = value.toString();

                Field[] fields = type.getDeclaredFields();

                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    if (!field.isEnumConstant()) {
                        continue;
                    }

                    Object constant;
                    try {
                        constant = field.get(null);
                    } catch (IllegalAccessException exception) {
                        throw new RuntimeException(exception);
                    }

                    if (name.equals(constant.toString())) {
                        return constant;
                    }
                }

                throw new IllegalArgumentException();
            } else if (type == Date.class) {
                if (value instanceof Number) {
                    return new Date(((Number)value).longValue());
                } else {
                    return new Date(Long.parseLong(value.toString()));
                }
            } else if (type == Instant.class) {
                if (value instanceof Date) {
                    return ((Date)value).toInstant();
                } else {
                    return Instant.parse(value.toString());
                }
            } else if (type == LocalDate.class) {
                return LocalDate.parse(value.toString());
            } else if (type == LocalTime.class) {
                return LocalTime.parse(value.toString());
            } else if (type == LocalDateTime.class) {
                return LocalDateTime.parse(value.toString());
            } else if (type == UUID.class) {
                return UUID.fromString(value.toString());
            } else if (type == URL.class) {
                try {
                    return new URL(value.toString());
                } catch (MalformedURLException exception) {
                    throw new IllegalArgumentException(exception);
                }
            } else if (value instanceof Map<?, ?>
                && !Iterable.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type)) {
                Map<?, ?> map = (Map<?, ?>)value;

                if (type.isInterface()) {
                    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, new TypedInvocationHandler(map)));
                } else {
                    Constructor<?> constructor;
                    try {
                        constructor = type.getConstructor();
                    } catch (NoSuchMethodException exception) {
                        throw new RuntimeException(exception);
                    }

                    Object bean;
                    try {
                        bean = constructor.newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                        throw new RuntimeException(exception);
                    }

                    BeanAdapter beanAdapter = new BeanAdapter(bean);

                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        try {
                            beanAdapter.put(entry.getKey().toString(), entry.getValue());
                        } catch (UnsupportedOperationException exception) {
                            // No-op
                        }
                    }

                    return bean;
                }
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            return null;
        }
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
    public static <E> List<E> adaptList(List<?> list, Class<? extends E> elementType) {
        return adaptGenericList(list, elementType);
    }

    private static <E> List<E> adaptGenericList(List<?> list, Type elementType) {
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
    public static <K, V> Map<K, V> adaptMap(Map<K, ?> map, Class<? extends V> valueType) {
        return adaptGenericMap(map, valueType);
    }

    private static <K, V> Map<K, V> adaptGenericMap(Map<K, ?> map, Type valueType) {
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
     * Generates a parameterized type descriptor.
     *
     * @param <T>
     * The raw type.
     *
     * @param rawType
     * The raw type.
     *
     * @param actualTypeArguments
     * The actual type arguments.
     *
     * @return
     * A parameterized type that describes the given raw type and actual type
     * arguments.
     */
    public static <T> ParameterizedType typeOf(Class<T> rawType, Type... actualTypeArguments) {
        if (rawType == null) {
            throw new IllegalArgumentException();
        }

        if (actualTypeArguments == null) {
            throw new IllegalArgumentException();
        }

        if (rawType.getTypeParameters().length != actualTypeArguments.length) {
            throw new IllegalArgumentException();
        }

        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return actualTypeArguments;
            }

            @Override
            public Type getRawType() {
                return rawType;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

    /**
     * Returns the properties for a given type.
     *
     * @param type
     * The bean type.
     *
     * @return
     * The properties defined by the given type.
     */
    public static Map<String, Property> getProperties(Class<?> type) {
        Map<String, Property> properties = new TreeMap<>();

        Method[] methods = type.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            String key = getKey(method);

            if (key != null) {
                Property property = properties.get(key);

                if (property == null) {
                    property = new Property();

                    properties.put(key, property);
                }

                if (method.getParameterCount() == 0) {
                    property.accessor = method;
                } else {
                    property.mutators.add(method);
                }
            }
        }

        return properties;
    }

    private static String getKey(Method method) {
        if (method.isBridge()) {
            return null;
        }

        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();
        int parameterCount = method.getParameterCount();

        String prefix;
        if (methodName.startsWith(GET_PREFIX)
            && !(returnType == Void.TYPE || returnType == Void.class)
            && parameterCount == 0) {
            prefix = GET_PREFIX;
        } else if (methodName.startsWith(IS_PREFIX)
            && !(returnType == Void.TYPE || returnType == Void.class)
            && parameterCount == 0) {
            prefix = IS_PREFIX;
        } else if (methodName.startsWith(SET_PREFIX)
            && (returnType == Void.TYPE || returnType == Void.class)
            && parameterCount == 1) {
            prefix = SET_PREFIX;
        } else {
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
}
