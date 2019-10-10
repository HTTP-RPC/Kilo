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

/**
 * Abstract base class for decoders.
 */
public abstract class Decoder {
    private Charset charset;

    /**
     * Constructs a new decoder.
     *
     * @param charset
     * The character set to use when decoding an input stream.
     */
    protected Decoder(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException();
        }

        this.charset = charset;
    }

    /**
     * Reads a value from an input stream.
     *
     * @param <T>
     * The value type.
     *
     * @param inputStream
     * The input stream to read from.
     *
     * @return
     * The decoded value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public <T> T read(InputStream inputStream) throws IOException {
        return read(new InputStreamReader(inputStream, charset));
    }

    /**
     * Reads a value from a character stream.
     *
     * @param <T>
     * The value type.
     *
     * @param reader
     * The character stream to read from.
     *
     * @return
     * The decoded value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public abstract <T> T read(Reader reader) throws IOException;
}
