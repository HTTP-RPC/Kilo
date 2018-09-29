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

import java.beans.FeatureDescriptor;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 * Class that presents a custom view of another cursor.
 */
public class CursorAdapter implements Iterable<Map<String, Object>> {
    private Iterable<Map<String, ?>> cursor;
    private LinkedHashMap<String, ValueExpression> mappings;

    private Map<String, ?> values = null;
    private LinkedHashMap<String, Object> row = new LinkedHashMap<>();

    private Iterator<Map<String, Object>> iterator = new Iterator<Map<String, Object>>() {
        private Iterator<Map<String, ?>> iterator = cursor.iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Map<String, Object> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            row.clear();

            for (Map.Entry<String, ValueExpression> mapping : mappings.entrySet()) {
                row.put(mapping.getKey(), mapping.getValue().getValue(context));
            }

            return row;
        }
    };

    private ELResolver resolver = new ELResolver() {
        @Override
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
            return Collections.emptyIterator();
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return Object.class;
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property) {
            return Object.class;
        }

        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return true;
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property) {
            Object value = values.get(property);

            context.setPropertyResolved(true);

            return value;
        }

        @Override
        public void setValue(ELContext context, Object base, Object property, Object value) {
            throw new UnsupportedOperationException();
        }
    };

    private ELContext context = new ELContext() {
        @Override
        public ELResolver getELResolver() {
            return resolver;
        }

        @Override
        public VariableMapper getVariableMapper() {
            return null;
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return null;
        }
    };

    /**
     * Constructs a cursor adapter.
     *
     * @param cursor
     * The source cursor.
     *
     * @param mappings
     * A map of row keys to value expressions.
     */
    public CursorAdapter(Iterable<Map<String, ?>> cursor, Map<String, String> mappings) {
        if (cursor == null) {
            throw new IllegalArgumentException();
        }

        if (mappings == null) {
            throw new IllegalArgumentException();
        }

        this.cursor = cursor;

        this.mappings = new LinkedHashMap<>();

        ExpressionFactory expressionFactory = ExpressionFactory.newInstance();

        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            ValueExpression expression = expressionFactory.createValueExpression(context, String.format("${%s}", value), Object.class);

            this.mappings.put(key, expression);
        }
    }

    @Override
    public Iterator<Map<String, Object>> iterator() {
        return iterator;
    }
}
