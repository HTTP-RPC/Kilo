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

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Provides read-only access to the contents of an XML DOM element via the
 * {@link Map} interface.
 */
public class ElementAdapter extends AbstractMap<String, Object> {
    // Node list adapter
    private static class NodeListAdapter extends AbstractList<ElementAdapter> {
        NodeList nodeList;

        NodeListAdapter(NodeList nodeList) {
            this.nodeList = nodeList;
        }

        @Override
        public ElementAdapter get(int i) {
            return new ElementAdapter((Element)nodeList.item(i));
        }

        @Override
        public int size() {
            return nodeList.getLength();
        }
    }

    private Element element;

    private static final String ATTRIBUTE_PREFIX = "@";
    private static final String LIST_SUFFIX = "*";

    /**
     * Constructs a new element adapter.
     *
     * @param element
     * The source element.
     */
    public ElementAdapter(Element element) {
        if (element == null) {
            throw new IllegalArgumentException();
        }

        this.element = element;
    }

    /**
     * <p>Retrieves a value associated with the source element.</p>
     *
     * <p>Attribute values can be obtained by prepending an "@" symbol to the
     * attribute name. Adapters for individual sub-elements can be obtained via
     * the element name. If there is more than one element with a given name,
     * the last matching element will be returned. A list of all elements
     * matching a given name can be obtained by appending an asterisk to the
     * element name.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var name = key.toString();

        Object value;
        if (isAttribute(name)) {
            name = getAttributeName(name);

            if (element.hasAttribute(name)) {
                value = element.getAttribute(name);
            } else {
                value = null;
            }
        } else if (isList(name)) {
            value = new NodeListAdapter(element.getElementsByTagName(getListTagName(name)));
        } else {
            var nodeList = element.getElementsByTagName(name);

            var n = nodeList.getLength();

            if (n > 0) {
                value = new ElementAdapter((Element)nodeList.item(n - 1));
            } else {
                value = null;
            }
        }

        return value;
    }

    /**
     * Determines if the source element contains a given key.
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        var name = key.toString();

        if (isAttribute(name)) {
            return element.hasAttribute(getAttributeName(name));
        } else if (isList(name)) {
            return true;
        } else {
            return element.getElementsByTagName(name).getLength() > 0;
        }
    }

    /**
     * Enumerates the source element's attribute values.
     * {@inheritDoc}
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<>() {
            NamedNodeMap attributes = element.getAttributes();

            @Override
            public int size() {
                return attributes.getLength();
            }

            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < size();
                    }

                    @Override
                    public Entry<String, Object> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }

                        return new Entry<>() {
                            Node node = attributes.item(i++);

                            @Override
                            public String getKey() {
                                return String.format("%s%s", ATTRIBUTE_PREFIX, node.getNodeName());
                            }

                            @Override
                            public Object getValue() {
                                return node.getNodeValue();
                            }

                            @Override
                            public Object setValue(Object value) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        };
    }

    /**
     * Returns the text content of the element.
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return element.getTextContent();
    }

    private static boolean isAttribute(String name) {
        return name.startsWith(ATTRIBUTE_PREFIX);
    }

    private static String getAttributeName(String name) {
        return name.substring(ATTRIBUTE_PREFIX.length());
    }

    private static boolean isList(String name) {
        return name.endsWith(LIST_SUFFIX);
    }

    private static String getListTagName(String name) {
        return name.substring(0, name.length() - LIST_SUFFIX.length());
    }
}
