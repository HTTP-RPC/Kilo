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
    private SchemaElement[] schemaElements;

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
        this(sqlBuilder, null);
    }

    private QueryBuilder(StringBuilder sqlBuilder, SchemaElement[] schemaElements) {
        this.sqlBuilder = sqlBuilder;
        this.schemaElements = schemaElements;
    }

    /**
     * Creates a "select all" query.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder selectAll() {
        var sqlBuilder = new StringBuilder(INITIAL_CAPACITY);

        sqlBuilder.append("select *");

        return new QueryBuilder(sqlBuilder);
    }

    /**
     * Creates a "select" query.
     *
     * @param schemaElements
     * A list of schema elements representing the columns to select.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder select(SchemaElement... schemaElements) {
        return select(false, schemaElements);
    }

    /**
     * Creates a "select distinct" query.
     *
     * @param schemaElements
     * A list of schema elements representing the columns to select.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder selectDistinct(SchemaElement... schemaElements) {
        return select(true, schemaElements);
    }

    private static QueryBuilder select(boolean distinct, SchemaElement... schemaElements) {
        if (schemaElements.length == 0) {
            throw new UnsupportedOperationException();
        }

        var sqlBuilder = new StringBuilder(INITIAL_CAPACITY);

        sqlBuilder.append("select ");

        if (distinct) {
            sqlBuilder.append("distinct ");
        }

        for (var i = 0; i < schemaElements.length; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            var schemaElement = schemaElements[i];

            sqlBuilder.append(schemaElement.getQualifiedName());

            var alias = schemaElement.getAlias();

            if (alias != null) {
                sqlBuilder.append(" as ");
                sqlBuilder.append(alias);
            }
        }

        return new QueryBuilder(sqlBuilder);
    }

    /**
     * Appends a "from" clause to a "select" query.
     *
     * @param schemaType
     * The schema type representing the table to select from.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder from(Class<? extends SchemaElement> schemaType) {
        if (schemaType == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" from ");
        sqlBuilder.append(SchemaElement.getTableName(schemaType));

        return this;
    }

    /**
     * Appends a "join" clause to a query.
     *
     * @param schemaType
     * The schema type representing the table to join on.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder join(Class<? extends SchemaElement> schemaType) {
        if (schemaType == null) {
            throw new IllegalArgumentException();
        }

        return join(null, schemaType);
    }

    /**
     * Appends a "left join" clause to a query.
     *
     * @param schemaType
     * The schema type representing the table to join on.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder leftJoin(Class<? extends SchemaElement> schemaType) {
        if (schemaType == null) {
            throw new IllegalArgumentException();
        }

        return join("left", schemaType);
    }

    /**
     * Appends a "right join" clause to a query.
     *
     * @param schemaType
     * The schema type representing the table to join on.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder rightJoin(Class<? extends SchemaElement> schemaType) {
        if (schemaType == null) {
            throw new IllegalArgumentException();
        }

        return join("right", schemaType);
    }

    private QueryBuilder join(String type, Class<? extends SchemaElement> schemaType) {
        if (type != null) {
            sqlBuilder.append(" ");
            sqlBuilder.append(type);
        }

        sqlBuilder.append(" join ");
        sqlBuilder.append(SchemaElement.getTableName(schemaType));

        return this;
    }

    /**
     * Appends an "on" clause to a query.
     *
     * @param predicateComponents
     * The predicate components.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder on(PredicateComponent... predicateComponents) {
        return filter("on", predicateComponents);
    }

    /**
     * Appends a "where" clause to a query.
     *
     * @param predicateComponents
     * The predicate components.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder where(PredicateComponent... predicateComponents) {
        return filter("where", predicateComponents);
    }

    /**
     * Appends a "group by" clause to a query.
     *
     * @param schemaElements
     * A list of schema elements representing the columns to group by.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder groupBy(SchemaElement... schemaElements) {
        if (schemaElements.length == 0) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append(" group by ");

        for (var i = 0; i < schemaElements.length; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(schemaElements[i].getQualifiedName());
        }

        return this;
    }

    /**
     * Appends a "having" clause to a query.
     *
     * @param predicateComponents
     * The predicate components.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder having(PredicateComponent... predicateComponents) {
        return filter("having", predicateComponents);
    }

    private QueryBuilder filter(String clause, PredicateComponent... predicateComponents) {
        if (predicateComponents.length == 0) {
            throw new UnsupportedOperationException();
        }

        var filterBuilder = new StringBuilder(64);

        filterBuilder.append(" ");
        filterBuilder.append(clause);
        filterBuilder.append(" ");

        for (var i = 0; i < predicateComponents.length; i++) {
            if (i > 0) {
                filterBuilder.append(" ");
            }

            filterBuilder.append(predicateComponents[i].toString());
        }

        append(filterBuilder.toString());

        return this;
    }

    /**
     * Appends an "order by" clause to a query.
     *
     * @param schemaElements
     * A list of schema elements representing the columns to order by.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder orderBy(SchemaElement... schemaElements) {
        if (schemaElements.length == 0) {
            throw new UnsupportedOperationException();
        }

        sqlBuilder.append(" order by ");

        for (var i = 0; i < schemaElements.length; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            var schemaElement = schemaElements[i];

            sqlBuilder.append(schemaElement.getQualifiedName());

            var sortOrder = schemaElement.getSortOrder();

            if (sortOrder != null) {
                sqlBuilder.append(" ");

                switch (sortOrder) {
                    case ASC -> sqlBuilder.append("asc");
                    case DESC -> sqlBuilder.append("desc");
                }
            }
        }

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

        append(queryBuilder);

        return this;
    }

    /**
     * Creates an "insert into" query.
     *
     * @param schemaType
     * The schema type representing the table to populate.
     *
     * @param schemaElements
     * A list of schema elements representing the columns to populate.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder insertInto(Class<? extends SchemaElement> schemaType, SchemaElement... schemaElements) {
        if (schemaType == null || schemaElements.length == 0) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder(INITIAL_CAPACITY);

        sqlBuilder.append("insert into ");
        sqlBuilder.append(SchemaElement.getTableName(schemaType));

        sqlBuilder.append(" (");

        for (var i = 0; i < schemaElements.length; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(schemaElements[i].getColumnName());
        }

        sqlBuilder.append(")");

        return new QueryBuilder(sqlBuilder, schemaElements);
    }

    /**
     * Appends a "values" clause to an "insert into" query.
     *
     * @param values
     * The values to insert.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder values(String... values) {
        if (schemaElements == null) {
            throw new IllegalStateException();
        }

        if (values.length != schemaElements.length) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" values (");

        for (var i = 0; i < values.length; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append("?");

            parameters.add(values[i]);
        }

        sqlBuilder.append(")");

        return this;
    }

    /**
     * Creates an "update" query.
     *
     * @param schemaType
     * The schema type representing the table to update.
     *
     * @param schemaElements
     * A list of schema elements representing the columns to update.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder update(Class<? extends SchemaElement> schemaType, SchemaElement... schemaElements) {
        if (schemaType == null || schemaElements.length == 0) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder(INITIAL_CAPACITY);

        sqlBuilder.append("update ");
        sqlBuilder.append(SchemaElement.getTableName(schemaType));

        return new QueryBuilder(sqlBuilder, schemaElements);
    }

    /**
     * Appends a "set" clause to an "update" query.
     *
     * @param values
     * The updated values.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder set(String... values) {
        if (schemaElements == null) {
            throw new IllegalStateException();
        }

        if (values.length != schemaElements.length) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" set ");

        for (var i = 0; i < schemaElements.length; i++) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            var schemaElement = schemaElements[i];

            var columnName = schemaElement.getColumnName();

            sqlBuilder.append(columnName);
            sqlBuilder.append(" = ");

            if (schemaElement.isRequired()) {
                sqlBuilder.append("?");
            } else {
                sqlBuilder.append("coalesce(?, ");
                sqlBuilder.append(columnName);
                sqlBuilder.append(")");
            }

            parameters.add(values[i]);
        }

        return this;
    }

    /**
     * Creates a "delete from" query.
     *
     * @param schemaType
     * The schema type.
     *
     * @return
     * The new {@link QueryBuilder} instance.
     */
    public static QueryBuilder deleteFrom(Class<? extends SchemaElement> schemaType) {
        if (schemaType == null) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder(INITIAL_CAPACITY);

        sqlBuilder.append("delete from ");
        sqlBuilder.append(SchemaElement.getTableName(schemaType));

        return new QueryBuilder(sqlBuilder);
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

    private void append(QueryBuilder queryBuilder) {
        sqlBuilder.append(queryBuilder);

        parameters.addAll(queryBuilder.parameters);
    }

    /**
     * Returns the parameters parsed by the query builder.
     *
     * @return
     * The parameters parsed by the query builder.
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

            statement.setObject(i++, arguments.get(parameter));
        }
    }

    @Override
    public String toString() {
        return sqlBuilder.toString();
    }
}
