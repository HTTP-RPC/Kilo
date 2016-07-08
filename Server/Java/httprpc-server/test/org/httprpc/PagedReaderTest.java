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
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

public class PagedReaderTest {
    @Test
    public void testStartMark() throws IOException {
        // Test placing a mark on the first character
        String text = "abcdefg";

        try (PagedReader reader = new PagedReader(new StringReader(text), 3)) {
            reader.mark(0);

            for (int i = 0, n = text.length(); i < n; i++) {
                Assert.assertEquals(reader.read(), text.charAt(i));
            }

            reader.reset();

            for (int i = 0, n = text.length(); i < n; i++) {
                Assert.assertEquals(text.charAt(i), reader.read());
            }

            Assert.assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testEndMark() throws IOException {
        // Test placing a mark on the last character
        String text = "abcdefg";

        try (PagedReader reader = new PagedReader(new StringReader(text), 5)) {
            for (int i = 0, n = text.length() - 1; i < n; i++) {
                reader.read();
            }

            reader.mark(0);

            Assert.assertEquals(text.charAt(text.length() - 1), reader.read());

            reader.reset();

            Assert.assertEquals(text.charAt(text.length() - 1), reader.read());
            Assert.assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testSingleMark() throws IOException {
        // Test placing a single mark mid-stream
        String text = "abcdefg";

        int i = 0, n = text.length();

        int j = text.indexOf('b') + 1;
        int k = text.indexOf('e') + 1;

        try (PagedReader reader = new PagedReader(new StringReader(text), 7)) {
            while (i < j) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            reader.mark(0);

            while (i < k) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            reader.reset();

            i = j;

            while (i < n) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            Assert.assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testNestedMarks() throws IOException {
        // Test placing nested marks mid-stream
        String text = "abcdefghijklmnop";

        int i = 0, n = text.length();

        int j = text.indexOf('b') + 1;
        int k = text.indexOf('f') + 1;
        int l = text.indexOf('h') + 1;

        try (PagedReader reader = new PagedReader(new StringReader(text), 7)) {
            while (i < j) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            reader.mark(0);

            while (i < k) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            reader.mark(0);

            while (i < l) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            reader.reset();

            i = k;

            while (i < l) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            reader.reset();

            i = j;

            while (i < n) {
                Assert.assertEquals(text.charAt(i++), reader.read());
            }

            Assert.assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testEmptyReader() throws IOException {
        // Test handling of an empty source reader
        try (PagedReader reader = new PagedReader(new StringReader(""), 0)) {
            Assert.assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testSingleCharacterReader() throws IOException {
        // Test handling of a single-character source reader
        try (PagedReader reader = new PagedReader(new StringReader("a"), 1)) {
            reader.mark(0);

            Assert.assertEquals('a', reader.read());

            reader.reset();

            Assert.assertEquals('a', reader.read());
        }
    }

    @Test
    public void testReadBuffer1() throws IOException {
        // Test reading into a buffer
        String text = "abcdefghijklmnop";

        int off = 2, len = 8, n = text.length();

        try (PagedReader reader = new PagedReader(new StringReader(text), 3)) {
            char[] cbuf = new char[text.length()];

            for (int i = 0; i < off; i++) {
                cbuf[i] = (char)reader.read();
            }

            Assert.assertEquals(len, reader.read(cbuf, off, len));

            for (int i = off + len; i < n; i++) {
                cbuf[i] = (char)reader.read();
            }

            Assert.assertEquals(text, new String(cbuf));

            Assert.assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testReadBuffer2() throws IOException {
        // Test reading into a buffer past the end of the stream
        String text = "abcdef";

        int n = text.length();
        int off = n / 2;

        try (PagedReader reader = new PagedReader(new StringReader(text), 1)) {
            char[] cbuf = new char[text.length()];

            Assert.assertEquals(off, reader.read(cbuf, 0, off));
            Assert.assertEquals(off, reader.read(cbuf, off, n));

            Assert.assertEquals(text, new String(cbuf));

            Assert.assertEquals(-1, reader.read());
        }
    }
}
