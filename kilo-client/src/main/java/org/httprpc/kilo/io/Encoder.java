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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for encoders.
 *
 * @param <T>
 * The type of value consumed by the encoder.
 */
public abstract class Encoder<T> {
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * Returns the character set to use when encoding an output stream.
     *
     * @return
     * The output stream's character set.
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets the character set to use when encoding an output stream.
     *
     * @param charset
     * The output stream's character set.
     */
    public void setCharset(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException();
        }

        this.charset = charset;
    }

    /**
     * Writes a value to an output stream.
     *
     * @param value
     * The value to encode.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void write(T value, OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException();
        }

        write(value, new OutputStreamWriter(outputStream, charset));
    }

    /**
     * Writes a value to a character stream.
     *
     * @param value
     * The value to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public abstract void write(T value, Writer writer) throws IOException;
}
