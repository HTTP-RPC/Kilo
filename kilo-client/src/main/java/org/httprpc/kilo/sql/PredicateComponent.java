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
 * Represents a predicate component.
 */
public class PredicateComponent {
    private String text;

    private static final String AND = "and";
    private static final String OR = "or";

    /**
     * Constructs a new comparison predicate component.
     *
     * @param schemaElement
     * The schema element representing the left-hand side of the comparison.
     *
     * @param operator
     * The comparison operator.
     *
     * @param keys
     * The keys of the values representing the right-hand side of the
     * comparison.
     */
    public PredicateComponent(SchemaElement schemaElement, String operator, String... keys) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement.getColumnName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);

        if (keys.length > 0) {
            if (keys.length == 1) {
                stringBuilder.append(" :");
                stringBuilder.append(keys[0]);
            } else {
                stringBuilder.append(" (");

                for (var i = 0; i < keys.length; i++) {
                    if (i > 0) {
                        stringBuilder.append(", ");
                    }

                    stringBuilder.append(":");
                    stringBuilder.append(keys[i]);
                }

                stringBuilder.append(")");
            }
        }

        text = stringBuilder.toString();
    }

    /**
     * Constructs a new comparison predicate component.
     *
     * @param schemaElement1
     * The schema element representing the left-hand side of the comparison.
     *
     * @param operator
     * The comparison operator.
     *
     * @param schemaElement2
     * The schema element representing the right-hand side of the comparison.
     */
    public PredicateComponent(SchemaElement schemaElement1, String operator, SchemaElement schemaElement2) {
        if (schemaElement1 == null || operator == null || schemaElement2 == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(SchemaElement.getTableName(schemaElement1.getClass()));
        stringBuilder.append(".");
        stringBuilder.append(schemaElement1.getColumnName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);
        stringBuilder.append(" ");
        stringBuilder.append(SchemaElement.getTableName(schemaElement2.getClass()));
        stringBuilder.append(".");
        stringBuilder.append(schemaElement2.getColumnName());

        text = stringBuilder.toString();
    }

    private PredicateComponent(String operator, PredicateComponent... predicateComponents) {
        if (predicateComponents.length == 0) {
            throw new UnsupportedOperationException();
        }

        var stringBuilder = new StringBuilder(128);

        if (predicateComponents.length == 1) {
            stringBuilder.append(operator);
            stringBuilder.append(" ");
            stringBuilder.append(predicateComponents[0]);
        } else {
            stringBuilder.append("(");

            for (var i = 0; i < predicateComponents.length; i++) {
                if (i > 0) {
                    stringBuilder.append(" ");
                    stringBuilder.append(operator);
                    stringBuilder.append(" ");
                }

                stringBuilder.append(predicateComponents[i]);
            }

            stringBuilder.append(")");
        }

        text = stringBuilder.toString();
    }

    /**
     * Creates an "and" predicate component.
     *
     * @param predicateComponent
     * The predicate component representing the right-hand side of the
     * operation.
     *
     * @return
     * The predicate component.
     */
    public static PredicateComponent and(PredicateComponent predicateComponent) {
        return new PredicateComponent(AND, predicateComponent);
    }

    /**
     * Creates an "or" predicate component.
     *
     * @param predicateComponent
     * The predicate component representing the right-hand side of the
     * operation.
     *
     * @return
     * The predicate component.
     */
    public static PredicateComponent or(PredicateComponent predicateComponent) {
        return new PredicateComponent(OR, predicateComponent);
    }

    /**
     * Creates an "and" group predicate component.
     *
     * @param predicateComponents
     * The group's predicate components.
     *
     * @return
     * The predicate component.
     */
    public static PredicateComponent allOf(PredicateComponent... predicateComponents) {
        return new PredicateComponent(AND, predicateComponents);
    }

    /**
     * Creates an "or" group predicate component.
     *
     * @param predicateComponents
     * The group's predicate components.
     *
     * @return
     * The predicate component.
     */
    public static PredicateComponent anyOf(PredicateComponent... predicateComponents) {
        return new PredicateComponent(OR, predicateComponents);
    }

    /**
     * Returns the string representation of the predicate component.
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return text;
    }
}
