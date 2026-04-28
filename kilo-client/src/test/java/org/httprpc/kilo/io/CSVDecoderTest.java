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
        var text = "\"a\",\"b\",\"c\",\"d\"\r\n\"ABC\",123,true,0\r\n\"DEF\",,false,\r\n";

        var csvDecoder = new CSVDecoder();

        var rows = csvDecoder.read(new StringReader(text));

        assertEquals(listOf(
            mapOf(
                entry("a", "ABC"),
                entry("b", "123"),
                entry("c", "true"),
                entry("d", "0")
            ),
            mapOf(
                entry("a", "DEF"),
                entry("b", ""),
                entry("c", "false")
            )
        ), rows);
    }

    @Test
    public void testQuotes() throws IOException {
        var text = "\"a\"\r\n\"A,B,\"\"C\"\",\r\nD,É\"\r\n";

        var csvDecoder = new CSVDecoder();

        var rows = csvDecoder.read(new StringReader(text));

        assertEquals(listOf(
            mapOf(
                entry("a", "A,B,\"C\",\r\nD,É")
            )
        ), rows);
    }

    @Test
    public void testIterate() throws IOException {
        var text = "\"a\",\"b\",\"c\"\r\n1,2,3\r\n4,5,6\r\n7,8,9\r\n";

        var csvDecoder = new CSVDecoder();

        var rows = csvDecoder.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));

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
        ), rows);
    }
}
