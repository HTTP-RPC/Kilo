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

package org.httprpc.kilo.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Provides access to the contents of a resource bundle via the {@link Map}
 * interface.
 */
public class ResourceBundleAdapter extends AbstractMap<String, String> {
    private ResourceBundle resourceBundle;

    /**
     * Constructs a new resource bundle adapter.
     *
     * @param resourceBundle
     * The source resource bundle.
     */
    public ResourceBundleAdapter(ResourceBundle resourceBundle) {
        if (resourceBundle == null) {
            throw new IllegalArgumentException();
        }

        this.resourceBundle = resourceBundle;
    }

    /**
     * Retrieves a localized resource value.
     * {@inheritDoc}
     */
    @Override
    public String get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        try {
            return resourceBundle.getString(key.toString());
        } catch (MissingResourceException exception) {
            return null;
        }
    }

    /**
     * Determines if the source resource bundle contains a given key.
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        return resourceBundle.containsKey(key.toString());
    }

    /**
     * Enumerates the source resource bundle's localized values.
     * {@inheritDoc}
     */
    @Override
    public Set<Entry<String, String>> entrySet() {
        return new AbstractSet<>() {
            Set<String> keySet = resourceBundle.keySet();

            @Override
            public int size() {
                return keySet.size();
            }

            @Override
            public Iterator<Entry<String, String>> iterator() {
                return new Iterator<>() {
                    Iterator<String> keyIterator = keySet.iterator();

                    @Override
                    public boolean hasNext() {
                        return keyIterator.hasNext();
                    }

                    @Override
                    public Entry<String, String> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }

                        var key = keyIterator.next();
                        var value = get(key);

                        return new SimpleImmutableEntry<>(key, value);
                    }
                };
            }
        };
    }
}
