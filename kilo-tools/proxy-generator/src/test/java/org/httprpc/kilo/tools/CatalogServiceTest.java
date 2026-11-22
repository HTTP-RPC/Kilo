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

package org.httprpc.kilo.tools;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.web.WebServiceProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;

public class CatalogServiceTest {
    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testCatalog() throws IOException {
        var catalogServiceProxy = WebServiceProxy.of(CatalogServiceProxy.class, baseURI);

        var item = BeanAdapter.coerce(mapOf(), ItemDetails.class);

        item.setDescription("abc");
        item.setPrice(150.0);
        item.setSize(Size.MEDIUM);
        item.setColor("red");
        item.setWeight(5.0);

        item = catalogServiceProxy.addItem(item);

        Assertions.assertNotNull(item);

        var itemID = item.getID();

        Assertions.assertNotNull(itemID);

        Assertions.assertEquals("abc", item.getDescription());
        Assertions.assertEquals(150.0, item.getPrice());
        Assertions.assertEquals(Size.MEDIUM, item.getSize());
        Assertions.assertEquals(5.0, item.getWeight());

        Assertions.assertNotNull(item.getCreated());

        item = catalogServiceProxy.getItem(itemID);

        Assertions.assertNotNull(item);

        Assertions.assertEquals(itemID, item.getID());
        Assertions.assertEquals("abc", item.getDescription());
        Assertions.assertEquals(150.0, item.getPrice());
        Assertions.assertEquals(Size.MEDIUM, item.getSize());
        Assertions.assertEquals(5.0, item.getWeight());

        item.setDescription("xyz");
        item.setPrice(300.0);
        item.setSize(Size.LARGE);
        item.setColor("blue");
        item.setWeight(10.0);

        item = catalogServiceProxy.updateItem(item.getID(), item);

        Assertions.assertNotNull(item);

        Assertions.assertEquals(itemID, item.getID());
        Assertions.assertEquals("xyz", item.getDescription());
        Assertions.assertEquals(300.0, item.getPrice());
        Assertions.assertEquals(Size.LARGE, item.getSize());
        Assertions.assertEquals("blue", item.getColor());
        Assertions.assertEquals(10.0, item.getWeight());

        Assertions.assertNotNull(firstOf(filter(catalogServiceProxy.getItems(), result -> result.getID().equals(itemID))));

        catalogServiceProxy.deleteItem(item.getID());

        Assertions.assertNull(firstOf(filter(catalogServiceProxy.getItems(), result -> result.getID().equals(itemID))));
    }
}
