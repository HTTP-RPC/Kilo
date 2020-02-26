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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVDecoderTest {
    @Test
    public void testRead() throws IOException {
        String text = "\"a\",\"b\",\"c\",\"d\",\"e\"\r\n"
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

        StringReader reader = new StringReader(text);

        CSVDecoder csvDecoder = new CSVDecoder();

        List<Map<String, String>> actual = csvDecoder.read(reader).stream().collect(Collectors.toList());

        assertEquals(expected, actual);
    }
}
