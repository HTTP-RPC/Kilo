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
import java.util.Map;
import java.util.Set;

/**
 * {@link Map} adapter for XML elements.
 */
public class ElementAdapter extends AbstractMap<String, Object> {
    private static class NodeListAdapter extends AbstractList<ElementAdapter> {
        NodeList nodeList;
        boolean namespaceAware;

        NodeListAdapter(NodeList nodeList, boolean namespaceAware) {
            this.nodeList = nodeList;
            this.namespaceAware = namespaceAware;
        }

        @Override
        public ElementAdapter get(int i) {
            return new ElementAdapter((Element)nodeList.item(i), namespaceAware);
        }

        @Override
        public int size() {
            return nodeList.getLength();
        }
    }

    private Element element;
    private boolean namespaceAware;

    private static final String NAMESPACE_KEY = ":";

    private static final String ATTRIBUTE_PREFIX = "@";
    private static final String LIST_SUFFIX = "*";

    /**
     * Constructs a new element adapter.
     *
     * @param element
     * The source element.
     */
    public ElementAdapter(Element element) {
        this(element, false);
    }

    /**
     * Constructs a new element adapter.
     *
     * @param element
     * The source element.
     *
     * @param namespaceAware
     * Indicates that the element has been parsed by a namespace-aware document
     * builder.
     */
    public ElementAdapter(Element element, boolean namespaceAware) {
        if (element == null) {
            throw new IllegalArgumentException();
        }

        this.element = element;
        this.namespaceAware = namespaceAware;
    }

    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        String name = key.toString();

        Object value;
        if (name.equals(NAMESPACE_KEY)) {
            value = element.getNamespaceURI();
        } else if (isAttribute(name)) {
            name = getAttributeName(name);

            if (element.hasAttribute(name)) {
                value = element.getAttribute(name);
            } else {
                value = null;
            }
        } else if (isList(name)) {
            if (namespaceAware) {
                value = new NodeListAdapter(element.getElementsByTagNameNS("*", getListTagName(name)), namespaceAware);
            } else {
                value = new NodeListAdapter(element.getElementsByTagName(getListTagName(name)), namespaceAware);
            }
        } else {
            NodeList nodeList;
            if (namespaceAware) {
                nodeList = element.getElementsByTagNameNS("*", name);
            } else {
                nodeList = element.getElementsByTagName(name);
            }

            if (nodeList.getLength() > 0) {
                value = new ElementAdapter((Element)nodeList.item(0), namespaceAware);
            } else {
                value = null;
            }
        }

        return value;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        String name = key.toString();

        if (name.equals(NAMESPACE_KEY)) {
            return namespaceAware;
        } else if (isAttribute(name)) {
            return element.hasAttribute(getAttributeName(name));
        } else if (isList(name)) {
            return true;
        } else {
            NodeList nodeList;
            if (namespaceAware) {
                nodeList = element.getElementsByTagNameNS("*", name);
            } else {
                nodeList = element.getElementsByTagName(name);
            }

            return nodeList.getLength() > 0;
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

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
