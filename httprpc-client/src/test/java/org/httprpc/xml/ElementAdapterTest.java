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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ElementAdapterTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testElementAdapter() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document;
        try (InputStream inputStream = getClass().getResourceAsStream("test.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        ElementAdapter elementAdapter = new ElementAdapter(document.getDocumentElement());

        assertEquals("A", elementAdapter.get("@a"));

        Map<String, ?> map = (Map<String, ?>)elementAdapter.get("map");

        assertEquals("B", map.get("@b"));
        assertEquals("two", map.get("b").toString());

        Map<String, ?> list = (Map<String, ?>)map.get("list");

        assertEquals("C", list.get("@c"));

        List<?> items = (List<?>)list.get("item*");

        assertEquals(3, items.size());

        Map<String, ?> item1 = (Map<String, ?>)items.get(0);

        assertEquals("1", item1.get("@d"));
        assertEquals("abc", item1.toString());

        Map<String, ?> item2 = (Map<String, ?>)items.get(1);

        assertEquals("2", item2.get("@d"));
        assertEquals("déf", item2.toString());

        Map<String, ?> item3 = (Map<String, ?>)items.get(2);

        assertEquals("3", item3.get("@d"));
        assertEquals("ghi", item3.toString());

        // TODO Test interface binding
    }
}
