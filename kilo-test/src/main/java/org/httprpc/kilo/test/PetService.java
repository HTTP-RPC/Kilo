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
import org.httprpc.kilo.Required;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.sql.QueryBuilder;

import java.sql.SQLException;
import java.util.List;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

@WebServlet(urlPatterns = "/pets/*", loadOnStartup = 0)
public class PetService extends AbstractDatabaseService {
    @Override
    protected String getDataSourceName() {
        return DEMO_DB;
    }

    @RequestMethod("GET")
    public List<Pet> getPets(@Required String owner) throws SQLException {
        var queryBuilder = new QueryBuilder();

        queryBuilder.appendLine("select * from pet where owner = :owner order by name");

        try (var statement = queryBuilder.prepare(getConnection());
            var results = queryBuilder.executeQuery(statement, mapOf(
                entry("owner", owner)
            ))) {
            return listOf(mapAll(results, BeanAdapter.toType(Pet.class)));
        }
    }
}
