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
import org.w3c.dom.NodeList;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to the contents of an XML DOM element via the {@link Map}
 * interface. Keys begining with "@" represent attributes. Keys ending with "*"
 * represent multiple elements. All other keys represent individual elements.
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
     * <ul>
     * <li>If the key refers to an attribute, the attribute's string value (if
     * any) will be returned.</li>
     * <li>If the key refers to multiple elements, a list implementation that
     * recursively adapts all matching sub-elements will be returned.</li>
     * <li>Otherwise, an adapter for the last matching sub-element (if any)
     * will be returned.</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        return getValue(key.toString());
    }

    private Object getValue(String key) {
        if (isAttribute(key)) {
            key = getAttributeName(key);

            if (element.hasAttribute(key)) {
                return element.getAttribute(key);
            } else {
                return null;
            }
        } else if (isList(key)) {
            return new NodeListAdapter(element.getElementsByTagName(getListTagName(key)));
        } else {
            var nodeList = element.getElementsByTagName(key);

            var n = nodeList.getLength();

            if (n > 0) {
                return new ElementAdapter((Element)nodeList.item(n - 1));
            } else {
                return null;
            }
        }
    }

    /**
     * <p>Updates a value associated with the source element.</p>
     *
     * <ul>
     * <li>If the key refers to an attribute, the attribute's value (if any)
     * will be replaced with the string representation of the provided
     * value.</li>
     * <li>If the key refers to multiple elements, any matching sub-elements
     * will be replaced with the contents of the provided list.</li>
     * <li>Otherwise, a new sub-element will be appended. For {@link Map}
     * values, the element will be recursively populated using the map's
     * contents. For all other types, the element's text content will be set to
     * the string representation of the provided value.</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public Object put(String key, Object value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException();
        }

        if (isAttribute(key)) {
            element.setAttribute(getAttributeName(key), value.toString());
        } else if (isList(key)) {
            if (!(value instanceof List<?> list)) {
                throw new IllegalArgumentException();
            }

            removeValue(key);

            var name = getListTagName(key);

            for (var element : list) {
                put(name, element);
            }
        } else {
            var document = element.getOwnerDocument();

            var child = document.createElement(key);

            if (value instanceof Map<?, ?> map) {
                var childAdapter = new ElementAdapter(child);

                for (var entry : map.entrySet()) {
                    var entryKey = entry.getKey();

                    if (entryKey == null) {
                        throw new IllegalArgumentException();
                    }

                    childAdapter.put(entryKey.toString(), entry.getValue());
                }
            } else {
                child.setTextContent(value.toString());
            }

            element.appendChild(child);
        }

        return null;
    }

    /**
     * <p>Removes a value associated with the source element.</p>
     *
     * <ul>
     * <li>If the key refers to an attribute, the attribute will be
     * removed.</li>
     * <li>If the key refers to multiple elements, all matching sub-elements
     * will be removed.</li>
     * <li>Otherwise, the last matching sub-element (if any) will be
     * removed.</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public Object remove(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        removeValue(key.toString());

        return null;
    }

    private void removeValue(String key) {
        if (isAttribute(key)) {
            element.removeAttribute(getAttributeName(key));
        } else if (isList(key)) {
            var nodeList = element.getElementsByTagName(getListTagName(key));

            var n = nodeList.getLength();

            for (var i = n - 1; i >= 0; i--) {
                element.removeChild(nodeList.item(i));
            }
        } else {
            var nodeList = element.getElementsByTagName(key);

            var n = nodeList.getLength();

            if (n > 0) {
                element.removeChild(nodeList.item(n - 1));
            }
        }
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
     * Throws {@link UnsupportedOperationException}.
     * {@inheritDoc}
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the text content of the element.
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return element.getTextContent();
    }

    private static boolean isAttribute(String key) {
        return key.startsWith(ATTRIBUTE_PREFIX);
    }

    private static String getAttributeName(String key) {
        return key.substring(ATTRIBUTE_PREFIX.length());
    }

    private static boolean isList(String key) {
        return key.endsWith(LIST_SUFFIX);
    }

    private static String getListTagName(String key) {
        return key.substring(0, key.length() - LIST_SUFFIX.length());
    }
}
