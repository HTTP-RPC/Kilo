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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.mapOf;

/**
 * Provides support for programmatically constructing and executing SQL
 * queries.
 */
public class QueryBuilder {
    private StringBuilder sqlBuilder;

    private List<String> parameters = new LinkedList<>();
    private List<Object> generatedKeys = null;

    private static final int INITIAL_CAPACITY = 1024;

    /**
     * Constructs a new query builder.
     */
    public QueryBuilder() {
        this(INITIAL_CAPACITY);
    }

    /**
     * Constructs a new query builder.
     *
     * @param capacity
     * The initial capacity.
     */
    public QueryBuilder(int capacity) {
        this(new StringBuilder(capacity));
    }

    private QueryBuilder(StringBuilder sqlBuilder) {
        this.sqlBuilder = sqlBuilder;
    }

    /**
     * Appends arbitrary SQL text to a query. Named parameters can be declared
     * by prepending a colon to an argument name.
     *
     * @param sql
     * The SQL text to append.
     */
    public void append(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException();
        }

        var quoted = false;

        var n = sql.length();
        var i = 0;

        while (i < n) {
            var c = sql.charAt(i++);

            if (c == ':' && !quoted) {
                var parameterBuilder = new StringBuilder(32);

                while (i < n) {
                    c = sql.charAt(i);

                    if (!Character.isJavaIdentifierPart(c)) {
                        break;
                    }

                    parameterBuilder.append(c);

                    i++;
                }

                if (parameterBuilder.isEmpty()) {
                    throw new IllegalArgumentException("Missing parameter name.");
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
     * Returns the query builder's parameters.
     *
     * @return
     * The query builder's parameters.
     */
    public List<String> getParameters() {
        return Collections.unmodifiableList(parameters);
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

        return connection.prepareStatement(toString(), Statement.RETURN_GENERATED_KEYS);
    }

    /**
     * Executes a query.
     *
     * @param statement
     * The statement that will be used to execute the query.
     *
     * @return
     * The query results.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public ResultSet executeQuery(PreparedStatement statement) throws SQLException {
        return executeQuery(statement, mapOf());
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
     * @return
     * The number of rows that were affected by the query.
     *
     * @throws SQLException
     * If an error occurs while executing the query.
     */
    public int executeUpdate(PreparedStatement statement) throws SQLException {
        return executeUpdate(statement, mapOf());
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

        var updateCount = statement.executeUpdate();

        try (var generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                var generatedKeysMetaData = generatedKeys.getMetaData();

                var n = generatedKeysMetaData.getColumnCount();

                this.generatedKeys = new ArrayList<>(n);

                for (var i = 0; i < n; i++) {
                    this.generatedKeys.add(generatedKeys.getObject(i + 1));
                }
            } else {
                this.generatedKeys = null;
            }
        }

        return updateCount;
    }

    /**
     * Returns the keys that were generated by the query.
     *
     * @return
     * The list of generated keys, or {@code null} if the query did not produce
     * any keys.
     */
    public List<Object> getGeneratedKeys() {
        if (generatedKeys != null) {
            return Collections.unmodifiableList(generatedKeys);
        } else {
            return null;
        }
    }

    /**
     * Appends a set of arguments to a prepared statement.
     *
     * @param statement
     * The prepared statement.
     *
     * @param arguments
     * The batch arguments.
     *
     * @throws SQLException
     * If an error occurs while adding the batch.
     */
    public void addBatch(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (statement == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        apply(statement, arguments);

        statement.addBatch();
    }

    private void apply(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        var i = 1;

        for (var parameter : parameters) {
            if (parameter == null) {
                continue;
            }

            var value = arguments.get(parameter);

            if (value instanceof Enum<?>) {
                value = value.toString();
            }

            statement.setObject(i++, value);
        }
    }

    /**
     * Returns the generated query text.
     *
     * @return
     * The generated query text.
     */
    @Override
    public String toString() {
        return sqlBuilder.toString();
    }
}
