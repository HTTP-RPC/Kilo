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

package org.httprpc.kilo.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for decoders.
 *
 * @param <T>
 * The type of value produced by the decoder.
 */
public abstract class Decoder<T> {
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * Represents an "end of file" character.
     */
    protected static final int EOF = -1;

    /**
     * Returns the character set to use when decoding from an input stream.
     *
     * @return
     * The input stream's character set.
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets the character set to use when decoding from an input stream.
     *
     * @param charset
     * The input stream's character set.
     */
    public void setCharset(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException();
        }

        this.charset = charset;
    }

    /**
     * Reads a value from an input stream.
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
    public T read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }

        return read(new InputStreamReader(inputStream, charset));
    }

    /**
     * Reads a value from a character stream.
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
    public abstract T read(Reader reader) throws IOException;
}
