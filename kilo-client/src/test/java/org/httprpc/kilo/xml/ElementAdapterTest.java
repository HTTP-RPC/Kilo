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
    public void testElementAdapter1() throws Exception {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        var documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document;
        try (var inputStream = getClass().getResourceAsStream("test.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        testElementAdapter(new ElementAdapter(document.getDocumentElement()));
    }

    @Test
    public void testElementAdapter2() throws Exception {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        var documentBuilder = documentBuilderFactory.newDocumentBuilder();
        var document = documentBuilder.newDocument();

        var elementAdapter = new ElementAdapter(document.createElement("root"));

        elementAdapter.putAll(mapOf(
            entry("@a", "A"),
            entry("map", mapOf(
                entry("@b", "B"),
                entry("b", "two"),
                entry("list", mapOf(
                    entry("@c", "C"),
                    entry("item*", listOf(
                        "abc",
                        "déf",
                        "ghi"
                    ))
                ))
            ))
        ));

        testElementAdapter(elementAdapter);

        elementAdapter.remove("@a");

        assertFalse(elementAdapter.containsKey("@a"));

        elementAdapter.remove("map");

        assertFalse(elementAdapter.containsKey("map"));
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

        assertEquals(listOf(
            "abc",
            "déf",
            "ghi"
        ), items.stream().map(Object::toString).toList());

        assertTrue(list.containsKey("xyz*"));

        var xyz = (List<?>)list.get("xyz*");

        assertTrue(xyz.isEmpty());
    }
}
