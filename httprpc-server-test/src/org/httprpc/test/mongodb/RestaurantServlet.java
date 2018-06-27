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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;
import org.httprpc.util.IteratorAdapter;
import org.jtemplate.TemplateEncoder;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * Restaurant service.
 */
@WebServlet(urlPatterns={"/restaurants.csv", "/restaurants.html", "/restaurants.json", "/restaurants.xml"}, loadOnStartup=1)
public class RestaurantServlet extends HttpServlet {
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String name = request.getServletPath().substring(1);

        response.setContentType(String.format("%s;charset=UTF-8", getServletContext().getMimeType(name)));

        TemplateEncoder templateEncoder = new TemplateEncoder(getClass().getResource(String.format("%s.txt", name)));

        templateEncoder.setBaseName(getClass().getName());

        MongoDatabase db = mongoClient.getDatabase("test");

        FindIterable<Document> iterable = db.getCollection("restaurants").find(new Document("address.zipcode", request.getParameter("zipCode")));

        try (MongoCursor<Document> cursor = iterable.iterator()) {
            templateEncoder.writeValue(new IteratorAdapter(cursor), response.getOutputStream(), request.getLocale());
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }
}
