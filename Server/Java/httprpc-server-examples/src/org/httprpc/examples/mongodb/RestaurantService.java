// Copyright © 2016 Greg Brown. All rights reserved.

package org.httprpc.examples.mongodb;

import org.bson.Document;
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
     * Searches for restaurants.
     *
     * @param zipCode
     * The zip code to search for.
     *
     * @return
     * A list of restaurants matching the given zip code.
     */
    @Template("restaurant_search.html")
    public IteratorAdapter searchRestaurants(String zipCode) {
        MongoDatabase db = MongoClientManager.getMongoClient().getDatabase("test");
        FindIterable<Document> iterable = db.getCollection("restaurants").find(new Document("address.zipcode", zipCode));

        return new IteratorAdapter(iterable.iterator());
    }
}
