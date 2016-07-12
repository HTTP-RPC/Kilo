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

package org.httprpc.template;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Class that presents the contents of a root object and a resource bundle as
 * a single map.
 */
public class ResourceBundleAdapter extends AbstractMap<String, Object> {
    private Map<?, ?> dictionary;
    private ResourceBundle resourceBundle;

    private static final String RESOURCE_PREFIX = "@";

    /**
     * Constructs a new resource bundle adapter.
     *
     * @param root
     * The root object.
     *
     * @param resourceBundle
     * The resource bundle.
     */
    public ResourceBundleAdapter(Object root, ResourceBundle resourceBundle) {
        if (root == null) {
            throw new IllegalArgumentException();
        }

        if (root == resourceBundle) {
            throw new IllegalArgumentException();
        }

        dictionary = (root instanceof Map<?, ?>) ? (Map<?, ?>)root : Collections.singletonMap(".", root);

        this.resourceBundle = resourceBundle;
    }

    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        return get(key.toString());
    }

    private Object get(String key) {
        Object value;
        if (key.startsWith(RESOURCE_PREFIX)) {
            try {
                value = resourceBundle.getString(key.substring(RESOURCE_PREFIX.length()));
            } catch (MissingResourceException exception) {
                value = key;
            }
        } else {
            value = dictionary.get(key);
        }

        return value;
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
