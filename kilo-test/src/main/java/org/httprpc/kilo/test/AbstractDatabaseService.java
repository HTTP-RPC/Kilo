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

import org.httprpc.kilo.WebService;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractDatabaseService extends WebService {
    protected Connection openConnection(String dataSourceName) throws SQLException {
        try {
            return getDataSource(dataSourceName).getConnection();
        } catch (NamingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private DataSource getDataSource(String name) throws NamingException {
        var initialContext = new InitialContext();

        var environmentContext = (Context)initialContext.lookup("java:comp/env");

        return (DataSource)environmentContext.lookup(name);
    }
}
