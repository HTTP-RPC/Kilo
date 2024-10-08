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

import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.ServicePath;

import java.io.IOException;
import java.util.List;

@ServicePath("catalog")
public interface CatalogServiceProxy {
    @RequestMethod("GET")
    @ResourcePath("items")
    List<Item> getItems() throws IOException;

    @RequestMethod("GET")
    @ResourcePath("items/?")
    ItemDetail getItem(Integer itemID) throws IOException;

    @RequestMethod("POST")
    @ResourcePath("items")
    ItemDetail addItem(ItemDetail item) throws IOException;

    @RequestMethod("PUT")
    @ResourcePath("items/?")
    ItemDetail updateItem(Integer itemID, ItemDetail item) throws IOException;

    @RequestMethod("DELETE")
    @ResourcePath("items/?")
    void deleteItem(Integer itemID) throws IOException;
}
