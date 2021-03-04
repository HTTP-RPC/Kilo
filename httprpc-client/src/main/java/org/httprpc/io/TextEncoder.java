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
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Text encoder.
 */
public class TextEncoder extends Encoder<String> {
    /**
     * Constructs a new text encoder.
     */
    public TextEncoder() {
        this(StandardCharsets.UTF_8);
    }

    /**
     * Constructs a new text encoder.
     *
     * @param charset
     * The character set to use when encoding the text.
     */
    public TextEncoder(Charset charset) {
        super(charset);
    }

    @Override
    public void write(String value, Writer writer) throws IOException {
        writer = new BufferedWriter(writer);

        for (int i = 0, n = value.length(); i < n; i++) {
            writer.write(value.charAt(i));
        }

        writer.flush();
    }
}
