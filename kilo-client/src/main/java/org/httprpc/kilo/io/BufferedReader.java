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
import java.io.Reader;

class BufferedReader extends Reader {
    private Reader reader;
    private char[] buffer;

    private int i = 0;
    private int n = 0;

    BufferedReader(Reader reader) {
        this.reader = reader;

        buffer = new char[8192];
    }

    @Override
    public int read() throws IOException {
        if (i == n) {
            i = 0;

            n = reader.read(buffer);

            if (n == -1) {
                n = 0;

                return -1;
            }
        }

        return buffer[i++];
    }

    @Override
    public int read(char[] cbuf, int off, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }
}
