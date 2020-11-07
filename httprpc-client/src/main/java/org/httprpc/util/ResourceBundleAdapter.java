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

package org.httprpc.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * {@link Map} adapter for resource bundles.
 */
public class ResourceBundleAdapter extends AbstractMap<String, Object> {
    private ResourceBundle resourceBundle;

    public ResourceBundleAdapter(ResourceBundle resourceBundle) {
        if (resourceBundle == null) {
            throw new IllegalArgumentException();
        }

        this.resourceBundle = resourceBundle;
    }

    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }

        return resourceBundle.getObject(key.toString());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
