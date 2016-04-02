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

import java.util.Iterator;

import org.httprpc.sql.ResultSetAdapter;
import org.junit.Assert;
import org.junit.Test;

public class PetServiceTest {
    @Test
    public void testPetSearch() throws Exception {
        PetService service = new PetService();

        int i = 0;

        try (ResultSetAdapter results = service.searchPets("Harold")) {
            Iterator<?> iterator = results.iterator();

            while (iterator.hasNext()) {
                iterator.next();
                i++;
            }
        }

        Assert.assertEquals(2, i);
    }
}
