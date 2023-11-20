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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Encodes plain text content.
 */
public class TextEncoder extends Encoder<CharSequence> {
    @Override
    public void write(CharSequence value, Writer writer) throws IOException {
        if (value == null || writer == null) {
            throw new IllegalArgumentException();
        }

        writer = new BufferedWriter(writer);

        for (int i = 0, n = value.length(); i < n; i++) {
            writer.write(value.charAt(i));
        }

        writer.flush();
    }
}
