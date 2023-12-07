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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.httprpc.kilo.util.Collections.immutableListOf;

/**
 * Represents a predicate component.
 */
public class PredicateComponent {
    private String text;
    private List<String> parameters;

    private static final String AND = "and";
    private static final String OR = "or";

    private PredicateComponent(String text, List<String> parameters) {
        this.text = text;
        this.parameters = parameters;
    }

    static PredicateComponent column(SchemaElement schemaElement1, String operator, SchemaElement schemaElement2) {
        if (schemaElement1 == null || operator == null || schemaElement2 == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement1.getQualifiedName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);
        stringBuilder.append(" ");
        stringBuilder.append(schemaElement2.getQualifiedName());

        return new PredicateComponent(stringBuilder.toString(), immutableListOf());
    }

    static PredicateComponent variable(SchemaElement schemaElement, String operator, String... parameters) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement.getQualifiedName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);

        if (parameters.length > 0) {
            if (parameters.length == 1) {
                stringBuilder.append(" ?");
            } else {
                stringBuilder.append(" (");

                for (var i = 0; i < parameters.length; i++) {
                    if (i > 0) {
                        stringBuilder.append(", ");
                    }

                    stringBuilder.append("?");
                }

                stringBuilder.append(")");
            }
        }

        return new PredicateComponent(stringBuilder.toString(), Arrays.asList(parameters));
    }

    static PredicateComponent literal(SchemaElement schemaElement, String operator, Number... values) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

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

        return new PredicateComponent(stringBuilder.toString(), immutableListOf());
    }

    static PredicateComponent literal(SchemaElement schemaElement, String operator, boolean value) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement.getQualifiedName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);
        stringBuilder.append(" ");
        stringBuilder.append(value);

        return new PredicateComponent(stringBuilder.toString(), immutableListOf());
    }

    static PredicateComponent unary(SchemaElement schemaElement, String operator) {
        if (schemaElement == null || operator == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(32);

        stringBuilder.append(schemaElement.getQualifiedName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);

        return new PredicateComponent(stringBuilder.toString(), immutableListOf());
    }

    static PredicateComponent subquery(SchemaElement schemaElement, String operator, QueryBuilder queryBuilder) {
        if (schemaElement == null || operator == null || queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(128);

        stringBuilder.append(schemaElement.getQualifiedName());
        stringBuilder.append(" ");
        stringBuilder.append(operator);
        stringBuilder.append(" (");
        stringBuilder.append(queryBuilder);
        stringBuilder.append(")");

        return new PredicateComponent(stringBuilder.toString(), queryBuilder.getParameters());
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

        var parameters = new LinkedList<String>();

        if (predicateComponents.length == 1) {
            stringBuilder.append(operator);
            stringBuilder.append(" ");

            var predicateComponent = predicateComponents[0];

            stringBuilder.append(predicateComponent.text);

            parameters.addAll(predicateComponent.parameters);
        } else {
            stringBuilder.append("(");

            for (var i = 0; i < predicateComponents.length; i++) {
                if (i > 0) {
                    stringBuilder.append(" ");
                    stringBuilder.append(operator);
                    stringBuilder.append(" ");
                }

                var predicateComponent = predicateComponents[i];

                stringBuilder.append(predicateComponent.text);

                parameters.addAll(predicateComponent.parameters);
            }

            stringBuilder.append(")");
        }

        return new PredicateComponent(stringBuilder.toString(), parameters);
    }

    /**
     * Creates an "exists" predicate component.
     *
     * @param queryBuilder
     * The "select" query.
     *
     * @return
     * The predicate component.
     */
    public static PredicateComponent exists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return new PredicateComponent(String.format("exists (%s)", queryBuilder), queryBuilder.getParameters());
    }

    /**
     * Creates a "not exists" predicate component.
     *
     * @param queryBuilder
     * The "select" query.
     *
     * @return
     * The predicate component.
     */
    public static PredicateComponent notExists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return new PredicateComponent(String.format("not exists (%s)", queryBuilder), queryBuilder.getParameters());
    }

    /**
     * Adapts a predicate component for use as a schema element.
     *
     * @param alias
     * The schema element's alias.
     *
     * @return
     * A schema element with the provided alias.
     */
    public SchemaElement as(String alias) {
        if (alias == null) {
            throw new IllegalArgumentException();
        }

        return new SchemaElementAdapter(null, text, alias);
    }

    /**
     * Returns the predicate component's parameters.
     *
     * @return
     * The predicate component's parameters.
     */
    public List<String> getParameters() {
        return Collections.unmodifiableList(parameters);
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
