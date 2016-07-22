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

package org.httprpc;

import java.io.InputStream;

/**
 * Interface representing a decoder.
 *
 * @param <V>
 * The type of value read by the decoder.
 */
public interface Decoder<V> {
    /**
     * Reads a value from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * The decoded value.
     */
    public V readValue(InputStream inputStream);
}
