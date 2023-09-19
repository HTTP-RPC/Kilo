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

package org.httprpc.kilo.sql;

/**
 * Represents a schema element.
 */
public interface SchemaElement {
    /**
     * Returns the schema element's label. By default, this is the value of the
     * {@link Column} annotation associated with the element.
     *
     * @return
     * The schema element's label.
     */
    default String label() {
        if (!(this instanceof Enum<?> schemaElement)) {
            throw new UnsupportedOperationException("Schema element is not an enum constant.");
        }

        var name = schemaElement.name();

        var fields = getClass().getDeclaredFields();

        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];

            if (!field.isEnumConstant()) {
                continue;
            }

            if (name.equals(field.getName())) {
                var column = field.getAnnotation(Column.class);

                if (column == null) {
                    throw new UnsupportedOperationException("Missing column annotation.");
                }

                return column.value();
            }
        }

        return null;
    }

    /**
     * Returns the schema element's alias, as specified via the
     * {@link #as(String)} method.
     *
     * @return
     * The schema element's alias, or {@code null} if an alias has not been
     * defined.
     */
    default String alias() {
        return null;
    }

    /**
     * Associates an alias with the schema element.
     *
     * @param alias
     * The schema element's alias.
     *
     * @return
     * A new schema element with the current label and given alias.
     */
    default SchemaElement as(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException();
        }

        var label = label();

        return new SchemaElement() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public String alias() {
                return alias;
            }

            @Override
            public SchemaElement as(String alias) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Creates an "equal to" predicate component.
     *
     * @param key
     * The key of the value against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent eq(String key) {
        return new PredicateComponent(this, "=", key);
    }

    /**
     * Creates a "not equal to" predicate component.
     *
     * @param key
     * The key of the value against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ne(String key) {
        return new PredicateComponent(this, "!=", key);
    }

    /**
     * Creates a "greater than" predicate component.
     *
     * @param key
     * The key of the value against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent gt(String key) {
        return new PredicateComponent(this, ">", key);
    }

    /**
     * Creates a "greater than or equal to" predicate component.
     *
     * @param key
     * The key of the value against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ge(String key) {
        return new PredicateComponent(this, ">=", key);
    }

    /**
     * Creates a "less than" predicate component.
     *
     * @param key
     * The key of the value against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent lt(String key) {
        return new PredicateComponent(this, "<", key);
    }

    /**
     * Creates a "less than or equal to" predicate component.
     *
     * @param key
     * The key of the value against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent le(String key) {
        return new PredicateComponent(this, "<=", key);
    }

    /**
     * Creates a "like" predicate component.
     *
     * @param key
     * The key of the value against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent like(String key) {
        return new PredicateComponent(this, "like", key);
    }

    /**
     * Creates an "in" predicate component.
     *
     * @param keys
     * The keys of the values against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent in(String... keys) {
        return new PredicateComponent(this, "in", keys);
    }

    /**
     * Creates a "not in" predicate component.
     *
     * @param keys
     * The keys of the values against which the conditional will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent notIn(String... keys) {
        return new PredicateComponent(this, "not in", keys);
    }
}
