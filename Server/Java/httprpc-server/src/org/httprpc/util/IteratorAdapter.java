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

package org.httprpc.util;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that presents the contents of an iterator as an iterable list of
 * values.
 */
public class IteratorAdapter extends AbstractList<Object> implements AutoCloseable {
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
        public Set<Entry<Object, Object>> entrySet() {
            return entrySet;
        }
    }

    private Iterator<?> iterator;

    /**
     * Constructs a new iterator adapter.
     *
     * @param iterator
     * The source iterator. If the iterator's type implements
     * {@link AutoCloseable}, it will be automatically closed when the adapter
     * is closed.
     */
    public IteratorAdapter(Iterator<?> iterator) {
        if (iterator == null) {
            throw new IllegalArgumentException();
        }

        this.iterator = iterator;
    }

    @Override
    public Map<String, Object> get(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
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

    @Override
    public void close() throws Exception {
        if (iterator instanceof AutoCloseable) {
            ((AutoCloseable)iterator).close();
        }
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    /**
     * Adapts a value. If the value is <tt>null</tt> or an instance of one of
     * the following types, it is returned as-is:
     * <ul>
     * <li>{@link String}</li>
     * <li>{@link Number}</li>
     * <li>{@link Boolean}</li>
     * </ul>
     * If the value is a {@link Date}, it is converted to its numeric
     * representation via {@link Date#getTime()}. If the value is a
     * {@link List}, it is wrapped in an adapter that will adapt the list's
     * elements. If the value is a {@link Map}, it is wrapped in an adapter
     * that will adapt the map's values. Otherwise, it is converted to a
     * {@link String}.
     *
     * @param <T> The expected type of the adapted value.
     *
     * @param value
     * The value to adapt.
     *
     * @return
     * The adapted value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T adapt(Object value) {
        if (value != null && !(value instanceof String || value instanceof Number || value instanceof Boolean)) {
            if (value instanceof Date) {
                value = ((Date)value).getTime();
            } else if (value instanceof List<?>) {
                value = new ListAdapter((List<Object>)value);
            } else if (value instanceof Map<?, ?>) {
                value = new MapAdapter((Map<Object, Object>)value);
            } else {
                value = value.toString();
            }
        }

        return (T)value;
    }
}
