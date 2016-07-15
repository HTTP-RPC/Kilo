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

import org.bson.Document;
import org.httprpc.RPC;
import org.httprpc.Template;
import org.httprpc.WebService;
import org.httprpc.util.IteratorAdapter;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

/**
 * Restaurant service.
 */
public class RestaurantService extends WebService {
    /**
     * Retrieves a list of restaurants in a given zip code.
     *
     * @param zipCode
     * The zip code to search for.
     *
     * @return
     * A list of restaurants in the given zip code.
     */
    @RPC(method="GET")
    @Template(name="restaurants.html", mimeType="text/html")
    public IteratorAdapter getRestaurants(String zipCode) {
        MongoDatabase db = MongoClientManager.getMongoClient().getDatabase("test");
        FindIterable<Document> iterable = db.getCollection("restaurants").find(new Document("address.zipcode", zipCode));

        return new IteratorAdapter(iterable.iterator());
    }
}
