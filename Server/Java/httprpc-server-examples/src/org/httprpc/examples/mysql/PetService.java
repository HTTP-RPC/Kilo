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

package org.httprpc.examples.mysql;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.httprpc.RPC;
import org.httprpc.Template;
import org.httprpc.WebService;
import org.httprpc.sql.Parameters;
import org.httprpc.sql.ResultSetAdapter;

/**
 * Pet service.
 */
public class PetService extends WebService {
    private static final String DB_URL = "jdbc:mysql://db.local:3306/menagerie?user=root&password=password";

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Retrieves a list of pets belonging to a given owner.
     *
     * @param owner
     * The pet owner to search for.
     *
     * @return
     * A list of pets belonging to the given owner.
     */
    @RPC(method="GET")
    @Template(name="pets.html", contentType="text/html")
    public ResultSetAdapter getPets(String owner) throws SQLException {
        Parameters parameters = Parameters.parse("select name, species, sex, birth from pet where owner = :owner");
        PreparedStatement statement = DriverManager.getConnection(DB_URL).prepareStatement(parameters.getSQL());

        parameters.apply(statement, mapOf(entry("owner", owner)));

        return new ResultSetAdapter(statement.executeQuery());
    }
}
