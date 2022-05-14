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

package org.httprpc.xml;

import org.httprpc.beans.BeanAdapter;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElementAdapterTest {
    @Test
    public void testElementAdapter() throws Exception {
        testElementAdapter(false, "test.xml");
    }

    @Test
    public void testElementAdapterNamespaceAware() throws Exception {
        testElementAdapter(true, "test-ns.xml");
    }

    private void testElementAdapter(boolean namespaceAware, String fileName) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);
        documentBuilderFactory.setNamespaceAware(namespaceAware);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document;
        try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
            document = documentBuilder.parse(inputStream);
        }

        ElementAdapter elementAdapter = new ElementAdapter(document.getDocumentElement(), namespaceAware);

        testUntypedAccess(elementAdapter, namespaceAware);
        testTypedAccess(elementAdapter, namespaceAware);
    }

    @SuppressWarnings("unchecked")
    private void testUntypedAccess(ElementAdapter elementAdapter, boolean namespaceAware) {
        assertTrue(elementAdapter.containsKey("@a"));
        assertFalse(elementAdapter.containsKey("@b"));

        assertEquals("A", elementAdapter.get("@a"));

        assertTrue(elementAdapter.containsKey("map"));

        Map<String, ?> map = (Map<String, ?>)elementAdapter.get("map");

        assertEquals(namespaceAware, map.containsKey(":"));

        if (namespaceAware) {
            assertEquals("x", map.get(":"));
        } else {
            assertNull(map.get(":"));
        }

        assertTrue(map.containsKey("@b"));
        assertFalse(map.containsKey("@c"));

        assertEquals("B", map.get("@b"));
        assertEquals("two", map.get("b").toString());

        assertTrue(map.containsKey("list"));

        Map<String, ?> list = (Map<String, ?>)map.get("list");

        if (namespaceAware) {
            assertEquals("y", list.get(":"));
        } else {
            assertNull(list.get(":"));
        }

        assertEquals("C", list.get("@c"));

        assertTrue(list.containsKey("item*"));

        List<?> items = (List<?>)list.get("item*");

        assertEquals(3, items.size());

        Map<String, ?> item1 = (Map<String, ?>)items.get(0);

        if (namespaceAware) {
            assertEquals("z", item1.get(":"));
        } else {
            assertNull(item1.get(":"));
        }

        assertEquals("1", item1.get("@d"));
        assertEquals("abc", item1.toString());

        Map<String, ?> item2 = (Map<String, ?>)items.get(1);

        assertEquals("2", item2.get("@d"));
        assertEquals("déf", item2.toString());

        Map<String, ?> item3 = (Map<String, ?>)items.get(2);

        assertEquals("3", item3.get("@d"));
        assertEquals("ghi", item3.toString());

        assertTrue(list.containsKey("xyz*"));

        List<?> xyz = (List<?>)list.get("xyz*");

        assertTrue(xyz.isEmpty());
    }

    private void testTypedAccess(ElementAdapter elementAdapter, boolean namespaceAware) {
        TestInterface testInterface = BeanAdapter.coerce(elementAdapter, TestInterface.class);

        assertEquals("A", testInterface.getA());

        TestInterface.MapInterface map = testInterface.getMap();

        if (namespaceAware) {
            assertEquals("x", map.getNamespaceURI());
        } else {
            assertNull(map.getNamespaceURI());
        }

        assertEquals("B", map.getB1());
        assertEquals("two", map.getB2());

        TestInterface.ListInterface list = map.getList();

        if (namespaceAware) {
            assertEquals("y", list.getNamespaceURI());
        } else {
            assertNull(map.getNamespaceURI());
        }

        assertEquals("C", list.getC());

        List<String> stringItems = list.getStringItems();

        assertEquals(3, stringItems.size());

        assertEquals("abc", stringItems.get(0));
        assertEquals("déf", stringItems.get(1));
        assertEquals("ghi", stringItems.get(2));

        List<Map<String, Object>> mapItems = list.getMapItems();

        assertEquals(3, mapItems.size());

        assertEquals("1", mapItems.get(0).get("@d"));
        assertEquals("2", mapItems.get(1).get("@d"));
        assertEquals("3", mapItems.get(2).get("@d"));
    }
}
