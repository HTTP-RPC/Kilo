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

class SchemaElementAdapter implements SchemaElement {
    private String tableName;
    private String columnName;

    private String alias = null;
    private SortOrder sortOrder = null;
    private boolean required = true;

    SchemaElementAdapter(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    SchemaElementAdapter(String tableName, String columnName, String alias) {
        this(tableName, columnName);

        this.alias = alias;
    }

    SchemaElementAdapter(String tableName, String columnName, SortOrder sortOrder) {
        this(tableName, columnName);

        this.sortOrder = sortOrder;
    }

    SchemaElementAdapter(String tableName, String columnName, boolean required) {
        this(tableName, columnName);

        this.required = required;
    }

    @Override
    public String getTableName() {
        return tableName;
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

    @Override
    public boolean isRequired() {
        return required;
    }
}
