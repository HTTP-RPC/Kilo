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
 * Provides support for constructing conditional expressions.
 */
public class Conditionals {
    private Conditionals() {
    }

    /**
     * Creates an "and" conditional.
     *
     * @param predicates
     * The conditional's predicates.
     *
     * @return
     * The conditional text.
     */
    public static String and(String... predicates) {
        return conditional("and", predicates);
    }

    /**
     * Creates an "or" conditional.
     *
     * @param predicates
     * The conditional's predicates.
     *
     * @return
     * The conditional text.
     */
    public static String or(String... predicates) {
        return conditional("or", predicates);
    }

    private static String conditional(String operator, String... predicates) {
        if (predicates == null || predicates.length == 0) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(64);

        stringBuilder.append(operator);
        stringBuilder.append(" ");

        if (predicates.length > 1) {
            stringBuilder.append("(");
        }

        for (var i = 0; i < predicates.length; i++) {
            if (i > 0) {
                stringBuilder.append(" ");
            }

            stringBuilder.append(predicates[i]);
        }

        if (predicates.length > 1) {
            stringBuilder.append(")");
        }

        return stringBuilder.toString();
    }

    /**
     * Creates an "and" conditional group.
     *
     * @param predicates
     * The group's predicates.
     *
     * @return
     * The conditional text.
     */
    public static String allOf(String... predicates) {
        if (predicates == null || predicates.length == 0) {
            throw new IllegalArgumentException();
        }

        return conditionalGroup("and", predicates);
    }

    /**
     * Creates an "or" conditional group.
     *
     * @param predicates
     * The group's predicates.
     *
     * @return
     * The conditional text.
     */
    public static String anyOf(String... predicates) {
        if (predicates == null || predicates.length == 0) {
            throw new IllegalArgumentException();
        }

        return conditionalGroup("or", predicates);
    }

    private static String conditionalGroup(String operator, String... predicates) {
        if (predicates == null || predicates.length == 0) {
            throw new IllegalArgumentException();
        }

        var stringBuilder = new StringBuilder(128);

        stringBuilder.append("(");

        for (var i = 0; i < predicates.length; i++) {
            if (i > 0) {
                stringBuilder.append(" ");
                stringBuilder.append(operator);
                stringBuilder.append(" ");
            }

            stringBuilder.append(predicates[i]);
        }

        stringBuilder.append(")");

        return stringBuilder.toString();
    }

    /**
     * @deprecated
     * Use {@link #in(QueryBuilder)} or {@link #exists(QueryBuilder)} instead.
     */
    @Deprecated
    public static String equalTo(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("= (%s)", queryBuilder);
    }

    /**
     * @deprecated
     * Use {@link #notIn(QueryBuilder)} or {@link #notExists(QueryBuilder)}
     * instead.
     */
    @Deprecated
    public static String notEqualTo(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("!= (%s)", queryBuilder);
    }

    /**
     * Creates an "in" conditional.
     *
     * @param queryBuilder
     * The conditional's subquery.
     *
     * @return
     * The conditional text.
     */
    public static String in(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("in (%s)", queryBuilder);
    }

    /**
     * Creates a "not in" conditional.
     *
     * @param queryBuilder
     * The conditional's subquery.
     *
     * @return
     * The conditional text.
     */
    public static String notIn(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("not in (%s)", queryBuilder);
    }

    /**
     * Creates an "exists" conditional.
     *
     * @param queryBuilder
     * The conditional's subquery.
     *
     * @return
     * The conditional text.
     */
    public static String exists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("exists (%s)", queryBuilder);
    }

    /**
     * Creates a "not exists" conditional.
     *
     * @param queryBuilder
     * The conditional's subquery.
     *
     * @return
     * The conditional text.
     */
    public static String notExists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("not exists (%s)", queryBuilder);
    }
}
