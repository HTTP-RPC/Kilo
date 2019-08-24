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
import java.io.StringReader;
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
     * The parsed SQL.
     */
    public String getSQL() {
        return sql;
    }

    /**
     * Applies the provided argument values to a prepared statement.
     *
     * @param statement
     * The prepared statement.
     *
     * @param arguments
     * The argument values.
     *
     * @throws SQLException
     * If an exception occurs while applying the argument values.
     */
    public void apply(PreparedStatement statement, Map<String, ?> arguments) throws SQLException {
        int i = 1;

        for (String key : keys) {
            statement.setObject(i++, arguments.get(key));
        }
    }

    /**
     * Parses a parameterized SQL statement.
     *
     * @param sql
     * A string containing the SQL to parse.
     *
     * @return
     * An {@link Parameters} instance containing the parsed SQL.
     */
    public static Parameters parse(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException();
        }

        Parameters parameters;
        try (Reader sqlReader = new StringReader(sql)) {
            parameters = parse(sqlReader);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return parameters;
    }

    /**
     * Parses a parameterized SQL statement.
     *
     * @param sqlReader
     * A reader containing the SQL to parse.
     *
     * @return
     * An {@link Parameters} instance containing the parsed SQL.
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

        boolean singleLineComment = false;
        boolean multiLineComment = false;

        boolean quoted = false;

        int c = sqlReader.read();

        while (c != EOF) {
            if (c == '-') {
                sqlBuilder.append((char) c);

                c = sqlReader.read();

                singleLineComment = (c == '-') && !multiLineComment;

                sqlBuilder.append((char) c);

                c = sqlReader.read();
            } else if (c == '\r' || c == '\n') {
                sqlBuilder.append((char) c);

                singleLineComment = false;

                c = sqlReader.read();
            } else if (c == '/') {
                sqlBuilder.append((char)c);

                c = sqlReader.read();

                multiLineComment = (c == '*');

                sqlBuilder.append((char)c);

                c = sqlReader.read();
            } else if (c == '*') {
                sqlBuilder.append((char) c);

                c = sqlReader.read();

                multiLineComment = (c == '/');

                sqlBuilder.append((char) c);

                c = sqlReader.read();
            } else if (singleLineComment || multiLineComment) {
                sqlBuilder.append((char)c);

                c = sqlReader.read();
            } else if (c == ':' && !quoted) {
                c = sqlReader.read();

                if (c == ':') {
                    sqlBuilder.append("::");

                    c = sqlReader.read();
                } else {
                    StringBuilder keyBuilder = new StringBuilder();

                    while (c != EOF && Character.isJavaIdentifierPart(c)) {
                        keyBuilder.append((char)c);

                        c = sqlReader.read();
                    }

                    keys.add(keyBuilder.toString());

                    sqlBuilder.append("?");
                }
            } else {
                if (c == '\'') {
                    quoted = !quoted;
                }

                sqlBuilder.append((char)c);

                c = sqlReader.read();
            }
        }

        return new Parameters(sqlBuilder.toString(), keys);
    }
}
