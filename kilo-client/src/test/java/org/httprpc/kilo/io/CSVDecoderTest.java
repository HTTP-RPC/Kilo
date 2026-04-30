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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class CSVDecoderTest {
    @Test
    public void testRead() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(
            mapOf(
                entry("a", "A"),
                entry("b", "B")
            ),
            mapOf(
                entry("a", "C"),
                entry("b", "D"),
                entry("c", "É")
            ),
            mapOf(
                entry("a", "F"),
                entry("b", "G"),
                entry("c", "H")
            )
        ), csvDecoder.read(new StringReader("a,b,c\r\nA,B\r\nC,D,É\r\nF,G,H,I")));
    }

    @Test
    public void testReadMultiple() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(
            mapOf(
                entry("a", "A"),
                entry("b", "B"),
                entry("c", "C")
            )
        ), csvDecoder.read(new StringReader("a,b,c\r\nA,B,C")));

        assertEquals(listOf(
            mapOf(
                entry("d", "D"),
                entry("e", "É"),
                entry("f", "F")
            )
        ), csvDecoder.read(new StringReader("d,e,f\r\nD,É,F")));
    }

    @Test
    public void testMissingKeys() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(
            mapOf(
                entry("", "A")
            ),
            mapOf(
                entry("", "D")
            )
        ), csvDecoder.read(new StringReader("\r\nA,B,C\r\nD,É,F\r\n")));
    }

    @Test
    public void testMissingValues() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(
            mapOf(
                entry("a", ""),
                entry("b", ""),
                entry("c", "")
            ),
            mapOf(
                entry("a", ""),
                entry("b", ""),
                entry("c", "")
            )
        ), csvDecoder.read(new StringReader("a,b,c\r\n,,\r\n,,\r\n")));
    }

    @Test
    public void testMissingRows() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(), csvDecoder.read(new StringReader("a,b,c")));
    }

    @Test
    public void testEmpty() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(), csvDecoder.read(new StringReader("")));
    }

    @Test
    public void testQuotes() throws IOException {
        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(
            mapOf(
                entry("a", "A,B,\"C\",\r\nD,É")
            )
        ), csvDecoder.read(new StringReader("\"a\"\r\n\"A,B,\"\"C\"\",\r\nD,É\"\r\n")));
    }

    @Test
    public void testIterate() throws IOException {
        var text = "\"a\",\"b\",\"c\"\r\n1,2,3\r\n4,5,6\r\n7,8,9\r\n";

        var inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        var csvDecoder = new CSVDecoder();

        assertEquals(listOf(
            mapOf(
                entry("a", "1"),
                entry("b", "2"),
                entry("c", "3")
            ),
            mapOf(
                entry("a", "4"),
                entry("b", "5"),
                entry("c", "6")
            ),
            mapOf(
                entry("a", "7"),
                entry("b", "8"),
                entry("c", "9")
            )
        ), csvDecoder.read(inputStream));
    }
}
