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
     * Sort order options.
     */
    enum SortOrder {
        /**
         * Ascending sort.
         */
        ASC,

        /**
         * Descending sort.
         */
        DESC
    }

    /**
     * The "equal to" operator.
     */
    String EQ = "=";

    /**
     * The "not equal to" operator.
     */
    String NE = "!=";

    /**
     * The "greater than" operator.
     */
    String GT = ">";

    /**
     * The "greater than or equal to" operator.
     */
    String GE = ">=";

    /**
     * The "less than" operator.
     */
    String LT = "<";

    /**
     * The "less than or equal to" operator.
     */
    String LE = "<=";

    /**
     * The "in" operator.
     */
    String IN = "in";

    /**
     * The "not in" operator.
     */
    String NOT_IN = "not in";

    /**
     * The "plus" operator.
     */
    String PLUS = "+";

    /**
     * The "minus" operator.
     */
    String MINUS = "-";

    /**
     * The "multiplied by" operator.
     */
    String MULTIPLIED_BY = "*";

    /**
     * The "divided by" operator.
     */
    String DIVIDED_BY = "/";

    /**
     * Returns the name of the table associated with the schema type.
     *
     * @param schemaType
     * The schema type.
     *
     * @return
     * The schema type's table name.
     */
    static String getTableName(Class<? extends SchemaElement> schemaType) {
        if (schemaType == null) {
            throw new IllegalArgumentException();
        }

        var table = schemaType.getAnnotation(Table.class);

        if (table == null) {
            throw new UnsupportedOperationException("Missing table annotation.");
        }

        return table.value();
    }

    /**
     * Creates an "all" schema element.
     *
     * @param schemaType
     * The schema type.
     *
     * @return
     * A schema element representing all columns in the associated table.
     */
    static SchemaElement all(Class<? extends SchemaElement> schemaType) {
        return new SchemaElementAdapter(getTableName(schemaType), "*");
    }

    /**
     * Returns the name of the schema element.
     *
     * @return
     * The schema element's name.
     */
    default String name() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the name of the table associated with the schema element.
     *
     * @return
     * The schema element's table name.
     */
    default String getTableName() {
        return getTableName(getClass());
    }

    /**
     * Returns the name of the column associated with the schema element.
     *
     * @return
     * The schema element's column name.
     */
    default String getColumnName() {
        var name = name();

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
     * Returns the qualified name of the schema element.
     *
     * @return
     * The schema element's qualified name.
     */
    default String getQualifiedName() {
        var tableName = getTableName();

        if (tableName == null) {
            return getColumnName();
        } else {
            return String.format("%s.%s", tableName, getColumnName());
        }
    }

    /**
     * Returns the schema element's alias, as specified via the
     * {@link #as(String)} method.
     *
     * @return
     * The schema element's alias, or {@code null} if an alias has not been
     * defined.
     */
    default String getAlias() {
        return null;
    }

    /**
     * Returns the schema element's sort order, as specified via either
     * {@link #asc()} or {@link #desc()}.
     *
     * @return
     * The schema element's sort order, or {@code null} if a sort order has not
     * been defined.
     */
    default SortOrder getSortOrder() {
        return null;
    }

    /**
     * Indicates that the schema element is required.
     *
     * @return
     * {@code true} if the element is required; {@code false}, otherwise.
     */
    default boolean isRequired() {
        return true;
    }

    /**
     * Creates a "count" schema element.
     *
     * @return
     * A count schema element.
     */
    default SchemaElement count() {
        return new SchemaElementAdapter(null, String.format("count(%s)", getQualifiedName()));
    }

    /**
     * Creates an "average" schema element.
     *
     * @return
     * An average schema element.
     */
    default SchemaElement avg() {
        return new SchemaElementAdapter(null, String.format("avg(%s)", getQualifiedName()));
    }

    /**
     * Creates a "sum" schema element.
     *
     * @return
     * A sum schema element.
     */
    default SchemaElement sum() {
        return new SchemaElementAdapter(null, String.format("sum(%s)", getQualifiedName()));
    }

    /**
     * Creates a "minimum" schema element.
     *
     * @return
     * A minimum schema element.
     */
    default SchemaElement min() {
        return new SchemaElementAdapter(null, String.format("min(%s)", getQualifiedName()));
    }

    /**
     * Creates a "maximum" schema element.
     *
     * @return
     * A maximum schema element.
     */
    default SchemaElement max() {
        return new SchemaElementAdapter(null, String.format("max(%s)", getQualifiedName()));
    }

    /**
     * Associates an alias with the schema element.
     *
     * @param alias
     * The schema element's alias.
     *
     * @return
     * A schema element with the provided alias.
     */
    default SchemaElement as(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException();
        }

        return new SchemaElementAdapter(getTableName(), getColumnName(), alias);
    }

    /**
     * Creates an "equal to" predicate component.
     *
     * @param schemaElement
     * The schema element representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent eq(SchemaElement schemaElement) {
        return PredicateComponent.column(this, EQ, schemaElement);
    }

    /**
     * Creates an "equal to" predicate component.
     *
     * @param key
     * The key of the value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent eq(String key) {
        return PredicateComponent.variable(this, EQ, key);
    }

    /**
     * Creates an "equal to" predicate component.
     *
     * @param value
     * The value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent eq(Number value) {
        return PredicateComponent.literal(this, EQ, value);
    }

    /**
     * Creates an "equal to" predicate component.
     *
     * @param value
     * The value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent eq(boolean value) {
        return PredicateComponent.literal(this, EQ, value);
    }

    /**
     * Creates a "not equal to" predicate component.
     *
     * @param schemaElement
     * The schema element representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ne(SchemaElement schemaElement) {
        return PredicateComponent.column(this, NE, schemaElement);
    }

    /**
     * Creates a "not equal to" predicate component.
     *
     * @param key
     * The key of the value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ne(String key) {
        return PredicateComponent.variable(this, NE, key);
    }

    /**
     * Creates a "not equal to" predicate component.
     *
     * @param value
     * The value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ne(Number value) {
        return PredicateComponent.literal(this, NE, value);
    }

    /**
     * Creates a "not equal to" predicate component.
     *
     * @param value
     * The value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ne(boolean value) {
        return PredicateComponent.literal(this, NE, value);
    }

    /**
     * Creates a "greater than" predicate component.
     *
     * @param schemaElement
     * The schema element representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent gt(SchemaElement schemaElement) {
        return PredicateComponent.column(this, GT, schemaElement);
    }

    /**
     * Creates a "greater than" predicate component.
     *
     * @param key
     * The key of the value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent gt(String key) {
        return PredicateComponent.variable(this, GT, key);
    }

    /**
     * Creates a "greater than or equal to" predicate component.
     *
     * @param schemaElement
     * The schema element representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ge(SchemaElement schemaElement) {
        return PredicateComponent.column(this, GE, schemaElement);
    }

    /**
     * Creates a "greater than or equal to" predicate component.
     *
     * @param key
     * The key of the value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent ge(String key) {
        return PredicateComponent.variable(this, GE, key);
    }

    /**
     * Creates a "less than" predicate component.
     *
     * @param schemaElement
     * The schema element representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent lt(SchemaElement schemaElement) {
        return PredicateComponent.column(this, LT, schemaElement);
    }

    /**
     * Creates a "less than" predicate component.
     *
     * @param key
     * The key of the value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent lt(String key) {
        return PredicateComponent.variable(this, LT, key);
    }

    /**
     * Creates a "less than or equal to" predicate component.
     *
     * @param schemaElement
     * The schema element representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent le(SchemaElement schemaElement) {
        return PredicateComponent.column(this, LE, schemaElement);
    }

    /**
     * Creates a "less than or equal to" predicate component.
     *
     * @param key
     * The key of the value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent le(String key) {
        return PredicateComponent.variable(this, LE, key);
    }

    /**
     * Creates a "like" predicate component.
     *
     * @param key
     * The key of the value representing the right-hand side of the comparison.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent like(String key) {
        return PredicateComponent.variable(this, "like", key);
    }

    /**
     * Creates an "in" predicate component.
     *
     * @param keys
     * The keys of the values against which the expression will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent in(String... keys) {
        return PredicateComponent.variable(this, IN, keys);
    }

    /**
     * Creates an "in" predicate component.
     *
     * @param values
     * The values against which the expression will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent in(Number... values) {
        return PredicateComponent.literal(this, IN, values);
    }

    /**
     * Creates a "not in" predicate component.
     *
     * @param keys
     * The keys of the values against which the expression will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent notIn(String... keys) {
        return PredicateComponent.variable(this, NOT_IN, keys);
    }

    /**
     * Creates a "not in" predicate component.
     *
     * @param values
     * The values against which the expression will be evaluated.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent notIn(Number... values) {
        return PredicateComponent.literal(this, NOT_IN, values);
    }

    /**
     * Creates an "is null" predicate component.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent isNull() {
        return PredicateComponent.unary(this, "is null");
    }

    /**
     * Creates an "is not null" predicate component.
     *
     * @return
     * The predicate component.
     */
    default PredicateComponent isNotNull() {
        return PredicateComponent.unary(this, "is not null");
    }

    /**
     * Creates an addition schema element.
     *
     * @param schemaElement
     * The schema element to add.
     *
     * @return
     * An addition schema element.
     */
    default SchemaElement plus(SchemaElement schemaElement) {
        if (schemaElement == null) {
            throw new IllegalArgumentException();
        }

        return new SchemaElementAdapter(null, String.format("(%s %s %s)",
            getQualifiedName(),
            PLUS,
            schemaElement.getQualifiedName()));
    }

    /**
     * Creates a subtraction schema element.
     *
     * @param schemaElement
     * The schema element to subtract.
     *
     * @return
     * A subtraction schema element.
     */
    default SchemaElement minus(SchemaElement schemaElement) {
        if (schemaElement == null) {
            throw new IllegalArgumentException();
        }

        return new SchemaElementAdapter(null, String.format("(%s %s %s)",
            getQualifiedName(),
            MINUS,
            schemaElement.getQualifiedName()));
    }

    /**
     * Creates a multiplication schema element.
     *
     * @param schemaElement
     * The schema element to multiply by.
     *
     * @return
     * A multiplication schema element.
     */
    default SchemaElement multipliedBy(SchemaElement schemaElement) {
        if (schemaElement == null) {
            throw new IllegalArgumentException();
        }

        return new SchemaElementAdapter(null, String.format("(%s %s %s)",
            getQualifiedName(),
            MULTIPLIED_BY,
            schemaElement.getQualifiedName()));
    }

    /**
     * Creates a division schema element.
     *
     * @param schemaElement
     * The schema element to divide by.
     *
     * @return
     * A division schema element.
     */
    default SchemaElement dividedBy(SchemaElement schemaElement) {
        if (schemaElement == null) {
            throw new IllegalArgumentException();
        }

        return new SchemaElementAdapter(null, String.format("(%s %s %s)",
            getQualifiedName(),
            DIVIDED_BY,
            schemaElement.getQualifiedName()));
    }

    /**
     * Associates an ascending sort order with the schema element.
     *
     * @return
     * A schema element with an ascending sort order.
     */
    default SchemaElement asc() {
        return new SchemaElementAdapter(getTableName(), getColumnName(), SortOrder.ASC);
    }

    /**
     * Associates a descending sort order with the schema element.
     *
     * @return
     * A schema element with a descending sort order.
     */
    default SchemaElement desc() {
        return new SchemaElementAdapter(getTableName(), getColumnName(), SortOrder.DESC);
    }

    /**
     * Flags the schema element as optional.
     *
     * @return
     * An optional schema element.
     */
    default SchemaElement optional() {
        return new SchemaElementAdapter(getTableName(), getColumnName(), false);
    }
}
