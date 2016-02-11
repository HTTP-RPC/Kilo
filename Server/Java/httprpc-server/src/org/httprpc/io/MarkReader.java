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
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Reader that provides multiple mark support.
 */
public class MarkReader extends Reader {
    private Reader reader;
    private int pageSize;

    private int position = 0;
    private int count = 0;

    private ArrayList<char[]> pages = new ArrayList<>();

    private LinkedList<Integer> marks = new LinkedList<>();

    private static int DEFAULT_PAGE_SIZE = 1024;
    private static int EOF = -1;

    /**
     * Constructs a new mark reader.
     *
     * @param reader
     * The source reader.
     */
    public MarkReader(Reader reader) {
        this(reader, DEFAULT_PAGE_SIZE);
    }

    /**
     * Constructs a new mark reader.
     *
     * @param reader
     * The source reader.
     *
     * @param pageSize
     * The page size.
     */
    public MarkReader(Reader reader, int pageSize) {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        this.reader = reader;
        this.pageSize = pageSize;
    }

    @Override
    public int read() throws IOException {
        int c;
        if (position < count) {
            c = pages.get(position / pageSize)[position % pageSize];

            position++;
        } else {
            c = reader.read();

            if (c != EOF) {
                if (position / pageSize == pages.size()) {
                    pages.add(new char[pageSize]);
                }

                pages.get(pages.size() - 1)[position % pageSize] = (char)c;

                position++;
                count++;
            }
        }

        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int c = read();

        int n;
        if (c != EOF) {
            n = 0;

            for (int i = off; i < cbuf.length && n < len; i++) {
                cbuf[i] = (char)c;
                n++;

                c = read();

                if (c == EOF) {
                    break;
                }
            }
        } else {
            n = EOF;
        }

        return n;
    }

    @Override
    public void mark(int readAheadLimit) {
        marks.push(position);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void reset() {
        if (marks.isEmpty()) {
            throw new IllegalStateException();
        }

        position = marks.pop();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
