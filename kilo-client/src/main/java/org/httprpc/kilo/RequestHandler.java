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

package org.httprpc.kilo;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Represents a request handler.
 */
public interface RequestHandler {
    /**
     * Returns the handler's content type.
     *
     * @return
     * The content type produced by the handler.
     */
    String getContentType();

    /**
     * Encodes a request to an output stream.
     *
     * @param body
     * A value representing the body content.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    void encodeRequest(Object body, OutputStream outputStream) throws IOException;
}
