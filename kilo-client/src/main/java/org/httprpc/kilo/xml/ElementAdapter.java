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
 * interface. The reserved "." key refers to the element itself. Keys begining
 * with "@" represent attributes. Keys ending with "*" represent multiple
 * elements. All other keys represent individual elements.
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

    private static final String SELF_REFERENCE = ".";

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
     * <li>If the key is equal to ".", the element's text content will be
     * returned.</li>
     * <li>If the key refers to an attribute, the attribute's value (if any)
     * will be returned.</li>
     * <li>If the key refers to multiple elements, a {@link List}
     * implementation that recursively adapts all matching sub-elements will be
     * returned.</li>
     * <li>Otherwise, an adapter for the single matching sub-element (if any)
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

        return get(key.toString());
    }

    private Object get(String key) {
        if (key.equals(SELF_REFERENCE)) {
            return element.getTextContent();
        } else if (isAttribute(key)) {
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

            if (n == 0) {
                return null;
            } else if (n == 1) {
                return new ElementAdapter((Element)nodeList.item(0));
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * <p>Updates a value associated with the source element.</p>
     *
     * <ul>
     * <li>If the key is equal to ".", the element's text content will be
     * replaced with the string representation of the provided value.</li>
     * <li>If the key refers to an attribute, the attribute's value (if any)
     * will be replaced with the string representation of the provided
     * value.</li>
     * <li>If the key refers to multiple elements, the provided value must be
     * an instance of {@link List}. Any matching sub-elements will be replaced
     * with new elements corresponding to the contents of the list.</li>
     * <li>Otherwise, any matching sub-elements will be replaced by a single
     * new element corresponding to the provided value.</li>
     * </ul>
     *
     * <p>For {@link Map} values, new elements will be recursively populated
     * based on the map's contents. For all other types, the element's text
     * content will be set to the string representation of the provided
     * value.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public Object put(String key, Object value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException();
        }

        if (key.equals(SELF_REFERENCE)) {
            element.setTextContent(value.toString());
        } else if (isAttribute(key)) {
            element.setAttribute(getAttributeName(key), value.toString());
        } else if (isList(key)) {
            if (!(value instanceof List<?> values)) {
                throw new IllegalArgumentException();
            }

            var tagName = getListTagName(key);

            remove(tagName);

            addAll(tagName, values);
        } else {
            remove(key);

            add(key, value);
        }

        return null;
    }

    private void addAll(String tagName, List<?> values) {
        for (var value : values) {
            add(tagName, value);
        }
    }

    private void add(String tagName, Object value) {
        var document = element.getOwnerDocument();

        var element = document.createElement(tagName);

        if (value instanceof Map<?, ?> map) {
            var elementAdapter = new ElementAdapter(element);

            for (var entry : map.entrySet()) {
                var key = entry.getKey();

                if (key == null) {
                    throw new IllegalArgumentException();
                }

                elementAdapter.put(key.toString(), entry.getValue());
            }
        } else {
            element.setTextContent(value.toString());
        }

        this.element.appendChild(element);
    }

    /**
     * <p>Removes a value associated with the source element.</p>
     *
     * <ul>
     * <li>If the key refers to an attribute, the attribute (if any) will be
     * removed.</li>
     * <li>Otherwise, any matching sub-elements will be removed.</li>
     * </ul>
     *
     * <p>Self reference and multi-element keys are not supported.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public Object remove(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        remove(key.toString());

        return null;
    }

    private void remove(String key) {
        if (key.equals(SELF_REFERENCE) || isList(key)) {
            throw new UnsupportedOperationException();
        }

        if (isAttribute(key)) {
            element.removeAttribute(getAttributeName(key));
        } else {
            var nodeList = element.getElementsByTagName(key);

            var n = nodeList.getLength();

            for (var i = n - 1; i >= 0; i--) {
                element.removeChild(nodeList.item(i));
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

        return containsKey(key.toString());
    }

    private boolean containsKey(String key) {
        if (key.equals(SELF_REFERENCE) || isList(key)) {
            return true;
        } else if (isAttribute(key)) {
            return element.hasAttribute(getAttributeName(key));
        } else {
            return element.getElementsByTagName(key).getLength() > 0;
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
