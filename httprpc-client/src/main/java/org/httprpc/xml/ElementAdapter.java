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
        private NodeList nodeList;

        public NodeListAdapter(NodeList nodeList) {
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

    @Override
    public Object get(Object key) {
        String name = key.toString();

        Object value;
        if (name.startsWith(ATTRIBUTE_PREFIX)) {
            name = name.substring(ATTRIBUTE_PREFIX.length());

            if (element.hasAttribute(name)) {
                value = element.getAttribute(name);
            } else {
                value = null;
            }
        } else {
            if (name.endsWith(LIST_SUFFIX)) {
                name = name.substring(0, name.length() - LIST_SUFFIX.length());

                value = new NodeListAdapter(element.getElementsByTagName(name));
            } else {
                NodeList nodeList = element.getElementsByTagName(name);

                if (nodeList.getLength() > 0) {
                    value = new ElementAdapter((Element)nodeList.item(0));
                } else {
                    value = null;
                }
            }
        }

        return value;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return element.getTextContent();
    }
}
