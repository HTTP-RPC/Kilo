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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ElementAdapterTest {
    private DocumentBuilder documentBuilder;

    @BeforeEach
    public void setUp() throws Exception {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        documentBuilder = documentBuilderFactory.newDocumentBuilder();
    }

    @Test
    public void testElementAdapter1() throws Exception {
        Document document;
        try (var inputStream = getClass().getResourceAsStream("test.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        testElementAdapter(new ElementAdapter(document.getDocumentElement()));
    }

    @Test
    public void testElementAdapter2() {
        var elementAdapter = new ElementAdapter(documentBuilder.newDocument().createElement("root"));

        elementAdapter.putAll(mapOf(
            entry("@a", "A"),
            entry("map", mapOf(
                entry("@b", "B"),
                entry("b", "two"),
                entry("list", mapOf(
                    entry("@c", "C"),
                    entry("item*", listOf(
                        mapOf(
                            entry("@d", 1),
                            entry(".", "abc")
                        ),
                        mapOf(
                            entry("@d", 2),
                            entry(".", "déf")
                        ),
                        mapOf(
                            entry("@d", 3),
                            entry(".", "ghi")
                        )
                    ))
                ))
            ))
        ));

        testElementAdapter(elementAdapter);
    }

    @SuppressWarnings("unchecked")
    private void testElementAdapter(ElementAdapter elementAdapter) {
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
        assertEquals("abc", item1.get("."));

        var item2 = (Map<String, ?>)items.get(1);

        assertEquals("2", item2.get("@d"));
        assertEquals("déf", item2.get("."));

        var item3 = (Map<String, ?>)items.get(2);

        assertEquals("3", item3.get("@d"));
        assertEquals("ghi", item3.get("."));

        assertTrue(list.containsKey("xyz*"));

        var xyz = (List<?>)list.get("xyz*");

        assertTrue(xyz.isEmpty());
    }

    @Test
    public void testSelfReference() {
        var elementAdapter = new ElementAdapter(documentBuilder.newDocument().createElement("root"));

        assertTrue(elementAdapter.containsKey("."));

        elementAdapter.put(".", "abc");

        assertEquals("abc", elementAdapter.toString());

        assertThrows(UnsupportedOperationException.class, () -> elementAdapter.remove("."));
    }

    @Test
    public void testAttribute() {
        var elementAdapter = new ElementAdapter(documentBuilder.newDocument().createElement("root"));

        elementAdapter.put("@e", "123");
        elementAdapter.put("@e", "456");

        assertEquals("456", elementAdapter.get("@e"));

        elementAdapter.remove("@e");

        assertFalse(elementAdapter.containsKey("@e"));
    }

    @Test
    public void testMultipleElements() {
        var elementAdapter = new ElementAdapter(documentBuilder.newDocument().createElement("root"));

        elementAdapter.put("item*", listOf(1, 2, 3));
        elementAdapter.put("item*", listOf(4, 5, 6));

        var items = (List<?>)elementAdapter.get("item*");

        assertEquals(listOf("4", "5", "6"), items.stream().map(Object::toString).toList());

        elementAdapter.put("item*", listOf());

        assertFalse(elementAdapter.containsKey("item"));

        assertThrows(UnsupportedOperationException.class, () -> elementAdapter.remove("item*"));
    }

    @Test
    public void testSingleElement() {
        var elementAdapter = new ElementAdapter(documentBuilder.newDocument().createElement("root"));

        elementAdapter.put("item", 1);
        elementAdapter.put("item", 2);

        assertEquals("2", elementAdapter.get("item").toString());

        elementAdapter.remove("item");

        assertFalse(elementAdapter.containsKey("item"));

        elementAdapter.put("item*", listOf(1, 2, 3));

        assertEquals("3", elementAdapter.get("item").toString());
    }
}
