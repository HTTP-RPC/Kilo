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

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface representing a response handler.
 *
 * @param <V>
 * The type of value produced by the handler.
 */
public interface ResponseHandler<V> {
    /**
     * Decodes a value from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @param contentType
     * The content type.
     *
     * @throws IOException
     * If an exception occurs.
     *
     * @return
     * The decoded value.
     */
    public V decode(InputStream inputStream, String contentType) throws IOException;
}
