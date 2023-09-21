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

/**
 * Adapts a schema element.
 */
public class SchemaElementAdapter implements SchemaElement {
    private String columnName;
    private String alias;
    private SortOrder sortOrder;

    /**
     * Constructs a new schema element adapter.
     *
     * @param columnName
     * The column name.
     */
    public SchemaElementAdapter(String columnName) {
        this(columnName, null, null);
    }

    /**
     * Constructs a new schema element adapter.
     *
     * @param columnName
     * The column name.
     *
     * @param alias
     * The schema element's alias.
     */
    public SchemaElementAdapter(String columnName, String alias) {
        this(columnName, alias, null);
    }

    /**
     * Constructs a new schema element adapter.
     *
     * @param columnName
     * The column name.
     *
     * @param sortOrder
     * The schema element's sort order.
     */
    public SchemaElementAdapter(String columnName, SortOrder sortOrder) {
        this(columnName, null, sortOrder);
    }

    private SchemaElementAdapter(String columnName, String alias, SortOrder sortOrder) {
        if (columnName == null) {
            throw new IllegalArgumentException();
        }

        this.columnName = columnName;
        this.alias = alias;
        this.sortOrder = sortOrder;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public SortOrder getSortOrder() {
        return sortOrder;
    }
}
