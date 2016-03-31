// Copyright © 2016 Greg Brown. All rights reserved.

package org.httprpc.examples.mongodb;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.mongodb.MongoClient;

/**
 * Mongo client manager.
 */
public class MongoClientManager implements ServletContextListener {
    private static MongoClient mongoClient = null;

    /**
     * Returns the Mongo client instance.
     *
     * @return
     * The Mongo client instance.
     */
    public static MongoClient getMongoClient() {
        return mongoClient;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        mongoClient = new MongoClient("db.local");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        mongoClient.close();
    }
}
