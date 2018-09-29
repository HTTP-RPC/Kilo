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

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * Class that presents a custom view of another cursor.
 */
public class CursorAdapter implements Iterable<Map<String, Object>> {
    // Cursor adapter iterator
    private static class CursorAdapterIterator implements Iterator<Map<String, Object>> {
        private Iterator<? extends Map<String, ?>> iterator;
        private Map<String, String> mappings;

        private ScriptEngine engine;

        private HashMap<String, Object> row = new HashMap<>();

        public CursorAdapterIterator(Iterator<? extends Map<String, ?>> iterator, Map<String, String> mappings) {
            this.iterator = iterator;
            this.mappings = mappings;

            ScriptEngineManager engineManager = new ScriptEngineManager();

            engine = engineManager.getEngineByName("nashorn");
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Map<String, ?> values = iterator.next();

            engine.getContext().setBindings(new SimpleBindings((Map<String, Object>)values) {
                @Override
                public Object put(String name, Object value) {
                    return null;
                }
            }, ScriptContext.ENGINE_SCOPE);

            row.clear();

            for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                String key = mapping.getKey();

                Object value;
                try {
                    value = engine.eval(mapping.getValue());
                } catch (ScriptException exception) {
                    throw new RuntimeException(exception);
                }

                row.put(key, value);
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
