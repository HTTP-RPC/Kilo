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
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

/**
 * CSV decoder.
 */
public class CSVDecoder {
    /**
     * CSV cursor.
     */
    public static class Cursor implements Iterable<Map<String, Object>> {
        @Override
        public Iterator<Map<String, Object>> iterator() {
            // TODO
            return null;
        }

        /**
         * Adapts the cursor for typed access.
         *
         * @param <T>
         * The element type.
         *
         * @param elementType
         * The element type.
         *
         * @return
         * An iterable sequence of the given type.
         */
        public <T> Iterable<T> adapt(Class<T> elementType) {
            // TODO
            return null;
        }
    }

    /**
     * Reads a sequence of values from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * A cursor over the values in the input stream.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public Cursor readValues(InputStream inputStream) throws IOException {
        return readValues(new InputStreamReader(inputStream, Charset.forName("ISO-8859-1")));
    }

    /**
     * Reads a sequence of values from a character stream.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * A cursor over the values in the input stream.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public Cursor readValues(Reader reader) throws IOException {
        // TODO
        return null;
    }
}
