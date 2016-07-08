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

package org.httprpc.serialization;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Interface representing a serializer.
 */
public interface Serializer {
    /**
     * Returns the serializer's content type.
     *
     * @return
     * The serializer's content type.
     */
    public String getContentType();

    /**
     * Writes a value to an output stream.
     *
     * @param writer
     * The writer to which the value will be written.
     *
     * @param value
     * The value to write.
     *
     * @throws IOException
     * If an exception occurs while writing the value.
     */
    public void writeValue(PrintWriter writer, Object value) throws IOException;
}
