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

package org.httprpc.kilo.xml;

import org.httprpc.kilo.beans.BeanAdapter;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElementAdapterTest {
    @Test
    public void testElementAdapter() throws Exception {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        var documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document;
        try (var inputStream = getClass().getResourceAsStream("test.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        var elementAdapter = new ElementAdapter(document.getDocumentElement());

        testUntypedAccess(elementAdapter);
        testTypedAccess(elementAdapter);
    }

    @SuppressWarnings("unchecked")
    private void testUntypedAccess(ElementAdapter elementAdapter) {
        assertTrue(elementAdapter.containsKey("@a"));
        assertFalse(elementAdapter.containsKey("@b"));

        assertEquals("A", elementAdapter.get("@a"));

        assertTrue(elementAdapter.containsKey("map"));

        var map = (Map<String, ?>)elementAdapter.get("map");

        assertTrue(map.containsKey("@b"));
        assertFalse(map.containsKey("@c"));

        assertEquals("B", map.get("@b"));
        assertEquals("two", map.get("b").toString());

        assertTrue(map.containsKey("list"));

        var list = (Map<String, ?>)map.get("list");

        assertEquals("C", list.get("@c"));

        assertTrue(list.containsKey("item*"));

        var items = (List<?>)list.get("item*");

        assertEquals(3, items.size());

        var item1 = (Map<String, ?>)items.get(0);

        assertEquals("1", item1.get("@d"));
        assertEquals("abc", item1.toString());

        var item2 = (Map<String, ?>)items.get(1);

        assertEquals("2", item2.get("@d"));
        assertEquals("déf", item2.toString());

        var item3 = (Map<String, ?>)items.get(2);

        assertEquals("3", item3.get("@d"));
        assertEquals("ghi", item3.toString());

        assertTrue(list.containsKey("xyz*"));

        var xyz = (List<?>)list.get("xyz*");

        assertTrue(xyz.isEmpty());
    }

    private void testTypedAccess(ElementAdapter elementAdapter) {
        var testInterface = BeanAdapter.coerce(elementAdapter, TestInterface.class);

        assertEquals("A", testInterface.getA());

        var map = testInterface.getMap();

        assertEquals("B", map.getB1());
        assertEquals("two", map.getB2());

        var list = map.getList();

        assertEquals("C", list.getC());

        var stringItems = list.getStringItems();

        assertEquals(3, stringItems.size());

        assertEquals("abc", stringItems.get(0));
        assertEquals("déf", stringItems.get(1));
        assertEquals("ghi", stringItems.get(2));

        assertEquals(listOf(
            mapOf(entry("@d", "1")),
            mapOf(entry("@d", "2")),
            mapOf(entry("@d", "3"))
        ), list.getMapItems());
    }
}
