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

package org.httprpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for typed results.
 */
public class Result {
    private Map<String, Method> mutators;

    private static final String SET_PREFIX = "set";

    /**
     * Constructs a result.
     */
    public Result() {
        HashMap<String, Method> mutators = new HashMap<>();

        Method[] methods = getClass().getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            if (Result.class.isAssignableFrom(method.getDeclaringClass())) {
                String methodName = method.getName();

                if (methodName.startsWith(SET_PREFIX)) {
                    int j = SET_PREFIX.length();
                    int n = methodName.length();

                    // TODO Call getParameterCount() in Java 8
                    if (j < n && method.getParameterTypes().length == 1) {
                        char c = methodName.charAt(j++);

                        if (j == n || Character.isLowerCase(methodName.charAt(j))) {
                            c = Character.toLowerCase(c);
                        }

                        String key = c + methodName.substring(j);

                        mutators.put(key, method);
                    }
                }
            }
        }

        this.mutators = Collections.unmodifiableMap(mutators);
    }

    /**
     * Sets a property value.
     *
     * @param name
     * The property name.
     *
     * @param value
     * The property value.
     */
    public void set(String name, Object value) {
        Method method = mutators.get(name);

        if (method != null) {
            if (value instanceof Number) {
                Number number = (Number)value;

                Class<?> type = method.getParameterTypes()[0];

                if (type == Byte.TYPE || type == Byte.class) {
                    value = number.byteValue();
                } else if (type == Short.TYPE || type == Short.class) {
                    value = number.shortValue();
                } else if (type == Integer.TYPE || type == Integer.class) {
                    value = number.intValue();
                } else if (type == Long.TYPE || type == Long.class) {
                    value = number.longValue();
                } else if (type == Float.TYPE || type == Float.class) {
                    value = number.floatValue();
                } else if (type == Double.TYPE || type == Double.class) {
                    value = number.doubleValue();
                } else {
                    throw new IllegalArgumentException(type.getName() + " is not a supported numeric type.");
                }
            }

            try {
                method.invoke(this, value);
            } catch (InvocationTargetException | IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        } else {
            setUndefined(name, value);
        }
    }

    /**
     * Called by {@link #set(String, Object)} when a setter is not found for a
     * given property. The default implementation throws an
     * <tt>{@link IllegalArgumentException}</tt>.
     *
     * @param name
     * The property name.
     *
     * @param value
     * The property value.
     */
    protected void setUndefined(String name, Object value) {
        throw new IllegalArgumentException(String.format("Property \"%s\" is not defined.", name));
    }

    /**
     * Sets a group of property values.
     *
     * @param values
     * A map containing the property values to set.
     */
    public void setAll(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }
}
