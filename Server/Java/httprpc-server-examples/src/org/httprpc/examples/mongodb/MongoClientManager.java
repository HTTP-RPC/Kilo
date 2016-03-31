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
