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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class for programmatically constructing and executing SQL queries.
 */
public class QueryBuilder {
    private StringBuilder sqlBuilder;

    private List<String> parameters = new LinkedList<>();

    private List<Map<String, Object>> results = null;
    private int updateCount = -1;
    private List<Object> generatedKeys = null;

    /**
     * Constructs a query builder from an existing SQL query.
     *
     * @param sql
     * The existing SQL query.
     */
    public QueryBuilder(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder = new StringBuilder();

        append(sql);
    }

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

        QueryBuilder queryBuilder = new QueryBuilder(sqlBuilder);

        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                queryBuilder.sqlBuilder.append(", ");
            }

            queryBuilder.append(columns[i]);
        }

        return queryBuilder;
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

        append(predicate);

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

        append(predicate);

        return this;
    }

    private void append(String predicate) {
        boolean quoted = false;

        int n = predicate.length();
        int i = 0;

        while (i < n) {
            char c = predicate.charAt(i++);

            if (c == ':' && !quoted) {
                StringBuilder parameterBuilder = new StringBuilder();

                while (i < n) {
                    c = predicate.charAt(i);

                    if (!Character.isJavaIdentifierPart(c)) {
                        break;
                    }

                    parameterBuilder.append(c);

                    i++;
                }

                if (parameterBuilder.length() == 0) {
                    throw new IllegalArgumentException("Missing parameter.");
                }

                parameters.add(parameterBuilder.toString());

                sqlBuilder.append("?");
            } else if (c == '?' && !quoted) {
                parameters.add(null);

                sqlBuilder.append(c);
            } else {
                if (c == '\'') {
                    quoted = !quoted;
                }

                sqlBuilder.append(c);
            }
        }
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
     * Appends a "union" clause to a query.
     *
     * @param queryBuilder
     * The query builder to append.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder union(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" union ");
        sqlBuilder.append(queryBuilder.getSQL());

        parameters.addAll(queryBuilder.parameters);

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
     *
     * @deprecated Use {@link #insertInto(String)} and {@link #values(Map)} instead.
     */
    @Deprecated
    public static QueryBuilder insertInto(String table, Map<String, ?> values) {
        return insertInto(table).values(values);
    }

    /**
     * Creates an "insert into" query.
     *
     * @param table
     * The table name.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder insertInto(String table) {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("insert into ");
        sqlBuilder.append(table);

        return new QueryBuilder(sqlBuilder);
    }

    /**
     * Appends column values to an "insert into" query.
     *
     * @param values
     * The values to insert.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder values(Map<String, ?> values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

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

            encode(values.get(columns.get(i)));
        }

        sqlBuilder.append(")");

        return this;
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
     * Appends column values to an "update" query.
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

            encode(entry.getValue());

            i++;
        }

        return this;
    }

    private void encode(Object value) {
        if (value instanceof String) {
            String string = (String)value;

            if (string.equals("?")) {
                parameters.add(null);

                sqlBuilder.append(string);
            } else if (string.startsWith(":")) {
                String parameter = string.substring(1);

                if (parameter.isEmpty()) {
                    throw new IllegalArgumentException("Missing parameter.");
                }

                parameters.add(parameter);

                sqlBuilder.append("?");
            } else {
                sqlBuilder.append("'");

                for (int i = 0, n = string.length(); i < n; i++) {
                    char c = string.charAt(i);

                    if (c == '\'') {
                        sqlBuilder.append(c);
                    }

                    sqlBuilder.append(c);
                }

                sqlBuilder.append("'");
            }
        } else if (value instanceof QueryBuilder) {
            QueryBuilder queryBuilder = (QueryBuilder)value;

            sqlBuilder.append("(");
            sqlBuilder.append(queryBuilder.getSQL());
            sqlBuilder.append(")");

            parameters.addAll(queryBuilder.parameters);
        } else {
            sqlBuilder.append(value);
        }
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

    /**
     * Executes a query.
     *
     * @param connection
     * The connection on which the query will be executed.
     *
     * @return
     * The {@link QueryBuilder} instance.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public QueryBuilder execute(Connection connection) throws SQLException {
        return execute(connection, Collections.emptyMap());
    }

    /**
     * Executes a query.
     *
     * @param connection
     * The connection on which the query will be executed.
     *
     * @param arguments
     * The query arguments.
     *
     * @return
     * The {@link QueryBuilder} instance.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public QueryBuilder execute(Connection connection, Map<String, ?> arguments) throws SQLException {
        if (connection == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        try (PreparedStatement statement = prepare(connection)) {
            apply(statement, arguments);

            if (statement.execute()) {
                try (ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.getResultSet())) {
                    results = resultSetAdapter.stream().collect(Collectors.toList());
                }
            } else {
                updateCount = statement.getUpdateCount();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ResultSetMetaData generatedKeysMetaData = generatedKeys.getMetaData();

                        int n = generatedKeysMetaData.getColumnCount();

                        this.generatedKeys = new ArrayList<>(n);

                        for (int i = 0; i < n; i++) {
                            this.generatedKeys.add(generatedKeys.getObject(i + 1));
                        }
                    } else {
                        this.generatedKeys = null;
                    }
                }
            }
        }

        return this;
    }

    /**
     * Returns the result of executing a query that is expected to return at
     * most a single row.
     *
     * @return
     * The query result, or <code>null</code> if the query either did not
     * produce a result set or did not return any rows.
     */
    public Map<String, Object> getResult() {
        if (results == null) {
            return null;
        }

        switch (results.size()) {
            case 0: {
                return null;
            }

            case 1: {
                return results.get(0);
            }

            default: {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Returns the results of executing a query.
     *
     * @return
     * The query results, or <code>null</code> if the query did not produce a
     * result set.
     */
    public List<Map<String, Object>> getResults() {
        return results;
    }

    /**
     * Returns the number of rows that were affected by the query.
     *
     * @return
     * The number of rows that were affected by the query, or -1 if the query
     * did not produce an update count.
     */
    public int getUpdateCount() {
        return updateCount;
    }

    /**
     * Returns the keys that were generated by the query.
     *
     * @return
     * The keys that were generated by the query, or <code>null</code> if the
     * query did not produce any generated keys.
     */
    public List<Object> getGeneratedKeys() {
        return generatedKeys;
    }

    /**
     * Prepares a query for execution.
     *
     * @param connection
     * The connection on which the query will be executed.
     *
     * @return
     * A prepared statement that can be used to execute the query.
     *
     * @throws SQLException
     * If an error occurs while preparing the query.
     */
    public PreparedStatement prepare(Connection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException();
        }

        return connection.prepareStatement(getSQL(), Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * Executes a query.
     *
     * @param statement
     * The statement that will be used to execute the query.
     *
     * @param arguments
     * The query arguments.
     *
     * @return
     * The query results.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public ResultSet executeQuery(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (statement == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        apply(statement, arguments);

        return statement.executeQuery();
    }

    /**
     * Executes a query.
     *
     * @param statement
     * The statement that will be used to execute the query.
     *
     * @param arguments
     * The query arguments.
     *
     * @return
     * The number of rows that were affected by the query.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public int executeUpdate(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (statement == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        apply(statement, arguments);

        return statement.executeUpdate();
    }

    private void apply(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (parameters == null) {
            throw new IllegalStateException();
        }

        int i = 1;

        for (String parameter : parameters) {
            if (parameter == null) {
                continue;
            }

            statement.setObject(i++, arguments.get(parameter));
        }
    }

    /**
     * Returns the parameters parsed by the query builder.
     *
     * @return
     * The parameters parsed by the query builder.
     */
    public Collection<String> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Returns the generated SQL.
     *
     * @return
     * The generated SQL.
     */
    public String getSQL() {
        return sqlBuilder.toString();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        Iterator<String> parameterIterator = parameters.iterator();

        for (int i = 0, n = sqlBuilder.length(); i < n; i++) {
            char c = sqlBuilder.charAt(i);

            if (c == '?') {
                String parameter = parameterIterator.next();

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
