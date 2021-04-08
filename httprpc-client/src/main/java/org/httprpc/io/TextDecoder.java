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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Text decoder.
 */
public class TextDecoder extends Decoder<CharSequence> {
    private boolean builder;

    /**
     * Constructs a new text decoder.
     */
    public TextDecoder() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * Constructs a new text decoder.
     *
     * @param charset
     * The character set to use when decoding the text.
     */
    public TextDecoder(Charset charset) {
        this(charset, false);
    }

    /**
     * Constructs a new text decoder.
     *
     * @param charset
     * The character set to use when decoding the text.
     *
     * @param builder
     * <code>true</code> if the result should be returned as a mutable builder;
     * <code>false</code>, otherwise.
     */
    public TextDecoder(Charset charset, boolean builder) {
        super(charset);

        this.builder = builder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends CharSequence> U read(Reader reader) throws IOException {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        reader = new BufferedReader(reader);

        StringBuilder stringBuilder = new StringBuilder();

        int c;
        while ((c = reader.read()) != EOF) {
            stringBuilder.append((char)c);
        }

        if (builder) {
            return (U)stringBuilder;
        } else {
            return (U)stringBuilder.toString();
        }
    }
}
