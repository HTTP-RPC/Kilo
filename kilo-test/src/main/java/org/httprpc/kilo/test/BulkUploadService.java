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

package org.httprpc.kilo.test;

import jakarta.servlet.annotation.WebServlet;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.CSVDecoder;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.sql.QueryBuilder;

import java.io.IOException;
import java.sql.SQLException;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

@WebServlet(urlPatterns = {"/bulk-upload/*"}, loadOnStartup = 1)
public class BulkUploadService extends AbstractDatabaseService {
    private static final int BATCH_SIZE = 5000;

    @RequestMethod("POST")
    @ResourcePath("json")
    public int uploadJSON(Void body) throws IOException, SQLException {
        var queryBuilder = QueryBuilder.insert(Row.class);

        var i = 0;

        try (var statement = queryBuilder.prepare(getConnection())) {
            var jsonDecoder = new JSONDecoder();

            var reader = getRequest().getReader();

            for (var map : jsonDecoder.readAll(reader)) {
                var row = BeanAdapter.coerce(map, Row.class);

                queryBuilder.addBatch(statement, new BeanAdapter(row));

                if (++i % BATCH_SIZE == 0) {
                    statement.executeBatch();
                }
            }

            statement.executeBatch();
        }

        return i;
    }

    @RequestMethod("POST")
    @ResourcePath("csv")
    public int uploadCSV(Void body) throws IOException, SQLException {
        var queryBuilder = QueryBuilder.insert(Row.class);

        var i = 0;

        try (var statement = queryBuilder.prepare(getConnection())) {
            var csvDecoder = new CSVDecoder();

            var reader = getRequest().getReader();

            var keys = csvDecoder.read(reader);

            for (var iterator : mapAll(csvDecoder.readAll(reader), Iterable::iterator)) {
                var row = BeanAdapter.coerce(mapOf(mapAll(keys, key -> entry(key, iterator.next()))), Row.class);

                queryBuilder.addBatch(statement, new BeanAdapter(row));

                if (++i % BATCH_SIZE == 0) {
                    statement.executeBatch();
                }
            }

            statement.executeBatch();
        }

        return i;
    }
}
