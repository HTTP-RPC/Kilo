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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVDecoderTest {
    @Test
    public void testRead() throws IOException {
        var text = "\"a\",\"b\",\"c\",\"d\",\"e\"\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true,\r\n"
            + "\" D\rÉ\nF\r\n\",2,4.0,false\r\n"
            + ",3,6.0\n";

        List<Map<String, ?>> expected = listOf(
            mapOf(
                entry("a", "A,B,\"C\" "),
                entry("b", "1"),
                entry("c", "2.0"),
                entry("d", "true")
            ),
            mapOf(
                entry("a", " D\rÉ\nF\r\n"),
                entry("b", "2"),
                entry("c", "4.0"),
                entry("d", "false")
            ),
            mapOf(
                entry("b", "3"),
                entry("c", "6.0")
            )
        );

        var reader = new StringReader(text);

        var csvDecoder = new CSVDecoder();

        assertEquals(expected, csvDecoder.read(reader));
    }

    @Test
    public void testIterate() throws IOException {
        List<Map<String, ?>> expected = listOf(
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
        );

        var actual = new ArrayList<Map<String, String>>(expected.size());

        try (var inputStream = getClass().getResourceAsStream("example.csv")) {
            var csvDecoder = new CSVDecoder();

            for (var row : csvDecoder.iterate(inputStream)) {
                actual.add(row);
            }
        }

        assertEquals(expected, actual);
    }
}
