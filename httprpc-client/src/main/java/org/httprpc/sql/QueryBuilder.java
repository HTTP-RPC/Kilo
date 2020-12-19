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

package org.httprpc.sql;

/**
 * Class for programmatically constructing a SQL query.
 */
public class QueryBuilder {
    private StringBuilder sqlBuilder = new StringBuilder();

    private QueryBuilder(String operation) {
        sqlBuilder.append(operation);
    }

    /**
     * Creates a "select" query.
     *
     * @param columns
     * The column names.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder select(String... columns) {
        if (columns == null) {
            throw new IllegalArgumentException();
        }

        return new QueryBuilder("select " + String.join(", ", columns));
    }

    /**
     * Creates an "insert into" query.
     *
     * @param table
     * The table name.
     *
     * @param columns
     * The column names.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder insertInto(String table, String... columns) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        if (columns == null) {
            throw new IllegalArgumentException();
        }

        return new QueryBuilder("insert into " + table + " (" + String.join(", ", columns) + ")");
    }

    /**
     * Creates an "update" query.
     *
     * @param table
     * The table name.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder update(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        return new QueryBuilder("update " + table);
    }

    /**
     * Creates a "delete from" query.
     *
     * @param table
     * The table name.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder deleteFrom(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        return new QueryBuilder("delete from " + table);
    }

    /**
     * Appends a "from" clause to a query.
     *
     * @param tables
     * The table names.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder from(String... tables) {
        if (tables == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" from ");
        sqlBuilder.append(String.join(", ", tables));

        return this;
    }

    /**
     * Appends a "join" clause to a query.
     *
     * @param table
     * The table name.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder join(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" join ");
        sqlBuilder.append(table);

        return this;
    }

    /**
     * Appends a "left join" clause to a query.
     *
     * @param table
     * The table name.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder leftJoin(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" left join ");
        sqlBuilder.append(table);

        return this;
    }

    /**
     * Appends a "right join" clause to a query.
     *
     * @param table
     * The table name.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder rightJoin(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" right join ");
        sqlBuilder.append(table);

        return this;
    }

    /**
     * Appends an "on" clause to a query.
     *
     * @param predicate
     * The predicate.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder on(String predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" on ");
        sqlBuilder.append(predicate);

        return this;
    }

    /**
     * Appends a "where" clause to a query.
     *
     * @param predicate
     * The predicate.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder where(String predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" where ");
        sqlBuilder.append(predicate);

        return this;
    }

    /**
     * Appends an "order by" clause to a query.
     *
     * @param columns
     * The column names.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder orderBy(String... columns) {
        if (columns == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" order by ");
        sqlBuilder.append(String.join(", ", columns));

        return this;
    }

    /**
     * Appends a "for update" clause to a query.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder forUpdate() {
        sqlBuilder.append(" for update");

        return this;
    }

    /**
     * Appends a "values" clause to a query.
     *
     * @param values
     * The column values.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder values(Object... values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" values (");

        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(encode(values[i]));
        }

        sqlBuilder.append(")");

        return this;
    }

    /**
     * Appends a "set" command to a query.
     *
     * @param column
     * The column name.
     *
     * @param value
     * The column value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder set(String column, Object value) {
        sqlBuilder.append(" set ");
        sqlBuilder.append(column);
        sqlBuilder.append(" = ");
        sqlBuilder.append(encode(value));

        return this;
    }

    @Override
    public String toString() {
        return sqlBuilder.toString();
    }

    private static String encode(Object value) {
        if (value instanceof String) {
            String string = (String)value;

            if (string.startsWith(":") || string.equals("?")) {
                return string;
            } else {
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("'");
                stringBuilder.append(string.replace("'", "''"));
                stringBuilder.append("'");

                return stringBuilder.toString();
            }

        } else {
            return String.valueOf(value);
        }
    }
}
