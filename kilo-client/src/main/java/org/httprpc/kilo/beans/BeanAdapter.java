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

package org.httprpc.kilo.beans;

import org.httprpc.kilo.Required;
import org.httprpc.kilo.util.Optionals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@link Map} adapter for Java bean types.
 */
public class BeanAdapter extends AbstractMap<String, Object> {
    /**
     * Represents a bean property.
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
         * The property's accessor, or {@code null} if no accessor is defined.
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
        public Collection<Method> getMutators() {
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
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<>() {
                Iterator<?> iterator = iterable.iterator();

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

    // List adapter
    private static class ListAdapter extends IterableAdapter {
        List<?> list;

        ListAdapter(List<?> list, Map<Class<?>, Map<String, Property>> propertyCache) {
            super(list, propertyCache);

            this.list = list;
        }

        @Override
        public Object get(int index) {
            return adapt(list.get(index), propertyCache);
        }

        @Override
        public int size() {
            return list.size();
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
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return map.size();
                }

                @Override
                public Iterator<Entry<Object, Object>> iterator() {
                    return new Iterator<>() {
                        Iterator<? extends Entry<?, ?>> iterator = map.entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Entry<Object, Object> next() {
                            return new Entry<>() {
                                Entry<?, ?> entry = iterator.next();

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
                if (method.getParameterCount() > 0) {
                    throw new UnsupportedOperationException();
                }

                var propertyName = getPropertyName(method);

                if (propertyName == null) {
                    throw new UnsupportedOperationException("Method is not an accessor.");
                }

                var key = Optionals.coalesce(Optionals.map(method.getAnnotation(Key.class), Key::value), propertyName);

                var value = map.get(key);

                if (method.getAnnotation(Required.class) != null && value == null) {
                    throw new UnsupportedOperationException(String.format("Property \"%s\" is not defined.", key));
                }

                return coerce(value, method.getGenericReturnType());
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

        var type = bean.getClass();

        if (Proxy.class.isAssignableFrom(type)) {
            var interfaces = type.getInterfaces();

            if (interfaces.length != 1) {
                throw new UnsupportedOperationException();
            }

            type = interfaces[0];
        }

        properties = propertyCache.get(type);

        if (properties == null) {
            properties = getProperties(type);

            propertyCache.put(type, properties);
        }
    }

    /**
     * Gets a bean property value.
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var property = properties.get(key);

        if (property == null || property.accessor == null) {
            return null;
        }

        try {
            var value = property.accessor.invoke(bean);

            if (property.accessor.getAnnotation(Required.class) != null && value == null) {
                throw new IllegalStateException(String.format("Property \"%s\" cannot be null.", key));
            }

            return adapt(value, propertyCache);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Sets a bean property value.
     * {@inheritDoc}
     */
    @Override
    public Object put(String key, Object value) {
        var property = properties.get(key);

        if (property == null) {
            throw new UnsupportedOperationException();
        }

        if (property.accessor.getAnnotation(Required.class) != null && value == null) {
            throw new IllegalArgumentException(String.format("Property \"%s\" is required.", key));
        }

        var i = 0;

        for (var mutator : property.mutators) {
            try {
                mutator.invoke(bean, coerce(value, mutator.getGenericParameterTypes()[0]));
            } catch (Exception exception) {
                i++;
            }
        }

        if (i == property.mutators.size()) {
            throw new UnsupportedOperationException();
        }

        return null;
    }

    /**
     * Enumerates bean property values.
     * {@inheritDoc}
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public int size() {
                return properties.size();
            }

            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<>() {
                    Iterator<Entry<String, Property>> iterator = properties.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<String, Object> next() {
                        var entry = iterator.next();

                        var key = entry.getKey();

                        try {
                            var property = entry.getValue();

                            var value = property.accessor.invoke(bean);

                            if (property.accessor.getAnnotation(Required.class) != null && value == null) {
                                throw new IllegalStateException(String.format("Property \"%s\" cannot be null.", key));
                            }

                            return new SimpleImmutableEntry<>(key, adapt(value, propertyCache));
                        } catch (IllegalAccessException | InvocationTargetException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                };
            }
        };
    }

    /**
     * <p>Adapts a value for loose typing. If the value is {@code null} or an
     * instance of one of the following types, it is returned as is:</p>
     *
     * <ul>
     * <li>{@link CharSequence}</li>
     * <li>{@link Number}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link Enum}</li>
     * <li>{@link Date}</li>
     * <li>{@link TemporalAccessor}</li>
     * <li>{@link TemporalAmount}</li>
     * <li>{@link UUID}</li>
     * <li>{@link URL}</li>
     * </ul>
     *
     * <p>If the value is an {@link Iterable}, it is wrapped in an adapter that will
     * recursively adapt the iterable's elements. If the value is a {@link Map},
     * it is wrapped in an adapter that will recursively adapt the map's values.
     * Otherwise, the value is assumed to be a bean and is wrapped in a
     * {@link BeanAdapter}.</p>
     *
     * @param value
     * The value to adapt.
     *
     * @return
     * The adapted value.
     */
    public static Object adapt(Object value) {
        return adapt(value, new HashMap<>());
    }

    private static Object adapt(Object value, Map<Class<?>, Map<String, Property>> propertyCache) {
        if (value == null
            || value instanceof CharSequence
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof Enum<?>
            || value instanceof Date
            || value instanceof TemporalAccessor
            || value instanceof TemporalAmount
            || value instanceof UUID
            || value instanceof URL) {
            return value;
        } else if (value instanceof Iterable<?>) {
            if (value instanceof List<?>) {
                return new ListAdapter((List<?>)value, propertyCache);
            } else {
                return new IterableAdapter((Iterable<?>)value, propertyCache);
            }
        } else if (value instanceof Map<?, ?>) {
            return new MapAdapter((Map<?, ?>)value, propertyCache);
        } else {
            return new BeanAdapter(value, propertyCache);
        }
    }

    /**
     * <p>Coerces a value to a given type.</p>
     *
     * <p>For unparamterized types, if the value is already an instance of the
     * requested type, it is returned as is. Otherwise, if the requested type
     * is one of the following, the return value is obtained via an appropriate
     * conversion method; for example, {@link Number#intValue()},
     * {@link Object#toString()}, or {@link LocalDate#parse(CharSequence)}:</p>
     *
     * <ul>
     * <li>{@link Byte} or {@code byte}</li>
     * <li>{@link Short} or {@code short}</li>
     * <li>{@link Integer} or {@code int}</li>
     * <li>{@link Long} or {@code long}</li>
     * <li>{@link Float} or {@code float}</li>
     * <li>{@link Double} or {@code double}</li>
     * <li>{@link Boolean} or {@code boolean}</li>
     * <li>{@link String}</li>
     * <li>{@link Date}</li>
     * <li>{@link Instant}</li>
     * <li>{@link LocalDate}</li>
     * <li>{@link LocalTime}</li>
     * <li>{@link LocalDateTime}</li>
     * <li>{@link Duration}</li>
     * <li>{@link Period}</li>
     * <li>{@link UUID}</li>
     * <li>{@link URL}</li>
     * </ul>
     *
     * <p>If the target type is an {@link Enum}, the resulting value is the first
     * constant whose string representation matches the value's string
     * representation.</p>
     *
     * <p>If none of the previous conditions apply, the target type is assumed to
     * be a bean. The provided value is assumed to be a map and is converted as
     * follows:</p>
     *
     * <ul>
     * <li>If the target type is an interface, the return value is a proxy
     * implementation of the interface that maps accessor methods to entries in
     * the map. {@link Object} methods are delegated to the underlying map.</li>
     * <li>If the target type is a concrete class, an instance of the type is
     * dynamically created and populated using the entries in the map.</li>
     * </ul>
     *
     * <p>For parameterized types, if the target type is {@link List} or
     * {@link Map}, the value is wrapped in an instance of the same type that
     * automatically coerces its contents. Other parameterized types are not
     * supported.</p>
     *
     * <p>For reference types, {@code null} values are returned as is. For
     * numeric or boolean primitives, they are converted to 0 or
     * {@code false}, respectively.</p>
     *
     * @param <T>
     * The target type.
     *
     * @param value
     * The value to coerce.
     *
     * @param rawType
     * The raw target type.
     *
     * @param actualTypeArguments
     * The actual target type arguments.
     *
     * @return
     * The coerced value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object value, Class<T> rawType, Type... actualTypeArguments) {
        return (T)coerce(value, typeOf(rawType, actualTypeArguments));
    }

    @SuppressWarnings("unchecked")
    private static Object coerce(Object value, Type type) {
        if (type instanceof Class<?>) {
            return coerceValue(value, (Class<?>)type);
        } else if (type instanceof ParameterizedType) {
            var parameterizedType = (ParameterizedType)type;

            var rawType = parameterizedType.getRawType();
            var actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (rawType == List.class) {
                if (value == null) {
                    return null;
                } else if (value instanceof List<?>) {
                    var list = (List<?>)value;

                    var elementType = actualTypeArguments[0];

                    return list.stream()
                        .map(element -> coerce(element, elementType))
                        .collect(Collectors.toCollection(() -> new ArrayList<>(list.size())));
                } else {
                    throw new IllegalArgumentException("Value is not a list.");
                }
            } else if (rawType == Map.class) {
                if (value == null) {
                    return null;
                } else if (value instanceof Map<?, ?>) {
                    var keyType = actualTypeArguments[0];
                    var valueType = actualTypeArguments[1];

                    var map = new LinkedHashMap<>();

                    for (var entry : ((Map<?, ?>)value).entrySet()) {
                        map.put(coerce(entry.getKey(), keyType), coerce(entry.getValue(), valueType));
                    }

                    return map;
                } else {
                    throw new IllegalArgumentException("Value is not a map.");
                }
            } else {
                throw new UnsupportedOperationException("Unsupported parameterized type.");
            }
        } else {
            throw new UnsupportedOperationException("Unsupported type.");
        }
    }

    private static Object coerceValue(Object value, Class<?> type) {
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
            } else if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {
                return ((Number)value).longValue() != 0;
            } else if (value instanceof Float
                || value instanceof Double) {
                return ((Number)value).doubleValue() != 0.0;
            } else {
                return Boolean.parseBoolean(value.toString());
            }
        } else {
            if (value == null) {
                return null;
            }

            if (type == String.class) {
                return value.toString();
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
            } else if (type == Duration.class) {
                if (value instanceof Number) {
                    return Duration.ofMillis(((Number)value).longValue());
                } else {
                    return Duration.parse(value.toString());
                }
            } else if (type == Period.class) {
                return Period.parse(value.toString());
            } else if (type == UUID.class) {
                return UUID.fromString(value.toString());
            } else if (type == URL.class) {
                try {
                    return new URL(value.toString());
                } catch (MalformedURLException exception) {
                    throw new IllegalArgumentException(exception);
                }
            } else if (type.isEnum()) {
                var name = value.toString();

                var fields = type.getDeclaredFields();

                for (var i = 0; i < fields.length; i++) {
                    var field = fields[i];

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
            } else {
                if (!(value instanceof Map<?, ?>)) {
                    throw new IllegalArgumentException();
                }

                var map = (Map<?, ?>)value;

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

                    var beanAdapter = new BeanAdapter(bean);

                    for (var entry : beanAdapter.properties.entrySet()) {
                        var key = entry.getKey();

                        beanAdapter.put(key, map.get(key));
                    }

                    return bean;
                }
            }
        }
    }

    /**
     * Generates a type descriptor.
     *
     * @param rawType
     * The raw type.
     *
     * @param actualTypeArguments
     * The actual type arguments.
     *
     * @return
     * A type that describes the given raw type and actual type arguments.
     */
    public static Type typeOf(Class<?> rawType, Type... actualTypeArguments) {
        if (rawType == null || actualTypeArguments == null) {
            throw new IllegalArgumentException();
        }

        var typeParameters = rawType.getTypeParameters();

        if (typeParameters.length != actualTypeArguments.length) {
            throw new IllegalArgumentException("Type parameter mismatch.");
        }

        if (typeParameters.length == 0) {
            return rawType;
        } else {
            for (var i = 0; i < actualTypeArguments.length; i++) {
                if (actualTypeArguments[i] == null) {
                    throw new IllegalArgumentException();
                }
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
    }

    /**
     * Returns the properties for a given type.
     *
     * @param type
     * The bean type.
     *
     * @return
     * The properties defined by the requested type.
     */
    public static Map<String, Property> getProperties(Class<?> type) {
        Map<String, Property> properties = new HashMap<>();

        var methods = type.getMethods();

        for (var i = 0; i < methods.length; i++) {
            var method = methods[i];

            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            var propertyName = getPropertyName(method);

            if (propertyName == null) {
                continue;
            }

            var property = properties.get(propertyName);

            if (property == null) {
                property = new Property();

                properties.put(propertyName, property);
            }

            if (method.getParameterCount() == 0) {
                property.accessor = method;
            } else {
                property.mutators.add(method);
            }
        }

        return properties.entrySet().stream()
            .filter(entry -> {
                var accessor = entry.getValue().getAccessor();

                if (accessor == null) {
                    throw new UnsupportedOperationException("Missing accessor.");
                }

                return (accessor.getAnnotation(Ignore.class) == null);
            }).collect(Collectors.toMap(entry -> {
                var key = entry.getValue().getAccessor().getAnnotation(Key.class);

                return Optionals.coalesce(Optionals.map(key, Key::value), entry.getKey());
            }, Map.Entry::getValue, (v1, v2) -> {
                throw new UnsupportedOperationException("Duplicate key.");
            }, TreeMap::new));
    }

    private static String getPropertyName(Method method) {
        if (method.isBridge()) {
            return null;
        }

        var methodName = method.getName();
        var returnType = method.getReturnType();
        var parameterCount = method.getParameterCount();

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
            prefix = null;
        }

        if (prefix == null) {
            return null;
        }

        var j = prefix.length();
        var n = methodName.length();

        if (j == n) {
            return null;
        }

        var c = methodName.charAt(j++);

        if (j == n || Character.isLowerCase(methodName.charAt(j))) {
            c = Character.toLowerCase(c);
        }

        return c + methodName.substring(j);
    }
}
