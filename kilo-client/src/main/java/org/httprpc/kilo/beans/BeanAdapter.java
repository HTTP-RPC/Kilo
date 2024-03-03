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

import org.httprpc.kilo.Name;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provides access to Java bean properties via the {@link Map} interface.
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

        /**
         * Indicates that the property is read-only.
         *
         * @return
         * {@code true} if the property is read-only; {@code false}, otherwise.
         */
        public boolean isReadOnly() {
            return mutators.isEmpty();
        }
    }

    // List adapter
    private static class ListAdapter extends AbstractList<Object> {
        List<?> list;

        ListAdapter(List<?> list) {
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
            return new Iterator<>() {
                Iterator<?> iterator = list.iterator();

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
        Map<?, ?> map;

        MapAdapter(Map<?, ?> map) {
            this.map = map;
        }

        @Override
        public Object get(Object key) {
            return adapt(map.get(key));
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
        }
    }

    // Record adapter
    private static class RecordAdapter extends AbstractMap<String, Object> {
        Object value;

        Map<String, Property> properties;

        RecordAdapter(Object value) {
            this.value = value;

            properties = getProperties(value.getClass());
        }

        @Override
        public Object get(Object key) {
            var property = properties.get(key);

            if (property == null) {
                return null;
            }

            try {
                return adapt(property.accessor.invoke(value));
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        }

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

                                return new SimpleImmutableEntry<>(key, adapt(property.accessor.invoke(value)));
                            } catch (IllegalAccessException | InvocationTargetException exception) {
                                throw new RuntimeException(exception);
                            }
                        }
                    };
                }
            };
        }
    }

    // Typed invocation handler
    private static class TypedInvocationHandler implements InvocationHandler {
        Map<?, ?> map;
        Class<?> type;

        Map<String, Method> accessors = new HashMap<>();

        TypedInvocationHandler(Map<?, ?> map, Class<?> type) {
            this.map = map;
            this.type = type;

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

                if (method.getParameterCount() == 0) {
                    accessors.put(propertyName, method);
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, arguments);
            } else {
                var propertyName = getPropertyName(method);

                if (propertyName == null) {
                    throw new UnsupportedOperationException("Invalid method.");
                }

                if (method.getParameterCount() == 0) {
                    var key = getKey(method, propertyName);

                    var value = map.get(key);

                    if (method.getAnnotation(Required.class) != null && value == null) {
                        throw new UnsupportedOperationException("Value is not defined.");
                    }

                    return toGenericType(value, method.getGenericReturnType());
                } else {
                    var accessor = accessors.get(propertyName);

                    if (accessor == null) {
                        throw new UnsupportedOperationException("Missing accessor.");
                    }

                    var key = getKey(accessor, propertyName);

                    var value = arguments[0];

                    if (accessor.getAnnotation(Required.class) != null && value == null) {
                        throw new IllegalArgumentException("Value is required.");
                    }

                    ((Map<Object, Object>)map).put(key, adapt(value));

                    return null;
                }
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

            if (!(object instanceof TypedInvocationHandler typedInvocationHandler) || type != typedInvocationHandler.type) {
                return false;
            }

            for (var entry : accessors.entrySet()) {
                var propertyName = entry.getKey();
                var accessor = entry.getValue();

                var key = getKey(accessor, propertyName);

                var type = accessor.getGenericReturnType();

                var value1 = toGenericType(map.get(key), type);
                var value2 = toGenericType(typedInvocationHandler.map.get(key), type);

                if (!Objects.equals(value1, value2)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String toString() {
            return map.toString();
        }

        static String getKey(Method accessor, String propertyName) {
            return Optionals.map(accessor.getAnnotation(Name.class), Name::value, propertyName);
        }
    }

    // Container type
    private static class ContainerType implements ParameterizedType {
        Type[] actualTypeArguments;
        Type rawType;

        ContainerType(Type[] actualTypeArguments, Type rawType) {
            this.actualTypeArguments = actualTypeArguments;
            this.rawType = rawType;
        }

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
    }

    private Object bean;

    private Map<String, Property> properties;

    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    private static final String SET_PREFIX = "set";

    private static Map<Class<?>, Map<String, Property>> propertyCache = new HashMap<>();

    /**
     * Constructs a new bean adapter.
     *
     * @param bean
     * The source bean.
     */
    public BeanAdapter(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException();
        }

        this.bean = bean;

        var type = bean.getClass();

        if (Proxy.class.isAssignableFrom(type)) {
            var interfaces = type.getInterfaces();

            if (interfaces.length != 1) {
                throw new UnsupportedOperationException();
            }

            type = interfaces[0];
        }

        properties = getProperties(type);
    }

    /**
     * Gets a property value.
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var property = properties.get(key);

        if (property == null) {
            return null;
        }

        try {
            var value = property.accessor.invoke(bean);

            if (property.accessor.getAnnotation(Required.class) != null && value == null) {
                throw new UnsupportedOperationException("Value is not defined.");
            }

            return adapt(value);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Sets a property value.
     * {@inheritDoc}
     */
    @Override
    public Object put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var property = properties.get(key);

        if (property == null) {
            throw new UnsupportedOperationException();
        }

        if (property.accessor.getAnnotation(Required.class) != null && value == null) {
            throw new IllegalArgumentException("Value is required.");
        }

        var i = 0;

        for (var mutator : property.mutators) {
            try {
                mutator.invoke(bean, toGenericType(value, mutator.getGenericParameterTypes()[0]));
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
     * Enumerates property values.
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
                                throw new UnsupportedOperationException("Required property is not defined.");
                            }

                            return new SimpleImmutableEntry<>(key, adapt(value));
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
     * <li>{@link Number}</li>
     * <li>{@link Boolean}</li>
     * <li>{@link String}</li>
     * <li>{@link Enum}</li>
     * <li>{@link Date}</li>
     * <li>{@link TemporalAccessor}</li>
     * <li>{@link TemporalAmount}</li>
     * <li>{@link UUID}</li>
     * <li>{@link URL}</li>
     * </ul>
     *
     * <p>If the value is a {@link List}, it is wrapped in an adapter that
     * will recursively adapt the list's elements.</p>
     *
     * <p>If the value is a {@link Map}, it is wrapped in an adapter that will
     * recursively adapt the map's values. Map keys are not adapted.</p>
     *
     * <p>If the value is a {@link Record}, it is wrapped in an adapter that
     * will recursively adapt the record's fields.</p>
     *
     * <p>If none of the previous conditions apply, the value is assumed to be
     * a bean and is wrapped in a {@link BeanAdapter}.</p>
     *
     * @param value
     * The value to adapt.
     *
     * @return
     * The adapted value.
     */
    public static Object adapt(Object value) {
        if (value == null
            || value instanceof Number
            || value instanceof Boolean
            || value instanceof String
            || value instanceof Enum
            || value instanceof Date
            || value instanceof TemporalAccessor
            || value instanceof TemporalAmount
            || value instanceof UUID
            || value instanceof URL) {
            return value;
        } else if (value instanceof List<?> list) {
            return new ListAdapter(list);
        } else if (value instanceof Map<?, ?> map) {
            return new MapAdapter(map);
        } else if (value instanceof Record) {
            return new RecordAdapter(value);
        } else {
            return new BeanAdapter(value);
        }
    }

    /**
     * <p>Coerces a value to a given type. If the value is already an instance
     * of the requested type, it is returned as is. Otherwise, if the requested
     * type is one of the following, the return value is obtained via an
     * appropriate conversion method; for example, {@link Number#intValue()},
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
     * <p>If the target type is an {@link Enum}, the resulting value is the
     * first constant whose string representation matches the value's string
     * representation.</p>
     *
     * <p>If none of the previous conditions apply, the provided value is
     * assumed to be a map. If the if the target type is a {@link Record}, the
     * resulting value is instantiated via the type's canonical constructor
     * using the entries in the map. Otherwise, the target type is assumed to
     * be a bean:</p>
     *
     * <ul>
     * <li>If the type is an interface, the return value is a proxy
     * implementation of the interface that maps accessor and mutator methods
     * to entries in the map. {@link Object} methods are delegated to the
     * underlying map.</li>
     * <li>If the type is a concrete class, an instance of the type is
     * dynamically created and populated using the entries in the map.</li>
     * </ul>
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
     * @param type
     * The target type.
     *
     * @return
     * The coerced value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object value, Class<T> type) {
        return (T)toGenericType(value, type);
    }

    /**
     * Coerces list elements to a given type.
     *
     * @param <E>
     * The element type.
     *
     * @param list
     * The source list.
     *
     * @param elementType
     * The element type.
     *
     * @return
     * A list containing the coerced elements.
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> coerceList(List<?> list, Class<E> elementType) {
        return (List<E>)toGenericType(list, typeOfList(elementType));
    }

    /**
     * Coerces map values to a given type.
     *
     * @param <K>
     * The key type.
     *
     * @param <V>
     * The value type.
     *
     * @param map
     * The source map.
     *
     * @param valueType
     * The value type.
     *
     * @return
     * A map containing the coerced values.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> coerceMap(Map<K, ?> map, Class<V> valueType) {
        return (Map<K, V>)toGenericType(map, typeOfMap(valueType));
    }

    /**
     * Converts a value to a generic type.
     *
     * @param value
     * The value to convert.
     *
     * @param type
     * The target type.
     *
     * @return
     * The converted value.
     */
    public static Object toGenericType(Object value, Type type) {
        if (type instanceof Class<?> rawType) {
            return toRawType(value, rawType);
        } else if (type instanceof ParameterizedType parameterizedType) {
            var rawType = parameterizedType.getRawType();
            var actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (rawType == List.class) {
                if (value == null) {
                    return null;
                } else if (value instanceof List<?> list) {
                    var elementType = actualTypeArguments[0];

                    var genericList = new ArrayList<>(list.size());

                    for (var element : list) {
                        genericList.add(toGenericType(element, elementType));
                    }

                    return genericList;
                } else {
                    throw new IllegalArgumentException("Value is not a list.");
                }
            } else if (rawType == Map.class) {
                if (value == null) {
                    return null;
                } else if (value instanceof Map<?, ?> map) {
                    var keyType = actualTypeArguments[0];
                    var valueType = actualTypeArguments[1];

                    var genericMap = new LinkedHashMap<>();

                    for (var entry : map.entrySet()) {
                        genericMap.put(toGenericType(entry.getKey(), keyType), toGenericType(entry.getValue(), valueType));
                    }

                    return genericMap;
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

    private static Object toRawType(Object value, Class<?> type) {
        if (type.isInstance(value)) {
            return value;
        } else if (type == Byte.TYPE || type == Byte.class) {
            if (value == null) {
                return (type == Byte.TYPE) ? Byte.valueOf((byte)0) : null;
            } else if (value instanceof Number number) {
                return number.byteValue();
            } else {
                return Byte.parseByte(value.toString());
            }
        } else if (type == Short.TYPE || type == Short.class) {
            if (value == null) {
                return (type == Short.TYPE) ? Short.valueOf((short)0) : null;
            } else if (value instanceof Number number) {
                return number.shortValue();
            } else {
                return Short.parseShort(value.toString());
            }
        } else if (type == Integer.TYPE || type == Integer.class) {
            if (value == null) {
                return (type == Integer.TYPE) ? Integer.valueOf(0) : null;
            } else if (value instanceof Number number) {
                return number.intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } else if (type == Long.TYPE || type == Long.class) {
            if (value == null) {
                return (type == Long.TYPE) ? Long.valueOf(0) : null;
            } else if (value instanceof Number number) {
                return number.longValue();
            } else {
                return Long.parseLong(value.toString());
            }
        } else if (type == Float.TYPE || type == Float.class) {
            if (value == null) {
                return (type == Float.TYPE) ? Float.valueOf(0) : null;
            } else if (value instanceof Number number) {
                return number.floatValue();
            } else {
                return Float.parseFloat(value.toString());
            }
        } else if (type == Double.TYPE || type == Double.class) {
            if (value == null) {
                return (type == Double.TYPE) ? Double.valueOf(0) : null;
            } else if (value instanceof Number number) {
                return number.doubleValue();
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
                if (value instanceof Number number) {
                    return new Date(number.longValue());
                } else {
                    return new Date(Long.parseLong(value.toString()));
                }
            } else if (type == Instant.class) {
                if (value instanceof Date date) {
                    return date.toInstant();
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
                if (value instanceof Number number) {
                    return Duration.ofMillis(number.longValue());
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
                return toEnum(value, type);
            } else if (type.isRecord()) {
                return toRecord(value, type);
            } else {
                return toBean(value, type);
            }
        }
    }

    private static Object toEnum(Object value, Class<?> type) {
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
    }

    private static Object toRecord(Object value, Class<?> type) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException();
        }

        var recordComponents = type.getRecordComponents();

        var parameterTypes = new Class<?>[recordComponents.length];
        var arguments = new Object[recordComponents.length];

        for (var i = 0; i < recordComponents.length; i++) {
            var recordComponent = recordComponents[i];

            parameterTypes[i] = recordComponent.getType();

            var name = recordComponent.getName();
            var accessor = recordComponent.getAccessor();

            Object argument;
            if (accessor.getAnnotation(Internal.class) == null) {
                var genericType = recordComponent.getGenericType();

                argument = toGenericType(map.get(Optionals.map(accessor.getAnnotation(Name.class), Name::value, name)), genericType);
            } else {
                argument = null;
            }

            if (accessor.getAnnotation(Required.class) != null && argument == null) {
                throw new IllegalArgumentException("Required component is not defined.");
            }

            arguments[i] = argument;
        }

        Constructor<?> constructor;
        try {
            constructor = type.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new RuntimeException(exception);
        }

        try {
            return constructor.newInstance(arguments);
        } catch (InstantiationException | IllegalAccessException  | InvocationTargetException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static Object toBean(Object value, Class<?> type) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException();
        }

        if (type.isInterface()) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, new TypedInvocationHandler(map, type)));
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
                if (entry.getValue().isReadOnly()) {
                    continue;
                }

                var key = entry.getKey();

                beanAdapter.put(key, map.get(key));
            }

            return bean;
        }
    }

    /**
     * Generates a list type descriptor.
     *
     * @param elementType
     * The element type.
     *
     * @return
     * A list type descriptor.
     */
    public static Type typeOfList(Class<?> elementType) {
        if (elementType == null) {
            throw new IllegalArgumentException();
        }

        return new ContainerType(new Type[] {elementType}, List.class);
    }

    /**
     * Generates a map type descriptor.
     *
     * @param valueType
     * The value type.
     *
     * @return
     * A map type descriptor.
     */
    public static Type typeOfMap(Class<?> valueType) {
        if (valueType == null) {
            throw new IllegalArgumentException();
        }

        return new ContainerType(new Type[] {Object.class, valueType}, Map.class);
    }

    /**
     * Returns the properties for a given type, sorted by key.
     *
     * @param type
     * The bean type.
     *
     * @return
     * The properties defined by the requested type.
     */
    public synchronized static Map<String, Property> getProperties(Class<?> type) {
        var properties = propertyCache.get(type);

        if (properties == null) {
            properties = new HashMap<>();

            if (type.isRecord()) {
                var recordComponents = type.getRecordComponents();

                for (var i = 0; i < recordComponents.length; i++) {
                    var recordComponent = recordComponents[i];

                    var property = new Property();

                    property.accessor = recordComponent.getAccessor();

                    properties.put(recordComponent.getName(), property);
                }
            } else {
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
            }

            properties = properties.entrySet().stream().filter(entry -> {
                var accessor = entry.getValue().getAccessor();

                if (accessor == null) {
                    throw new UnsupportedOperationException("Missing accessor.");
                }

                return (accessor.getAnnotation(Internal.class) == null);
            }).collect(Collectors.toMap(entry -> {
                var key = entry.getValue().getAccessor().getAnnotation(Name.class);

                return Optionals.map(key, Name::value, entry.getKey());
            }, Map.Entry::getValue, (v1, v2) -> {
                throw new UnsupportedOperationException("Duplicate key.");
            }, TreeMap::new));

            propertyCache.put(type, properties);
        }

        return properties;
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
            && (returnType == Boolean.TYPE || returnType == Boolean.class)
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
