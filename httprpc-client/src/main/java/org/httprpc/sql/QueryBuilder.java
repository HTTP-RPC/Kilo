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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for programmatically constructing a SQL query.
 */
public class QueryBuilder {
    private StringBuilder sqlBuilder;

    private QueryBuilder(StringBuilder sqlBuilder) {
        this.sqlBuilder = sqlBuilder;
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

        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("select ");
        sqlBuilder.append(String.join(", ", columns));

        return new QueryBuilder(sqlBuilder);
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
     * Appends a "limit" clause to a query.
     *
     * @param count
     * The limit count.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder limit(int count) {
        if (count < 0) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" limit ");
        sqlBuilder.append(count);

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
     * Creates an "insert into" query.
     *
     * @param table
     * The table name.
     *
     * @param values
     * The values to insert.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder insertInto(String table, Map<String, ?> values) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        if (values == null) {
            throw new IllegalArgumentException();
        }

        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("insert into ");
        sqlBuilder.append(table);
        sqlBuilder.append(" (");

        List<String> columns = new ArrayList<>(values.keySet());

        int n = columns.size();

        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(columns.get(i));
        }

        sqlBuilder.append(") values (");

        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            Object value = values.get(columns.get(i));

            if (value instanceof QueryBuilder) {
                sqlBuilder.append("(");
                sqlBuilder.append(value.toString());
                sqlBuilder.append(")");
            } else {
                sqlBuilder.append(encode(value));
            }
        }

        sqlBuilder.append(")");

        return new QueryBuilder(sqlBuilder);
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

        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("update ");
        sqlBuilder.append(table);

        return new QueryBuilder(sqlBuilder);
    }

    /**
     * Appends a "set" command to a query.
     *
     * @param values
     * The values to update.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder set(Map<String, ?> values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" set ");

        int i = 0;

        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(entry.getKey());
            sqlBuilder.append(" = ");

            Object value = entry.getValue();

            if (value instanceof QueryBuilder) {
                sqlBuilder.append("(");
                sqlBuilder.append(value.toString());
                sqlBuilder.append(")");
            } else {
                sqlBuilder.append(encode(value));
            }

            i++;
        }

        return this;
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

        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("delete from ");
        sqlBuilder.append(table);

        return new QueryBuilder(sqlBuilder);
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
