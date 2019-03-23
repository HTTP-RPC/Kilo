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

package org.httprpc.test.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.httprpc.RequestMethod;
import org.httprpc.ResourcePath;
import org.httprpc.WebService;
import org.httprpc.io.CSVDecoder;
import org.httprpc.sql.Parameters;

/**
 * Bulk upload service.
 */
@WebServlet(urlPatterns={"/bulk-upload/*"}, loadOnStartup=1)
public class BulkUploadService extends WebService {
    private static final long serialVersionUID = 0;

    private static final String DB_URL = "jdbc:mysql://vm.local:3306/test?user=root&password=password&serverTimezone=UTC&useSSL=false&rewriteBatchedStatements=true";

    private static final String INSERT_SQL = "INSERT INTO bulk_upload_test ("
        + "text1, text2, number1, number2, number3) VALUES ("
        + ":text1, :text2, :number1, :number2, :number3)";

    private static final int BATCH_SIZE = 25000;

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ServletException(exception);
        }
    }

    @RequestMethod("POST")
    @ResourcePath("upload")
    public void upload() throws SQLException, IOException {
        CSVDecoder csvDecoder = new CSVDecoder();

        Iterable<Map<String, String>> cursor = csvDecoder.read(getRequest().getInputStream());

        Parameters parameters = Parameters.parse(INSERT_SQL);

        try (Connection connection = DriverManager.getConnection(DB_URL);
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            for (Map<String, String> row : cursor) {
                parameters.apply(statement, row);
                statement.executeUpdate();
            }
        }
    }

    @RequestMethod("POST")
    @ResourcePath("upload-batch")
    public void uploadBatch() throws SQLException, IOException {
        CSVDecoder csvDecoder = new CSVDecoder();

        Iterable<Map<String, String>> cursor = csvDecoder.read(getRequest().getInputStream());

        Parameters parameters = Parameters.parse(INSERT_SQL);

        int i = 0;

        try (Connection connection = DriverManager.getConnection(DB_URL);
            PreparedStatement statement = connection.prepareStatement(parameters.getSQL())) {
            for (Map<String, String> row : cursor) {
                parameters.apply(statement, row);
                statement.addBatch();

                if (++i % BATCH_SIZE == 0) {
                    statement.executeBatch();
                }
            }

            statement.executeBatch();
        }
    }
}
