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
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Abstract base class for custom result types.
 */
public abstract class Result extends AbstractMap<String, Object> {
    private HashMap<String, Method> accessors;

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
     * Constructs a new result.
     */
    public Result() {
        accessors = new HashMap<>();

        Method[] methods = getClass().getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            if (Result.class.isAssignableFrom(method.getDeclaringClass())) {
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

    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        Method method = accessors.get(key);

        Object value;
        try {
            value = method.invoke(this);
        } catch (InvocationTargetException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        return value;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return entrySet;
    }
}
