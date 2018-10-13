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

class BufferedWriter extends Writer {
    private Writer writer;
    private char[] buffer;

    private int i = 0;

    public BufferedWriter(Writer writer) {
        this.writer = writer;

        buffer = new char[8192];
    }

    @Override
    public void write(int c) throws IOException {
        buffer[i++] = (char)c;

        if (i == buffer.length) {
            flush();
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(String str) throws IOException {
        for (int i = 0, n = str.length(); i < n; i++) {
            write(str.charAt(i));
        }
    }

    @Override
    public void flush() throws IOException {
        writer.write(buffer, 0, i);
        writer.flush();

        i = 0;
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }
}
