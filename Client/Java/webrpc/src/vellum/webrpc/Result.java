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

package vellum.webrpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for typed results.
 */
public abstract class Result {
    private static final String SET_PREFIX = "set";

    /**
     * Constructs a result and initializes its properties.
     *
     * @param properties
     * The initial property values.
     */
    public Result(Map<String, Object> properties) {
        // Populate mutator list
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

        // Iterate over contents of map and invoke setters
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Method method = mutators.get(entry.getKey());

            if (method != null) {
                Object value = entry.getValue();

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
            }
        }
    }
}
