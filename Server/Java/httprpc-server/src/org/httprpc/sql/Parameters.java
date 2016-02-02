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

import java.io.IOException;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;

/**
 * Class for simplifying execution of prepared statements.
 */
public class Parameters {
    private String sql;
    private LinkedList<String> keys;

    private static final int EOF = -1;

    private Parameters(String sql, LinkedList<String> keys) {
        this.sql = sql;
        this.keys = keys;
    }

    /**
     * Returns the parsed SQL.
     *
     * @return
     * The SQL that was parsed by the call to {@link #parse(Reader)}.
     */
    public String getSQL() {
        return sql;
    }

    /**
     * Applies a set of argument values to a prepared statement.
     *
     * @param statement
     * The prepared statement.
     *
     * @param arguments
     * The argument values that will be applied to the prepared statement.
     *
     * @throws SQLException
     * If an exception occurs while applying the argument values.
     */
    public void apply(PreparedStatement statement, Map<String, Object> arguments) throws SQLException {
        int i = 1;

        for (String key : keys) {
            statement.setObject(i++, arguments.get(key));
        }
    }

    /**
     * Parses a parameterized SQL statement. Parameters are declared using
     * <a href="http://jcp.org/en/jsr/detail?id=317" target="_blank">JPA</a>
     * named parameter syntax.
     *
     * @param sqlReader
     * A reader containing the SQL to parse.
     *
     * @return
     * An instance of {@link Parameters} that can be used to apply a set of
     * argument values to a prepared statement.
     *
     * @throws IOException
     * If an exception occurs while reading the SQL statement.
     */
    public static Parameters parse(Reader sqlReader) throws IOException {
        if (sqlReader == null) {
            throw new IllegalArgumentException();
        }

        LinkedList<String> keys = new LinkedList<>();

        StringBuilder sqlBuilder = new StringBuilder();

        int c = sqlReader.read();

        while (c != EOF) {
            if (c == ':') {
                c = sqlReader.read();

                StringBuilder keyBuilder = new StringBuilder();

                while (c != EOF && Character.isJavaIdentifierPart(c)) {
                    keyBuilder.append((char)c);

                    c = sqlReader.read();
                }

                keys.add(keyBuilder.toString());

                sqlBuilder.append("?");
            } else {
                sqlBuilder.append((char)c);

                c = sqlReader.read();
            }
        }

        return new Parameters(sqlBuilder.toString(), keys);
    }
}
