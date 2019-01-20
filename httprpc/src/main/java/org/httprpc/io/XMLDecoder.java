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

package org.httprpc.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * XML decoder.
 */
public class XMLDecoder {
    /**
     * Reads a list of values from an input stream.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * The decoded values.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public List<Map<String, ?>> read(InputStream inputStream) throws IOException {
        return read(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    }

    /**
     * Reads a list of values from a character stream.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * The decoded values.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public List<Map<String, ?>> read(Reader reader) throws IOException {
        reader = new BufferedReader(reader);

        // TODO Parse input stream contents

        return null;
    }
}
