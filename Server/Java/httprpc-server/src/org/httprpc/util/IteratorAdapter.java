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

import java.util.AbstractList;
import java.util.Iterator;
import java.util.Map;

/**
 * Class that presents the contents of an iterator as an iterable list of
 * values.
 *
 * If the iterator's type implements {@link AutoCloseable}, it will be
 * automatically closed when the adapter is closed.
 */
public class IteratorAdapter extends AbstractList<Object> implements AutoCloseable {
    private Iterator<?> iterator;

    /**
     * Constructs a new iterator adapter.
     *
     * @param iterator
     * The source iterator.
     */
    public IteratorAdapter(Iterator<?> iterator) {
        if (iterator == null) {
            throw new IllegalArgumentException();
        }

        this.iterator = iterator;
    }

    @Override
    public Map<String, Object> get(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Object next() {
                return iterator.next();
            }
        };
    }

    @Override
    public void close() throws Exception {
        if (iterator instanceof AutoCloseable) {
            ((AutoCloseable)iterator).close();
        }
    }

    @Override
    public String toString() {
        return getClass().getName();
    }
}
