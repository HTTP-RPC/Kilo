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
import org.httprpc.kilo.io.CSVDecoder;
import org.httprpc.kilo.sql.QueryBuilder;

import java.io.IOException;
import java.sql.SQLException;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.mapOf;

@WebServlet(urlPatterns={"/bulk-upload/*"}, loadOnStartup=1)
public class BulkUploadService extends AbstractDatabaseService {
    private static final int BATCH_SIZE = 25000;

    @RequestMethod("POST")
    @ResourcePath("upload")
    public void upload() throws SQLException, IOException {
        var queryBuilder = QueryBuilder.insertInto("bulk_upload_test").values(mapOf(
            entry("text1", ":text1"),
            entry("text2", ":text2"),
            entry("number1", ":number1"),
            entry("number2", ":number2"),
            entry("number3", ":number3")
        ));

        try (var statement = queryBuilder.prepare(getConnection())) {
            var csvDecoder = new CSVDecoder();

            for (var row : csvDecoder.iterate(getRequest().getInputStream())) {
                queryBuilder.executeUpdate(statement, row);
            }
        }
    }

    @RequestMethod("POST")
    @ResourcePath("upload-batch")
    public void uploadBatch() throws SQLException, IOException {
        var queryBuilder = QueryBuilder.insertInto("bulk_upload_test").values(mapOf(
            entry("text1", ":text1"),
            entry("text2", ":text2"),
            entry("number1", ":number1"),
            entry("number2", ":number2"),
            entry("number3", ":number3")
        ));

        try (var statement = queryBuilder.prepare(getConnection())) {
            var i = 0;

            var csvDecoder = new CSVDecoder();

            for (var row : csvDecoder.iterate(getRequest().getInputStream())) {
                queryBuilder.addBatch(statement, row);

                if (++i % BATCH_SIZE == 0) {
                    statement.executeBatch();
                }
            }

            statement.executeBatch();
        }
    }
}
