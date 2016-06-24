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

package org.httprpc.examples.mongodb;

import java.util.Iterator;

import org.httprpc.util.IteratorAdapter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class RestaurantServiceTest {
    @BeforeClass
    public static void startup() {
        new MongoClientManager().contextInitialized(null);
    }

    @AfterClass
    public static void shutdown() {
        new MongoClientManager().contextDestroyed(null);
    }

    @Test
    public void testRestaurantSearch() throws Exception {
        RestaurantService service = new RestaurantService();

        int i = 0;

        try (IteratorAdapter results = service.getRestaurants("10075")) {
            Iterator<?> iterator = results.iterator();

            while (iterator.hasNext()) {
                iterator.next();
                i++;
            }
        }

        Assert.assertEquals(99, i);
    }
}
