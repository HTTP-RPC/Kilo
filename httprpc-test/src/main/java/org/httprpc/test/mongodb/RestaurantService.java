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

package org.httprpc.test.mongodb;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.bson.Document;
import org.httprpc.WebService;
import org.httprpc.CSVEncoder;
import org.httprpc.JSONEncoder;
import org.httprpc.RequestMethod;
import org.httprpc.Response;
import org.jtemplate.TemplateEncoder;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * Restaurant service.
 */
@WebServlet(urlPatterns={"/restaurants/*"}, loadOnStartup=1)
public class RestaurantService extends WebService {
    private static final long serialVersionUID = 0;

    private MongoClient mongoClient = null;

    @Override
    public void init() throws ServletException {
        super.init();

        mongoClient = new MongoClient("db.local");
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
        }

        super.destroy();
    }

    @RequestMethod("GET")
    @Response("[{string: any}]")
    public void getRestaurants(String zipCode, String format) throws IOException {
        MongoDatabase db = mongoClient.getDatabase("test");

        FindIterable<Document> iterable = db.getCollection("restaurants").find(new Document("address.zipcode", zipCode));

        try (MongoCursor<Document> cursor = iterable.iterator()) {
            Iterable<Document> cursorAdapter = new Iterable<Document>() {
                @Override
                public Iterator<Document> iterator() {
                    return cursor;
                }
            };

            if (format == null || format.equals("json")) {
                getResponse().setContentType("application/json");

                JSONEncoder jsonEncoder = new JSONEncoder();

                jsonEncoder.writeValue(cursorAdapter, getResponse().getOutputStream());
            } else if (format.equals("csv")) {
                getResponse().setContentType("text/csv");

                CSVEncoder csvEncoder = new CSVEncoder(Arrays.asList("name", "address.building", "address.street", "borough", "cuisine"));

                csvEncoder.writeValues(cursorAdapter, getResponse().getOutputStream());
            } else if (format.equals("html")){
                getResponse().setContentType("text/html;charset=UTF-8");

                TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource("restaurants.html"));

                templateEncoder.setBaseName(getClass().getPackage().getName() + ".restaurants");
                templateEncoder.writeValue(cursorAdapter, getResponse().getOutputStream(), getRequest().getLocale());
            } else {
                throw new UnsupportedOperationException();
            }
        } finally {
            getResponse().flushBuffer();
        }
    }
}
