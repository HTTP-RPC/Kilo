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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
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

    private static final Function<Object, Object> toJSON = value -> {
        var jsonEncoder = new JSONEncoder(true);

        var writer = new StringWriter();

        try {
            jsonEncoder.write(BeanAdapter.adapt(value), writer);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return writer.toString();
    };

    private static final Function<Object, Object> fromJSON = value -> {
        var jsonDecoder = new JSONDecoder();

        try {
            return jsonDecoder.read(new StringReader((String)value));
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    };

    private static final Function<Object, Object> toXML = value -> {
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException exception) {
            throw new RuntimeException(exception);
        }

        var writer = new StringWriter();

        try {
            transformer.transform(new DOMSource((Document)value), new StreamResult(writer));
        } catch (TransformerException exception) {
            throw new RuntimeException(exception);
        }

        return writer.toString();
    };

    private static final Function<Object, Object> fromXML = value -> {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        DocumentBuilder documentBuilder;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            throw new RuntimeException(exception);
        }

        try {
            return documentBuilder.parse(new InputSource(new StringReader((String)value)));
        } catch (SAXException | IOException exception) {
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
     * {@code true} if whitespace is allowed; {@code false}, otherwise.
     */
    public void setWhitespaceAllowed(boolean whitespaceAllowed) {
        this.whitespaceAllowed = whitespaceAllowed;
    }

    /**
     * Creates a "select" query.
     *
     * @param types
     * The types representing the tables to select from. Properties annotated
     * with {@link JSON} will be automatically deserialized from a JSON string.
     * Properties of type {@link Document} will be automatically deserialized
     * from an XML string.
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
     * Creates a "select" query.
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
        var table = type.getAnnotation(Table.class);

        if (table == null) {
            throw new UnsupportedOperationException("Table name is not defined.");
        }

        return table.value();
    }

    private static Function<Object, Object> getReadTransform(Method accessor) {
        if (accessor.getAnnotation(JSON.class) != null) {
            return fromJSON;
        } else if (accessor.getReturnType() == Document.class) {
            return fromXML;
        } else {
            return null;
        }
    }

    /**
     * Appends a "join" clause linking a foreign key defined by the most
     * recently joined type to the primary key of another type.
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

        sqlBuilder.append(" join ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(last));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(last, parentType));
        sqlBuilder.append(" = ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(parentType));

        types.add(parentType);

        return this;
    }

    /**
     * Appends a "join" clause linking the primary key of the first selected
     * type to a foreign key in another type.
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

        sqlBuilder.append(" join ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(first));
        sqlBuilder.append(".");
        sqlBuilder.append(getPrimaryKeyColumnName(first));
        sqlBuilder.append(" = ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, first));

        types.add(type);

        return this;
    }

    /**
     * Appends a "join" clause linking a foreign key defined by the first
     * selected type to a foreign key in another type.
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

        sqlBuilder.append(" join ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(" on ");
        sqlBuilder.append(getTableName(first));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(first, parentType));
        sqlBuilder.append(" = ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, parentType));

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
            throw new UnsupportedOperationException("Table does not define any columns.");
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
     * The type that defines the primary key.
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
     *
     * @param parentType
     * The type that defines the primary key.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByForeignKeyIsNull(Class<?> parentType) {
        return filterByForeignKeyIsNull(types.getFirst(), parentType);
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
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByForeignKeyIsNull(Class<?> type, Class<?> parentType) {
        if (type == null || parentType == null) {
            throw new IllegalArgumentException();
        }

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(getTableName(type));
        sqlBuilder.append(".");
        sqlBuilder.append(getForeignKeyColumnName(type, parentType));
        sqlBuilder.append(" is null");

        filterCount++;

        return this;
    }

    /**
     * Filters by identifier.
     *
     * @param key
     * The key of the argument representing the identifier value.
     *
     * @return
     * The {@link QueryBuilder} instance.
     */
    public QueryBuilder filterByIdentifier(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var first = types.getFirst();

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(getTableName(first));
        sqlBuilder.append(".");
        sqlBuilder.append(getIdentifierColumnName(first));
        sqlBuilder.append(" = ?");

        parameters.add(key);

        filterCount++;

        return this;
    }

    private static String getIdentifierColumnName(Class<?> type) {
        for (var property : BeanAdapter.getProperties(type).values()) {
            var accessor = property.getAccessor();

            var column = accessor.getAnnotation(Column.class);

            if (column != null) {
                var primaryKey = accessor.getAnnotation(Identifier.class);

                if (primaryKey != null) {
                    return column.value();
                }
            }
        }

        throw new UnsupportedOperationException("Identifier is not defined.");
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

        sqlBuilder.append(" ");
        sqlBuilder.append(filterCount == 0 ? WHERE : AND);
        sqlBuilder.append(" ");
        sqlBuilder.append(tableName);
        sqlBuilder.append(".");
        sqlBuilder.append(getIndexColumnNames(first).get(0));
        sqlBuilder.append(" ");
        sqlBuilder.append(operator);
        sqlBuilder.append(" ?");

        parameters.add(key);

        filterCount++;

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

        var tableName = getTableName(first);

        sqlBuilder.append(" order by ");

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
     * <p>Executes a query.</p>
     *
     * <p>{@link Enum} arguments are converted to strings. Temporal values are
     * converted as follows:</p>
     *
     * <ul>
     * <li>{@link Date} - long value representing epoch time in milliseconds</li>
     * <li>{@link LocalDate} - {@link java.sql.Date}</li>
     * <li>{@link LocalTime} - {@link java.sql.Time}</li>
     * <li>{@link Instant} - {@link java.sql.Timestamp}</li>
     * </ul>
     *
     * <p>All other arguments are applied as is.</p>
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
     * <p>Executes a query.</p>
     *
     * <p>Arguments are applied as described for
     * {@link #executeQuery(PreparedStatement, Map)}, or transformed as
     * specified by {@link #insert(Class)} and {@link #update(Class)}.</p>
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
            var argument = arguments.get(parameter);

            Object value;
            if (argument instanceof Enum<?>) {
                value = argument.toString();
            } else if (argument instanceof Date date) {
                value = date.getTime();
            } else if (argument instanceof LocalDate localDate) {
                value = java.sql.Date.valueOf(localDate);
            } else if (argument instanceof LocalTime localTime) {
                value = java.sql.Time.valueOf(localTime);
            } else if (argument instanceof Instant instant) {
                value = java.sql.Timestamp.from(instant);
            } else {
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
