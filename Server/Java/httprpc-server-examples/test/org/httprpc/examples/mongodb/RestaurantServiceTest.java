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

        try (IteratorAdapter results = service.searchRestaurants("10075")) {
            Iterator<?> iterator = results.iterator();

            while (iterator.hasNext()) {
                iterator.next();
                i++;
            }
        }

        Assert.assertEquals(99, i);
    }
}
