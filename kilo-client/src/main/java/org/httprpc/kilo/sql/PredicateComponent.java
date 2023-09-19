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
 * Represents a predicate.
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
     * The key or keys of the value or values, respectively, representing the
     * right-hand side of the comparison.
     */
    public PredicateComponent(SchemaElement schemaElement, String operator, String... keys) {
        if (operator == null || schemaElement == null || keys == null || keys.length == 0) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement.label());
        stringBuilder.append(" ");

        if (keys.length == 1) {
            stringBuilder.append(operator);
            stringBuilder.append(" :");
            stringBuilder.append(keys[0]);
        } else {
            stringBuilder.append(operator);
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

        text = stringBuilder.toString();
    }

    private PredicateComponent(String operator, PredicateComponent... predicateComponents) {
        if (operator == null || predicateComponents == null || predicateComponents.length == 0) {
            throw new IllegalArgumentException();
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
