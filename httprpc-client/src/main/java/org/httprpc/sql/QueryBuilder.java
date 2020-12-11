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
 * Class for dynamically constructing a SQL query.
 */
public class QueryBuilder {
    private StringBuilder sqlBuilder = new StringBuilder();

    private QueryBuilder(String operation) {
        sqlBuilder.append(operation);
    }

    /**
     * TODO
     * @param columns
     * @return
     */
    public static QueryBuilder select(String... columns) {
        if (columns == null) {
            throw new IllegalArgumentException();
        }

        return new QueryBuilder("select " + String.join(", ", columns));
    }

    /**
     * TODO
     * @param table
     * @param columns
     * @return
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
     * TODO
     * @param table
     * @return
     */
    public static QueryBuilder update(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        return new QueryBuilder("update " + table);
    }

    /**
     * TODO
     * @param table
     * @return
     */
    public static QueryBuilder deleteFrom(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        return new QueryBuilder("delete from " + table);
    }

    /**
     * TODO
     * @param tables
     * @return
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
     * TODO
     * @param table
     * @return
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
     * TODO
     * @param predicate
     * @return
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
     * TODO
     * @param predicate
     * @return
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
     * TODO
     * @param columns
     * @return
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
     * TODO
     * @param values
     * @return
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

            sqlBuilder.append(values[i]);
        }

        sqlBuilder.append(")");

        return this;
    }

    /**
     * TODO
     * @param column
     * @param value
     * @return
     */
    public QueryBuilder set(String column, Object value) {
        sqlBuilder.append(" set ");
        sqlBuilder.append(column);
        sqlBuilder.append(" = ");
        sqlBuilder.append(value);

        return this;
    }

    @Override
    public String toString() {
        return sqlBuilder.toString();
    }
}
