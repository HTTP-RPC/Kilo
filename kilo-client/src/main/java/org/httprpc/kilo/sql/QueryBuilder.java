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

import org.httprpc.kilo.Required;
import org.httprpc.kilo.beans.BeanAdapter;

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
    private List<String> parameters;

    private LinkedList<Class<?>> types = new LinkedList<>();

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
        this(new StringBuilder(capacity), new LinkedList<>(), null);
    }

    private QueryBuilder(StringBuilder sqlBuilder, LinkedList<String> parameters, Class<?> type) {
        this.sqlBuilder = sqlBuilder;
        this.parameters = parameters;

        types.add(type);
    }

    /**
     * Creates a "select" query.
     *
     * @param type
     * The type representing the table to select from.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder select(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("select ");

        var i = 0;

        for (var entry : BeanAdapter.getProperties(type).entrySet()) {
            var accessor = entry.getValue().getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column == null) {
                continue;
            }

            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(tableName);
            sqlBuilder.append(".");

            var columnName = column.value();

            sqlBuilder.append(columnName);

            var propertyName = entry.getKey();

            if (!columnName.equals(propertyName)) {
                sqlBuilder.append(" as ");
                sqlBuilder.append(propertyName);
            }

            i++;
        }

        if (i == 0) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append(" from ");
        sqlBuilder.append(tableName);
        sqlBuilder.append("\n");

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), type);
    }

    private static String getTableName(Class<?> type) {
        var table = type.getAnnotation(Table.class);

        if (table == null) {
            throw new UnsupportedOperationException("Table name is not defined.");
        }

        return table.value();
    }

    /**
     * Creates a "join" clause linking to the primary key of another table.
     *
     * @param type
     * The type representing the table that defines the primary key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder joinOnPrimaryKey(Class<?> type) {
        return join(type, false);
    }

    /**
     * Creates a "join" clause linking to a foreign key in another table.
     *
     * @param type
     * The type representing the table that defines the foreign key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder joinOnForeignKey(Class<?> type) {
        return join(type, true);
    }

    private QueryBuilder join(Class<?> type, boolean reverse) {
        if (type == null || types.contains(type)) {
            throw new IllegalArgumentException();
        }

        Class<?> to;
        Class<?> from;
        if (reverse) {
            to = types.getFirst();
            from = type;
        } else {
            to = type;
            from = types.getLast();
        }

        sqlBuilder.append("join ");
        sqlBuilder.append(getTableName(type));
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(to));
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(to));
        sqlBuilder.append(" = ");
        sqlBuilder.append(getTableName(from));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(from, to));
        sqlBuilder.append("\n");

        types.add(type);

        return this;
    }

    private static String getPrimaryKeyColumnName(Class<?> type) {
        for (var property : BeanAdapter.getProperties(type).values()) {
            var accessor = property.getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column != null) {
                var primaryKey = accessor.getAnnotation(PrimaryKey.class);

                if (primaryKey != null) {
                    return column.value();
                }
            }
        }

        throw new UnsupportedOperationException("Primary key is not defined.");
    }

    private static String getForeignKeyColumnName(Class<?> from, Class<?> to) {
        for (var property : BeanAdapter.getProperties(from).values()) {
            var accessor = property.getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column != null) {
                var foreignKey = accessor.getAnnotation(ForeignKey.class);

                if (foreignKey != null) {
                    if (foreignKey.value() == to) {
                        return column.value();
                    }
                }
            }
        }

        throw new UnsupportedOperationException("Foreign key is not defined.");
    }

    /**
     * Creates an "insert" query.
     *
     * @param type
     * The type representing the table to insert into.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder insert(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("insert into ");

        sqlBuilder.append(tableName);

        var columnNames = new LinkedList<String>();
        var parameters = new LinkedList<String>();

        for (var entry : BeanAdapter.getProperties(type).entrySet()) {
            var accessor = entry.getValue().getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column == null) {
                continue;
            }

            var primaryKey = accessor.getAnnotation(PrimaryKey.class);

            if (primaryKey != null && primaryKey.generated()) {
                continue;
            }

            columnNames.add(column.value());
            parameters.add(entry.getKey());
        }

        if (columnNames.isEmpty()) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append(" (");

        var i = 0;

        for (var columnName : columnNames) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(columnName);

            i++;
        }

        sqlBuilder.append(") values (");

        for (var j = 0; j < i; j++) {
            if (j > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append("?");
        }

        sqlBuilder.append(")\n");

        return new QueryBuilder(sqlBuilder, parameters, type);
    }

    /**
     * Creates an "update" query.
     *
     * @param type
     * The type representing the table to update.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder update(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("update ");

        sqlBuilder.append(tableName);
        sqlBuilder.append(" set ");

        var i = 0;

        var parameters = new LinkedList<String>();

        for (var entry : BeanAdapter.getProperties(type).entrySet()) {
            var accessor = entry.getValue().getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column == null) {
                continue;
            }

            var primaryKey = accessor.getAnnotation(PrimaryKey.class);

            if (primaryKey != null && primaryKey.generated()) {
                continue;
            }

            if (i > 0) {
                sqlBuilder.append(", ");
            }

            var columnName = column.value();

            sqlBuilder.append(columnName);
            sqlBuilder.append(" = ");

            if (accessor.getAnnotation(Required.class) == null) {
                sqlBuilder.append("coalesce(?, ");
                sqlBuilder.append(columnName);
                sqlBuilder.append(")");
            } else {
                sqlBuilder.append("?");
            }

            parameters.add(entry.getKey());

            i++;
        }

        if (i == 0) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append("\n");

        return new QueryBuilder(sqlBuilder, parameters, type);
    }

    /**
     * Creates a "delete" query.
     *
     * @param type
     * The type representing the table to delete from.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder delete(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var sqlBuilder = new StringBuilder("delete from ");

        sqlBuilder.append(tableName);
        sqlBuilder.append("\n");

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), type);
    }

    /**
     * Appends a "where" clause that filters on the primary key.
     *
     * @param key
     * The key of the argument representing the primary key value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder wherePrimaryKeyEquals(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var firstType = types.getFirst();

        sqlBuilder.append("where ");
        sqlBuilder.append(getTableName(firstType));
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(firstType));
        sqlBuilder.append(" = ?\n");

        parameters.add(key);

        return this;
    }

    /**
     * Appends a "where" clause that filters on a foreign key.
     *
     * @param type
     * The type representing the table referenced by the foreign key.
     *
     * @param key
     * The key of the argument representing the foreign key value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder whereForeignKeyEquals(Class<?> type, String key) {
        if (type == null || key == null) {
            throw new IllegalArgumentException();
        }

        var lastType = types.getLast();

        sqlBuilder.append("where ");
        sqlBuilder.append(getTableName(lastType));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(lastType, type));
        sqlBuilder.append(" = ?\n");

        parameters.add(key);

        return this;
    }

    /**
     * Appends an "order by" clause.
     *
     * @param ascending
     * {@code true} for ascending order; {@code false} for descending.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder ordered(boolean ascending) {
        var firstType = types.getFirst();

        sqlBuilder.append("order by ");
        sqlBuilder.append(getTableName(firstType));
        sqlBuilder.append(".");
        sqlBuilder.append(getIndexColumnName(firstType));
        sqlBuilder.append(" ");
        sqlBuilder.append(ascending ? "asc" : "desc");
        sqlBuilder.append("\n");

        return this;
    }

    private static String getIndexColumnName(Class<?> type) {
        for (var property : BeanAdapter.getProperties(type).values()) {
            var accessor = property.getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column != null && accessor.getAnnotation(Index.class) != null) {
                return column.value();
            }
        }

        throw new UnsupportedOperationException("Index is not defined.");
    }

    /**
     * Appends arbitrary SQL text to a query. Named parameters can be declared
     * by prepending a colon to an argument name.
     *
     * @param text
     * The SQL text to append.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder append(String text) {
        if (text == null) {
            throw new IllegalArgumentException();
        }

        var quoted = false;

        var n = text.length();
        var i = 0;

        while (i < n) {
            var c = text.charAt(i++);

            if (c == ':' && !quoted) {
                var parameterBuilder = new StringBuilder(32);

                while (i < n) {
                    c = text.charAt(i);

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

        return this;
    }

    /**
     * Appends arbitrary SQL text to a query, terminated by a newline character.
     *
     * @param text
     * The SQL text to append.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder appendLine(String text) {
        append(text);

        sqlBuilder.append("\n");

        return this;
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
