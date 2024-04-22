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

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.util.Optionals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static org.httprpc.kilo.util.Collections.mapOf;

/**
 * Provides support for programmatically constructing and executing SQL
 * queries.
 */
public class QueryBuilder {
    private StringBuilder sqlBuilder;
    private List<String> parameters;
    private Map<String, Function<Object, Object>> transforms;

    private LinkedList<Class<?>> types = new LinkedList<>();

    private int filterCount = 0;

    private List<Object> generatedKeys = null;

    private static final int INITIAL_CAPACITY = 1024;

    private static final String WHERE = "where";
    private static final String AND = "and";

    private static final Function<Object, Object> toJSON = value -> {
        var jsonEncoder = new JSONEncoder(true);

        var valueWriter = new StringWriter();

        try {
            jsonEncoder.write(BeanAdapter.adapt(value), valueWriter);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return valueWriter.toString();
    };

    private static final Function<Object, Object> fromJSON = value -> {
        var jsonDecoder = new JSONDecoder();

        try {
            return jsonDecoder.read(new StringReader((String)value));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    };

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
        this(new StringBuilder(capacity), new LinkedList<>(), new HashMap<>(), null);
    }

    private QueryBuilder(StringBuilder sqlBuilder, LinkedList<String> parameters, Map<String, Function<Object, Object>> transforms, Class<?> type) {
        this.sqlBuilder = sqlBuilder;
        this.parameters = parameters;
        this.transforms = transforms;

        types.add(type);
    }

    /**
     * Creates a "select" query.
     *
     * @param types
     * The types representing the tables to select from.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder select(Class<?>... types) {
        if (types.length == 0) {
            throw new UnsupportedOperationException();
        }

        var sqlBuilder = new StringBuilder("select ");

        var transforms = new HashMap<String, Function<Object, Object>>();

        var i = 0;

        for (var j = 0; j < types.length; j++) {
            var type = types[j];

            var tableName = getTableName(type);

            for (var entry : BeanAdapter.getProperties(type).entrySet()) {
                var accessor = entry.getValue().getAccessor();

                var column = accessor.getAnnotation(Column.class);

                if (column == null) {
                    continue;
                }

                if (j > 0 && accessor.getAnnotation(ForeignKey.class) != null) {
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

                if (accessor.getAnnotation(JSON.class) != null) {
                    transforms.put(propertyName, fromJSON);
                }

                i++;
            }
        }

        if (i == 0) {
            throw new UnsupportedOperationException("No columns defined.");
        }

        sqlBuilder.append(" from ");
        sqlBuilder.append(getTableName(types[0]));
        sqlBuilder.append("\n");

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), transforms, types[0]);
    }

    /**
     * Creates a "select distinct" query.
     *
     * @param type
     * The type representing the table to select from.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder selectDistinctIndex(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder("select distinct ");

        var tableName = getTableName(type);

        var i = 0;

        for (var indexColumnName : getIndexColumnNames(type)) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(tableName);
            sqlBuilder.append(".");
            sqlBuilder.append(indexColumnName);

            i++;
        }

        sqlBuilder.append(" from ");
        sqlBuilder.append(tableName);
        sqlBuilder.append("\n");

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), new HashMap<>(), type);
    }

    private static String getTableName(Class<?> type) {
        var table = type.getAnnotation(Table.class);

        if (table == null) {
            throw new UnsupportedOperationException("Table name is not defined.");
        }

        return table.value();
    }

    /**
     * Appends a "join" clause linking to the primary key of another table.
     *
     * @param parentType
     * The type that defines the primary key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder joinOnPrimaryKey(Class<?> parentType) {
        if (parentType == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(parentType);

        var last = types.getLast();

        sqlBuilder.append("join ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(last));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(last, parentType));
        sqlBuilder.append(" = ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(parentType));
        sqlBuilder.append("\n");

        types.add(parentType);

        return this;
    }

    /**
     * Appends a "join" clause linking to a foreign key in another table.
     *
     * @param type
     * The type that defines the foreign key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder joinOnForeignKey(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var first = types.getFirst();

        sqlBuilder.append("join ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(first));
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(first));
        sqlBuilder.append(" = ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, first));
        sqlBuilder.append("\n");

        types.add(type);

        return this;
    }

    /**
     * Appends a "join" clause linking to a foreign key in another table.
     *
     * @param type
     * The type that defines the foreign key.
     *
     * @param parentType
     * The type that defines the primary key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder joinOnForeignKey(Class<?> type, Class<?> parentType) {
        if (type == null || parentType == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var first = types.getFirst();

        sqlBuilder.append("join ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(first));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(first, parentType));
        sqlBuilder.append(" = ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, parentType));
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
                    var type = to;

                    while (type != null) {
                        if (foreignKey.value() == type) {
                            return column.value();
                        }

                        if (type.isInterface()) {
                            var interfaces = type.getInterfaces();

                            if (interfaces.length > 0) {
                                type = interfaces[0];
                            } else {
                                type = null;
                            }
                        } else {
                            type = type.getSuperclass();
                        }
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
        var transforms = new HashMap<String, Function<Object, Object>>();

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

            var columnName = column.value();

            columnNames.add(columnName);

            var propertyName = entry.getKey();

            parameters.add(propertyName);

            if (accessor.getAnnotation(JSON.class) != null) {
                transforms.put(propertyName, toJSON);
            }
        }

        if (columnNames.isEmpty()) {
            throw new UnsupportedOperationException("No columns defined.");
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

        return new QueryBuilder(sqlBuilder, parameters, transforms, type);
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
        var transforms = new HashMap<String, Function<Object, Object>>();

        for (var entry : BeanAdapter.getProperties(type).entrySet()) {
            var accessor = entry.getValue().getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column == null) {
                continue;
            }

            if (accessor.getAnnotation(PrimaryKey.class) != null || accessor.getAnnotation(Final.class) != null) {
                continue;
            }

            if (i > 0) {
                sqlBuilder.append(", ");
            }

            var columnName = column.value();

            sqlBuilder.append(columnName);
            sqlBuilder.append(" = ?");

            var propertyName = entry.getKey();

            parameters.add(propertyName);

            if (accessor.getAnnotation(JSON.class) != null) {
                transforms.put(propertyName, toJSON);
            }

            i++;
        }

        if (i == 0) {
            throw new UnsupportedOperationException("No columns defined.");
        }

        sqlBuilder.append("\n");

        return new QueryBuilder(sqlBuilder, parameters, transforms, type);
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

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), new HashMap<>(), type);
    }

    /**
     * Filters on the primary key.
     *
     * @param key
     * The key of the argument representing the primary key value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByPrimaryKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var first = types.getFirst();

        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(getTableName(first));
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(first));
        sqlBuilder.append(" = ?\n");

        parameters.add(key);

        filterCount++;

        return this;
    }

    /**
     * Filters on a foreign key.
     *
     * @param parentType
     * The type that defines the primary key.
     *
     * @param key
     * The key of the argument representing the foreign key value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByForeignKey(Class<?> parentType, String key) {
        return filterByForeignKey(types.getFirst(), parentType, key);
    }

    /**
     * Filters on a foreign key.
     *
     * @param type
     * The type that defines the foreign key.
     *
     * @param parentType
     * The type that defines the primary key.
     *
     * @param key
     * The key of the argument representing the foreign key value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByForeignKey(Class<?> type, Class<?> parentType, String key) {
        if (type == null || parentType == null || key == null) {
            throw new IllegalArgumentException();
        }

        if (!types.contains(type)) {
            throw new UnsupportedOperationException("Table has not been joined.");
        }

        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(getTableName(type));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, parentType));
        sqlBuilder.append(" = ?\n");

        parameters.add(key);

        filterCount++;

        return this;
    }

    /**
     * Appends a "greater than" filter.
     *
     * @param key
     * The key of the argument value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIndexGreaterThan(String key) {
        return filterByIndex(">", key);
    }

    /**
     * Appends a "greater than or equal to" filter.
     *
     * @param key
     * The key of the argument value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIndexGreaterThanOrEqualTo(String key) {
        return filterByIndex(">=", key);
    }

    /**
     * Appends a "less than" filter.
     *
     * @param key
     * The key of the argument value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIndexLessThan(String key) {
        return filterByIndex("<", key);
    }

    /**
     * Appends a "less than or equal to" filter.
     *
     * @param key
     * The key of the argument value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIndexLessThanOrEqualTo(String key) {
        return filterByIndex("<=", key);
    }

    /**
     * Appends a "like" filter.
     *
     * @param key
     * The key of the argument value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIndexLike(String key) {
        return filterByIndex("like", key);
    }

    private QueryBuilder filterByIndex(String operator, String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var first = types.getFirst();

        var tableName = getTableName(first);

        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getIndexColumnNames(first).get(0));
        sqlBuilder.append(" ");
        sqlBuilder.append(operator);
        sqlBuilder.append(" ?\n");

        parameters.add(key);

        filterCount++;

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
        var first = types.getFirst();

        var tableName = getTableName(first);

        sqlBuilder.append("order by ");

        var i = 0;

        for (var indexColumnName : getIndexColumnNames(first)) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(tableName);
            sqlBuilder.append(".");
            sqlBuilder.append(indexColumnName);
            sqlBuilder.append(" ");
            sqlBuilder.append(ascending ? "asc" : "desc");

            i++;
        }

        sqlBuilder.append("\n");

        return this;
    }

    private static List<String> getIndexColumnNames(Class<?> type) {
        var indexColumnNames = new TreeMap<Integer, String>();

        for (var property : BeanAdapter.getProperties(type).values()) {
            var accessor = property.getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column != null) {
                var index = accessor.getAnnotation(Index.class);

                if (index != null) {
                    indexColumnNames.put(index.value(), column.value());
                }
            }
        }

        if (indexColumnNames.isEmpty()) {
            throw new UnsupportedOperationException("Index is not defined.");
        }

        return new ArrayList<>(indexColumnNames.values());
    }

    /**
     * Appends a "limit" clause.
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

        sqlBuilder.append("limit ");
        sqlBuilder.append(count);
        sqlBuilder.append("\n");

        return this;
    }

    /**
     * Appends a "for update" clause.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder forUpdate() {
        sqlBuilder.append("for update\n");

        return this;
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
    public ResultSetAdapter executeQuery(PreparedStatement statement) throws SQLException {
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
    public ResultSetAdapter executeQuery(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        if (statement == null || arguments == null) {
            throw new IllegalArgumentException();
        }

        apply(statement, arguments);

        var resultSetAdapter = new ResultSetAdapter(statement.executeQuery());

        resultSetAdapter.setTransforms(transforms);

        return resultSetAdapter;
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
     * Returns a generated key.
     *
     * @param index
     * The index of the generated key.
     *
     * @param type
     * The type of the generated key.
     *
     * @return
     * The generated key.
     */
    public <T> T getGeneratedKey(int index, Class<T> type) {
        if (generatedKeys == null) {
            throw new IllegalStateException("No generated keys.");
        }

        return BeanAdapter.coerce(generatedKeys.get(index), type);
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
            var value = arguments.get(parameter);

            if (value instanceof Enum<?>) {
                statement.setObject(i, value.toString());
            } else if (value instanceof Date date) {
                statement.setObject(i, date.getTime());
            } else {
                statement.setObject(i, Optionals.map(value, Optionals.coalesce(transforms.get(parameter), Function.identity())));
            }

            i++;
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
