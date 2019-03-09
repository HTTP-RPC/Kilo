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

import org.httprpc.sql.Parameters;
import org.junit.Assert;
import org.junit.Test;

public class ParametersTest {
    @Test
    public void testParameters() {
        Parameters parameters = Parameters.parse("select * from xyz where foo = :foo and bar = :bar");

        Assert.assertEquals("select * from xyz where foo = ? and bar = ?", parameters.getSQL());
    }

    @Test
    public void testColon() {
        Parameters parameters = Parameters.parse("select * from xyz where foo = 'a:b:c'");

        Assert.assertEquals("select * from xyz where foo = 'a:b:c'", parameters.getSQL());
    }

    @Test
    public void testDoubleColon() {
        Parameters parameters = Parameters.parse("select 'ab:c'::varchar(16) as abc");

        Assert.assertEquals("select 'ab:c'::varchar(16) as abc", parameters.getSQL());
    }
}
