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

package org.httprpc.kilo.test;

import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class CatalogTest extends AbstractTest {
    @Test
    public void testCatalog() throws IOException {
        var catalogServiceProxy = WebServiceProxy.of(CatalogServiceProxy.class, baseURL, webServiceProxy -> webServiceProxy.setMonitorStream(System.out));

        var item = BeanAdapter.coerce(mapOf(), ItemDetail.class);

        item.setDescription("abc");
        item.setPrice(150.0);
        item.setSize(Size.MEDIUM);
        item.setColor("red");
        item.setWeight(5.0);

        item = catalogServiceProxy.addItem(item);

        assertNotNull(item);

        var itemID = item.getID();

        assertNotNull(itemID);

        assertEquals("abc", item.getDescription());
        assertEquals(150.0, item.getPrice());
        assertEquals(Size.MEDIUM, item.getSize());
        assertEquals(5.0, item.getWeight());

        assertNotNull(item.getCreated());

        item = catalogServiceProxy.getItem(itemID);

        assertNotNull(item);

        assertEquals(itemID, item.getID());
        assertEquals("abc", item.getDescription());
        assertEquals(150.0, item.getPrice());
        assertEquals(Size.MEDIUM, item.getSize());
        assertEquals(5.0, item.getWeight());

        item.setDescription("xyz");
        item.setPrice(300.0);
        item.setSize(Size.LARGE);
        item.setColor("blue");
        item.setWeight(10.0);

        item = catalogServiceProxy.updateItem(item.getID(), item);

        assertNotNull(item);

        assertEquals(itemID, item.getID());
        assertEquals("xyz", item.getDescription());
        assertEquals(300.0, item.getPrice());
        assertEquals(Size.LARGE, item.getSize());
        assertEquals("blue", item.getColor());
        assertEquals(10.0, item.getWeight());

        assertNotNull(catalogServiceProxy.getItems().stream()
            .filter(result -> result.getID().equals(itemID))
            .findAny().orElse(null));

        catalogServiceProxy.deleteItem(item.getID());

        assertNull(catalogServiceProxy.getItems().stream()
            .filter(result -> result.getID().equals(itemID))
            .findAny().orElse(null));
    }
}
