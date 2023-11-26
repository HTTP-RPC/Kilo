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

    private PredicateComponent(String text) {
        this.text = text;
    }

    static PredicateComponent column(SchemaElement schemaElement1, String operator, SchemaElement schemaElement2) {
        if (schemaElement1 == null || operator == null || schemaElement2 == null) {
            throw new IllegalArgumentException();
        }

        return new PredicateComponent(String.format("%s %s %s", schemaElement1.getQualifiedName(), operator, schemaElement2.getQualifiedName()));
    }

    static PredicateComponent variable(SchemaElement schemaElement, String operator, String... keys) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement.getQualifiedName());
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

        return new PredicateComponent(stringBuilder.toString());
    }

    static PredicateComponent literal(SchemaElement schemaElement, String operator, Number... values) {
        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement.getQualifiedName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);

        if (values.length > 0) {
            if (values.length == 1) {
                stringBuilder.append(" ");
                stringBuilder.append(values[0]);
            } else {
                stringBuilder.append(" (");

                for (var i = 0; i < values.length; i++) {
                    if (i > 0) {
                        stringBuilder.append(", ");
                    }

                    stringBuilder.append(values[i]);
                }

                stringBuilder.append(")");
            }
        }

        return new PredicateComponent(stringBuilder.toString());
    }

    static PredicateComponent literal(SchemaElement schemaElement, String operator, boolean value) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

        return new PredicateComponent(String.format("%s %s %b", schemaElement.getQualifiedName(), operator, value));
    }

    static PredicateComponent unary(SchemaElement schemaElement, String operator) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

        return new PredicateComponent(String.format("%s %s", schemaElement.getQualifiedName(), operator));
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
        return group(AND, predicateComponent);
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
        return group(OR, predicateComponent);
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
        return group(AND, predicateComponents);
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
        return group(OR, predicateComponents);
    }

    private static PredicateComponent group(String operator, PredicateComponent... predicateComponents) {
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

        return new PredicateComponent(stringBuilder.toString());
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
