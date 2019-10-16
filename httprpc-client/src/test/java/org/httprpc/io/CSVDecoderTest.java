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
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.httprpc.AbstractTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CSVDecoderTest extends AbstractTest {
    @Test
    public void testRead() throws IOException {
        String text = "\"a\",\"b\",\"c\",\"d\"\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true\r\n"
            + "\" D\rÉ\nF\r\n\",2,4.0,false\n";

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
            )
        );

        StringReader reader = new StringReader(text);

        CSVDecoder csvDecoder = new CSVDecoder();

        Iterable<Map<String, String>> cursor = csvDecoder.read(reader);
        List<Map<String, String>> actual = StreamSupport.stream(cursor.spliterator(), false).collect(Collectors.toList());

        Assertions.assertEquals(expected, actual);
    }
}
