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
import java.io.InputStream;

/**
 * Represents a response handler.
 */
public interface ResponseHandler {
    /**
     * Decodes a response from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @param contentType
     * The content type, or {@code null} if the content type is not known.
     *
     * @return
     * The decoded body content.
     *
     * @throws IOException
     * If an exception occurs.
     */
    Object decodeResponse(InputStream inputStream, String contentType) throws IOException;
}
