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
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

/**
 * CSV decoder.
 */
public class CSVDecoder {
    /**
     * CSV cursor.
     */
    public static class Cursor implements Iterable<Map<String, Object>>, AutoCloseable {
        @Override
        public Iterator<Map<String, Object>> iterator() {
            // TODO
            return null;
        }

        @Override
        public void close() throws IOException {
            // TODO
        }
    }

    /**
     * Reads a collection of values from an input stream.
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
        // TODO
        return null;
    }

    /**
     * Reads a collection of values from a character stream.
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
