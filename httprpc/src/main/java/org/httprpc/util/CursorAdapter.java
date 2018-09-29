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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Class that presents a custom view of another cursor.
 */
public class CursorAdapter implements Iterable<Map<String, Object>> {
    // Cursor adapter iterator
    private static class CursorAdapterIterator implements Iterator<Map<String, Object>> {
        private Iterator<? extends Map<String, ?>> iterator;
        private Map<String, String> mappings;

        private HashMap<String, Object> row = new HashMap<>();

        public CursorAdapterIterator(Iterator<? extends Map<String, ?>> iterator, Map<String, String> mappings) {
            this.iterator = iterator;
            this.mappings = mappings;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Map<String, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Map<String, ?> values = iterator.next();

            row.clear();

            for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                // TODO Evaluate the mapping expression against the values
                row.put(mapping.getKey(), values.get(mapping.getKey()));
            }

            return row;
        }
    }

    private Iterable<? extends Map<String, ?>> cursor;
    private Map<String, String> mappings;

    /**
     * Constructs a cursor adapter.
     *
     * @param cursor
     * The cursor to adapt.
     *
     * @param mappings
     * A map of row keys to value expressions.
     */
    public CursorAdapter(Iterable<? extends Map<String, ?>> cursor, Map<String, String> mappings) {
        if (cursor == null) {
            throw new IllegalArgumentException();
        }

        if (mappings == null) {
            throw new IllegalArgumentException();
        }

        this.cursor = cursor;
        this.mappings = mappings;
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return new CursorAdapterIterator(cursor.iterator(), mappings);
    }
}
