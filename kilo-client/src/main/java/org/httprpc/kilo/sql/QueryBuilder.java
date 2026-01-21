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
import org.httprpc.kilo.xml.ElementAdapter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static org.httprpc.kilo.util.Collections.*;

/**
 * Provides support for programmatically constructing and executing SQL
 * queries.
 */
public class QueryBuilder {
    private StringBuilder sqlBuilder;
    private List<String> parameters;
    private Map<String, Function<Object, Object>> transforms;

    private Deque<Class<?>> types = new LinkedList<>();

    private boolean whitespaceAllowed = false;

    private int filterCount = 0;

    private List<Object> generatedKeys = null;

    private static final int INITIAL_CAPACITY = 1024;

    private static final String WHERE = "where";
    private static final String AND = "and";

    private static class EnumTransform implements Function<Object, Object> {
        Class<?> type;

        EnumTransform(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object apply(Object value) {
            if (value instanceof Number number) {
                value = number.intValue();
            }

            var fields = type.getDeclaredFields();

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];

                if (!field.isEnumConstant()) {
                    continue;
                }

                Object constant;
                try {
                    constant = field.get(null);
                } catch (IllegalAccessException exception) {
                    throw new RuntimeException(exception);
                }

                Object identifier;
                if (constant instanceof Numeric numeric) {
                    identifier = numeric.value();
                } else {
                    identifier = constant.toString();
                }

                if (value.equals(identifier)) {
                    return constant;
                }
            }

            throw new IllegalArgumentException("Invalid value.");
        }
    }

    private static final Function<Object, Object> fromJSON = value -> {
        var jsonDecoder = new JSONDecoder();

        try {
            return jsonDecoder.read(new StringReader((String)value));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    };

    private static final Function<Object, Object> toJSON = value -> {
        var jsonEncoder = new JSONEncoder(true);

        var writer = new StringWriter();

        try {
            jsonEncoder.write(value, writer);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return writer.toString();
    };

    private static final Function<Object, Object> fromXML = value -> {
        var documentBuilder = ElementAdapter.newDocumentBuilder();

        try {
            return documentBuilder.parse(new InputSource(new StringReader((String)value)));
        } catch (SAXException | IOException exception) {
            throw new RuntimeException(exception);
        }
    };

    private static final Function<Object, Object> toXML = value -> {
        var transformer = ElementAdapter.newTransformer();

        var writer = new StringWriter();

        try {
            transformer.transform(new DOMSource((Document)value), new StreamResult(writer));
        } catch (TransformerException exception) {
            throw new RuntimeException(exception);
        }

        return writer.toString();
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

    private QueryBuilder(StringBuilder sqlBuilder, List<String> parameters, Map<String, Function<Object, Object>> transforms, Class<?> type) {
        this.sqlBuilder = sqlBuilder;
        this.parameters = parameters;
        this.transforms = transforms;

        if (type != null) {
            types.add(type);
        }
    }

    /**
     * Indicates that whitespace is allowed.
     *
     * @return
     * {@code true} if whitespace is allowed; {@code false}, otherwise.
     */
    public boolean isWhitespaceAllowed() {
        return whitespaceAllowed;
    }

    /**
     * Toggles whitespace support.
     *
     * @param whitespaceAllowed
     * {@code true} to enable whitespace support; {@code false} to disable it.
     */
    public void setWhitespaceAllowed(boolean whitespaceAllowed) {
        this.whitespaceAllowed = whitespaceAllowed;
    }

    /**
     * Creates a "select" query.
     * <p>
     * {@link Enum} properties will be decoded from either a string or
     * {@link Numeric#value()}.
     * <p>
     * Properties annotated with {@link JSON} will be automatically
     * deserialized from a JSON string. Properties of type {@link Document}
     * will be automatically deserialized from an XML string.
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

            if (type == null) {
                throw new IllegalArgumentException();
            }

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

                var transform = getReadTransform(accessor);

                if (transform != null) {
                    transforms.put(propertyName, transform);
                }

                i++;
            }
        }

        if (i == 0) {
            throw new UnsupportedOperationException("Table does not define any columns.");
        }

        sqlBuilder.append(" from ");
        sqlBuilder.append(getTableName(types[0]));

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), transforms, types[0]);
    }

    /**
     * Creates a "select all" query.
     *
     * This method is typically used with {@link #filterByExists(QueryBuilder)}
     * or {@link #filterByNotExists(QueryBuilder)}. Aliases and transforms are
     * not applied.
     *
     * @param type
     * The type representing the table to select from.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder selectAll(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder("select ");

        var tableName = getTableName(type);

        sqlBuilder.append(tableName);
        sqlBuilder.append(".* from ");
        sqlBuilder.append(tableName);

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), new HashMap<>(), type);
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

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), new HashMap<>(), type);
    }

    private static String getTableName(Class<?> type) {
        while (type != null) {
            var table = type.getAnnotation(Table.class);

            if (table != null) {
                return table.value();
            }

            type = getSupertype(type);
        }

        throw new UnsupportedOperationException("Table name is not defined.");
    }

    private static Class<?> getSupertype(Class<?> type) {
        if (type.isInterface()) {
            var interfaces = type.getInterfaces();

            if (interfaces.length > 0) {
                return interfaces[0];
            } else {
                return null;
            }
        } else {
            return type.getSuperclass();
        }
    }

    private static Function<Object, Object> getReadTransform(Method accessor) {
        var returnType = accessor.getReturnType();

        if (returnType.isEnum()) {
            return new EnumTransform(returnType);
        } else if (accessor.getAnnotation(JSON.class) != null) {
            return fromJSON;
        } else if (returnType == Document.class) {
            return fromXML;
        } else {
            return null;
        }
    }

    /**
     * Appends a "join" clause linking the most recently joined type to another
     * type.
     *
     * @param parentType
     * The type representing both the table to join and the table that defines
     * the primary key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     *
     * @deprecated
     * This method will be removed in a future release. Use
     * {@link #join(Class, Class)} instead.
     */
    @Deprecated
    public QueryBuilder join(Class<?> parentType) {
        return join(parentType, parentType);
    }

    /**
     * Appends a "join" clause linking the most recently joined type to another
     * type.
     *
     * @param type
     * The type representing the table to join.
     *
     * @param parentType
     * The type representing the table that defines the primary key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder join(Class<?> type, Class<?> parentType) {
        if (type == null || parentType == null) {
            throw new IllegalArgumentException();
        }

        var tableName = getTableName(type);

        var last = types.getLast();

        sqlBuilder.append(" join ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(last));
        sqlBuilder.append(".");

        if (parentType == last) {
            sqlBuilder.append(getPrimaryKeyColumnName(parentType));
        } else {
            sqlBuilder.append(getForeignKeyColumnName(last, parentType));
        }

        sqlBuilder.append(" = ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");

        if (parentType == type) {
            sqlBuilder.append(getPrimaryKeyColumnName(parentType));
        } else {
            sqlBuilder.append(getForeignKeyColumnName(type, parentType));
        }

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

                        type = getSupertype(type);
                    }
                }
            }
        }

        throw new UnsupportedOperationException("Foreign key is not defined.");
    }

    /**
     * Appends a "union" operation.
     *
     * @param queryBuilder
     * A "select" query.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder union(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" union ");
        sqlBuilder.append(queryBuilder);

        parameters.addAll(queryBuilder.parameters);

        return this;
    }

    /**
     * Creates an "insert" query.
     *
     * @param type
     * The type representing the table to insert into. Properties annotated
     * with {@link JSON} will be automatically serialized to a JSON string.
     * Properties of type {@link Document} will be automatically serialized to
     * an XML string.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder insert(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder("insert into ");

        sqlBuilder.append(getTableName(type));

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

            var transform = getWriteTransform(accessor);

            if (transform != null) {
                transforms.put(propertyName, transform);
            }
        }

        if (columnNames.isEmpty()) {
            throw new UnsupportedOperationException("Table does not define any columns.");
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

        sqlBuilder.append(")");

        return new QueryBuilder(sqlBuilder, parameters, transforms, type);
    }

    /**
     * Appends an "on duplicate key update" clause.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder onDuplicateKeyUpdate() {
        var type = types.getFirst();

        sqlBuilder.append(" on duplicate key update ");

        var i = 0;

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
            sqlBuilder.append(" = values(");
            sqlBuilder.append(columnName);
            sqlBuilder.append(")");

            i++;
        }

        if (i == 0) {
            throw new UnsupportedOperationException("Table does not define any updatable columns.");
        }

        return this;
    }

    /**
     * Creates an "update" query.
     *
     * @param type
     * The type representing the table to update. Properties annotated with
     * {@link JSON} will be automatically serialized to a JSON string.
     * Properties of type {@link Document} will be automatically serialized to
     * an XML string.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder update(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder("update ");

        sqlBuilder.append(getTableName(type));
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

            sqlBuilder.append(column.value());
            sqlBuilder.append(" = ?");

            var propertyName = entry.getKey();

            parameters.add(propertyName);

            var transform = getWriteTransform(accessor);

            if (transform != null) {
                transforms.put(propertyName, transform);
            }

            i++;
        }

        if (i == 0) {
            throw new UnsupportedOperationException("Table does not define any updatable columns.");
        }

        return new QueryBuilder(sqlBuilder, parameters, transforms, type);
    }

    /**
     * Creates an "update" query.
     *
     * @param type
     * The type representing the table to update.
     *
     * @param parentType
     * The type representing the table that defines the primary key.
     *
     * @param key
     * The key of the argument representing the foreign key value.
     *
     * @return
     * A new {@link QueryBuilder} instance.
     */
    public static QueryBuilder updateParent(Class<?> type, Class<?> parentType, String key) {
        if (type == null || parentType == null || key == null) {
            throw new IllegalArgumentException();
        }

        var sqlBuilder = new StringBuilder("update ");

        sqlBuilder.append(getTableName(type));
        sqlBuilder.append(" set ");
        sqlBuilder.append(getForeignKeyColumnName(type, parentType));
        sqlBuilder.append(" = ?");

        var parameters = new LinkedList<String>();

        parameters.add(key);

        return new QueryBuilder(sqlBuilder, parameters, new HashMap<>(), type);
    }

    private static Function<Object, Object> getWriteTransform(Method accessor) {
        if (accessor.getAnnotation(JSON.class) != null) {
            return toJSON;
        } else if (accessor.getReturnType() == Document.class) {
            return toXML;
        } else {
            return null;
        }
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

        var sqlBuilder = new StringBuilder("delete from ");

        sqlBuilder.append(getTableName(type));

        return new QueryBuilder(sqlBuilder, new LinkedList<>(), new HashMap<>(), type);
    }

    /**
     * Filters by the primary key of the first selected type.
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

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(getTableName(first));
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(first));
        sqlBuilder.append(" = ?");

        parameters.add(key);

        filterCount++;

        return this;
    }

    /**
     * Filters by a foreign key defined by the first selected type.
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
     * Filters by a foreign key defined by a joined type.
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

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(getTableName(type));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, parentType));
        sqlBuilder.append(" = ?");

        parameters.add(key);

        filterCount++;

        return this;
    }

    /**
     * Filters by a foreign key defined by the first selected type.
     * <p>
     * This method is typically used with correlated sub-queries.
     *
     * @param parentType
     * The type that defines the primary key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByForeignKey(Class<?> parentType) {
        return filterByForeignKey(types.getFirst(), parentType);
    }

    /**
     * Filters by a foreign key defined by a joined type.
     * <p>
     * This method is typically used with correlated sub-queries.
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
    public QueryBuilder filterByForeignKey(Class<?> type, Class<?> parentType) {
        if (type == null || parentType == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(getTableName(type));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, parentType));
        sqlBuilder.append(" = ");
        sqlBuilder.append(getTableName(parentType));
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(parentType));

        filterCount++;

        return this;
    }

    /**
     * Filters by identifier.
     *
     * @param keys
     * The keys of the arguments representing the identifier values.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIdentifier(String... keys) {
        if (keys.length == 0) {
            throw new UnsupportedOperationException();
        }

        var first = types.getFirst();

        var tableName = getTableName(first);
        var identifierColumnNames = getIdentifierColumnNames(first);

        for (var i = 0; i < keys.length; i++) {
            sqlBuilder.append(" ");
            sqlBuilder.append(filterCount == 0 ? WHERE : AND);
            sqlBuilder.append(" ");
            sqlBuilder.append(tableName);
            sqlBuilder.append(".");
            sqlBuilder.append(identifierColumnNames.get(i));
            sqlBuilder.append(" = ?");

            parameters.add(keys[i]);

            filterCount++;
        }

        return this;
    }

    private static List<String> getIdentifierColumnNames(Class<?> type) {
        var identifierColumnNames = new TreeMap<Integer, String>();

        for (var property : BeanAdapter.getProperties(type).values()) {
            var accessor = property.getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column != null) {
                var identifier = accessor.getAnnotation(Identifier.class);

                if (identifier != null) {
                    identifierColumnNames.put(identifier.value(), column.value());
                }
            }
        }

        if (identifierColumnNames.isEmpty()) {
            throw new UnsupportedOperationException("Identifier is not defined.");
        }

        return new ArrayList<>(identifierColumnNames.values());
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
     * Appends one or more "equal to" filters.
     *
     * @param keys
     * The keys of the argument values.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIndexEqualTo(String... keys) {
        return filterByIndex("=", keys);
    }

    /**
     * Appends one or more "like" filters.
     *
     * @param keys
     * The keys of the argument values.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIndexLike(String... keys) {
        return filterByIndex("like", keys);
    }

    private QueryBuilder filterByIndex(String operator, String... keys) {
        if (keys.length == 0) {
            throw new UnsupportedOperationException();
        }

        var first = types.getFirst();

        var tableName = getTableName(first);
        var indexColumnNames = getIndexColumnNames(first);

        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];

            if (key == null) {
                throw new IllegalArgumentException();
            }

            sqlBuilder.append(" ");
            sqlBuilder.append(filterCount == 0 ? WHERE : AND);
            sqlBuilder.append(" ");
            sqlBuilder.append(tableName);
            sqlBuilder.append(".");
            sqlBuilder.append(indexColumnNames.get(i));
            sqlBuilder.append(" ");
            sqlBuilder.append(operator);
            sqlBuilder.append(" ?");

            parameters.add(key);

            filterCount++;
        }

        return this;
    }

    /**
     * Appends an "exists" filter.
     *
     * @param queryBuilder
     * A "select" query.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByExists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" exists (");
        sqlBuilder.append(queryBuilder);
        sqlBuilder.append(")");

        filterCount++;

        parameters.addAll(queryBuilder.parameters);

        return this;
    }

    /**
     * Appends a "not exists" filter.
     *
     * @param queryBuilder
     * A "select" query.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByNotExists(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" not exists (");
        sqlBuilder.append(queryBuilder);
        sqlBuilder.append(")");

        filterCount++;

        parameters.addAll(queryBuilder.parameters);

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

        sqlBuilder.append(" order by ");

        var i = 0;

        for (var indexColumnName : getIndexColumnNames(first)) {
            if (i > 0) {
                sqlBuilder.append(", ");
            }

            sqlBuilder.append(indexColumnName);
            sqlBuilder.append(" ");
            sqlBuilder.append(ascending ? "asc" : "desc");

            i++;
        }

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

        sqlBuilder.append(" limit ");
        sqlBuilder.append(count);

        return this;
    }

    /**
     * Appends a "for update" clause.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder forUpdate() {
        sqlBuilder.append(" for update");

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
     * Returns the parameter count.
     *
     * @return
     * The parameter count.
     */
    public int getParameterCount() {
        return parameters.size();
    }

    /**
     * Returns a parameter.
     *
     * @param index
     * The parameter index.
     *
     * @return
     * The parameter at the given index.
     */
    public String getParameter(int index) {
        return parameters.get(index);
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
     * <p>
     * {@link Enum} values will be encoded to either a string or
     * {@link Numeric#value()}.</p>
     * <p>
     * Temporal values are converted as follows:
     * <p>
     * <ul>
     * <li>{@link Date} - long value representing epoch time in milliseconds</li>
     * <li>{@link LocalDate} - {@link java.sql.Date}</li>
     * <li>{@link LocalTime} - {@link java.sql.Time}</li>
     * <li>{@link Instant} - {@link java.sql.Timestamp}</li>
     * </ul>
     * <p>
     * All other arguments are applied as is.
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

        return new ResultSetAdapter(statement.executeQuery(), transforms);
    }

    /**
     * Executes a query.
     * <p>
     * Arguments are applied as described for
     * {@link #executeQuery(PreparedStatement, Map)}, or transformed as
     * specified by {@link #insert(Class)} and {@link #update(Class)}.
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
     * @param <T>
     * The type of the generated key.
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
            var argument = arguments.get(parameter);

            Object value;
            if (argument instanceof Enum<?>) {
                if (argument instanceof Numeric numeric) {
                    value = numeric.value();
                } else {
                    value = argument.toString();
                }
            } else {
                switch (argument) {
                    case Date date -> value = date.getTime();
                    case LocalDate localDate -> value = java.sql.Date.valueOf(localDate);
                    case LocalTime localTime -> value = java.sql.Time.valueOf(localTime);
                    case Instant instant -> value = java.sql.Timestamp.from(instant);
                    case null, default -> {
                        var transform = transforms.get(parameter);

                        if (transform != null && argument != null) {
                            value = transform.apply(argument);
                        } else {
                            if (!whitespaceAllowed && argument instanceof String text) {
                                var n = text.length();

                                if (n > 0 && (Character.isWhitespace(text.charAt(0)) || Character.isWhitespace(text.charAt(n - 1)))) {
                                    throw new IllegalArgumentException("Value contains leading or trailing whitespace.");
                                }
                            }

                            value = argument;
                        }
                    }
                }
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
