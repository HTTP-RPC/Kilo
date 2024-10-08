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
import static org.junit.jupiter.api.Assertions.*;

public class CSVDecoderTest {
    @Test
    public void testRead() throws IOException {
        var expected = listOf(
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

        var text = "\"a\",\"b\",\"c\",\"d\",\"e\",\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true,\r\n"
            + "\" D\rÉ\nF\r\n\",2,4.0,false\r\n"
            + ",3,6.0\n";

        var csvDecoder = new CSVDecoder();

        var actual = csvDecoder.read(new StringReader(text));

        assertEquals(expected, actual);
    }

    @Test
    public void testMissingHeader() {
        var csvDecoder = new CSVDecoder();

        assertThrows(IOException.class, () -> csvDecoder.read(new StringReader("")));
        assertThrows(IOException.class, () -> csvDecoder.read(new StringReader("\n")));
        assertThrows(IOException.class, () -> csvDecoder.read(new StringReader("\n\n")));
    }
}
