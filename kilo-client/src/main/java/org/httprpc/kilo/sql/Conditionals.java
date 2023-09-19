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
     * @deprecated
     * Use {{@link PredicateComponent#and(PredicateComponent)} instead.
     */
    @Deprecated
    public static String and(String predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException();
        }

        return String.format("and %s", predicate);
    }

    /**
     * @deprecated
     * Use {{@link PredicateComponent#or(PredicateComponent)} instead.
     */
    @Deprecated
    public static String or(String predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException();
        }

        return String.format("or %s", predicate);
    }

    /**
     * @deprecated
     * Use {{@link PredicateComponent#allOf(PredicateComponent...)} instead.
     */
    @Deprecated
    public static String allOf(String... predicates) {
        if (predicates == null || predicates.length == 0) {
            throw new IllegalArgumentException();
        }

        return group("and", predicates);
    }

    /**
     * @deprecated
     * Use {{@link PredicateComponent#anyOf(PredicateComponent...)} instead.
     */
    @Deprecated
    public static String anyOf(String... predicates) {
        if (predicates == null || predicates.length == 0) {
            throw new IllegalArgumentException();
        }

        return group("or", predicates);
    }

    private static String group(String operator, String... predicates) {
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
     * Use {@link QueryBuilder#whereExists(QueryBuilder)} instead.
     */
    @Deprecated
    public static String exists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("exists (%s)", reconstruct(queryBuilder));
    }

    /**
     * @deprecated
     * Use {@link QueryBuilder#whereNotExists(QueryBuilder)} instead.
     */
    @Deprecated
    public static String notExists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        return String.format("not exists (%s)", reconstruct(queryBuilder));
    }

    private static String reconstruct(QueryBuilder queryBuilder) {
        var sql = queryBuilder.toString();

        var stringBuilder = new StringBuilder(sql);

        var parameterIterator = queryBuilder.getParameters().iterator();

        for (int i = 0, n = sql.length(); i < n; i++) {
            var c = sql.charAt(i);

            if (c == '?') {
                var parameter = parameterIterator.next();

                if (parameter == null) {
                    stringBuilder.append(c);
                } else {
                    stringBuilder.append(':');
                    stringBuilder.append(parameter);
                }
            } else {
                stringBuilder.append(c);
            }
        }

        return stringBuilder.toString();
    }
}
