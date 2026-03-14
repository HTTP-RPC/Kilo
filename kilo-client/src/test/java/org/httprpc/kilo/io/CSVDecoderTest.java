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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

public class CSVDecoderTest {
    @Test
    public void testRead() throws IOException {
        var text = " a , \"\"\"b\"\",\rc,\nd\r\n\"\"é\"\"\" , f ";

        var csvDecoder = new CSVDecoder();

        var reader = new StringReader(text);

        var row = csvDecoder.read(reader);

        assertEquals(listOf(" a ", " \"b\",\rc,\nd\r\n\"é\" ", " f "), row);
    }

    @Test
    public void testReadEmpty() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertTrue(csvDecoder.read(new StringReader("")).isEmpty());
    }

    @Test
    public void testReadAll() {
        var text = "1,2,3\r4,5,6\n7,8,9\r\n";

        var csvDecoder = new CSVDecoder();

        var reader = new StringReader(text);

        var rows = listOf(mapAll(csvDecoder.readAll(reader), row -> listOf(mapAll(row, Integer::valueOf))));

        assertEquals(listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        ), rows);
    }

    @Test
    public void testReadAllEmpty() {
        var csvDecoder = new CSVDecoder();

        assertEquals(0, countOf(csvDecoder.readAll(new StringReader(""))));
    }

    @Test
    public void testHeadings() throws IOException {
        var text = "\"a\",\"b\",\"c\"\r\n1,2,3\r\n4,5,6\r\n";

        var csvDecoder = new CSVDecoder();

        var reader = new StringReader(text);

        var headings = csvDecoder.read(reader);

        assertEquals(listOf("a", "b", "c"), headings);

        var rows = listOf(mapAll(csvDecoder.readAll(reader), row -> listOf(mapAll(row, Integer::valueOf))));

        assertEquals(listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        ), rows);
    }
}
