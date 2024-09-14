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
import org.httprpc.kilo.sql.QueryBuilder;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(urlPatterns = {"/bulk-upload/*"}, loadOnStartup = 1)
public class BulkUploadService extends AbstractDatabaseService {
    public interface Row {
        String getText1();
        String getText2();
        Double getNumber1();
        Double getNumber2();
        Double getNumber3();
    }

    private static final int BATCH_SIZE = 25000;

    @RequestMethod("POST")
    @ResourcePath("upload")
    public void upload(Void body) throws SQLException, IOException {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("insert into bulk_upload_test (text1, text2, number1, number2, number3)");
        queryBuilder.appendLine("values (:text1, :text2, :number1, :number2, :number3)");

        try (var statement = queryBuilder.prepare(getConnection())) {
            var csvDecoder = new CSVDecoder();

            for (var row : csvDecoder.iterate(getRequest().getInputStream())) {
                queryBuilder.executeUpdate(statement, new BeanAdapter(BeanAdapter.coerce(row, Row.class)));
            }
        }
    }

    @RequestMethod("POST")
    @ResourcePath("upload-batch")
    public void uploadBatch(Void body) throws SQLException, IOException {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("insert into bulk_upload_test (text1, text2, number1, number2, number3)");
        queryBuilder.appendLine("values (:text1, :text2, :number1, :number2, :number3)");

        try (var statement = queryBuilder.prepare(getConnection())) {
            var i = 0;

            var csvDecoder = new CSVDecoder();

            for (var row : csvDecoder.iterate(getRequest().getInputStream())) {
                queryBuilder.addBatch(statement, new BeanAdapter(BeanAdapter.coerce(row, Row.class)));

                if (++i % BATCH_SIZE == 0) {
                    statement.executeBatch();
                }
            }

            statement.executeBatch();
        }
    }
}
