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
import org.junit.Assert;
import org.junit.Test;

public class FlatFileDecoderTest extends AbstractTest {
    @Test
    public void testRead() throws IOException {
        String text = " AB C     1    2.35   \r\n" +
            "  DEFGH IJ2    4.57   \r\n";

        List<Map<String, ?>> expected = listOf(
            mapOf(
                entry("a", "AB C"),
                entry("b", "1"),
                entry("c", "2.35")
            ),
            mapOf(
                entry("a", "DEFGH IJ"),
                entry("b", "2"),
                entry("c", "4.57")
            )
        );

        StringReader reader = new StringReader(text);

        FlatFileDecoder flatFileDecoder = new FlatFileDecoder(listOf(
            new FlatFileDecoder.Field("a", 10),
            new FlatFileDecoder.Field("b", 5),
            new FlatFileDecoder.Field("c", 7)
        ));

        Iterable<Map<String, String>> cursor = flatFileDecoder.read(reader);
        List<Map<String, String>> actual = StreamSupport.stream(cursor.spliterator(), false).collect(Collectors.toList());

        Assert.assertEquals(expected, actual);
    }
}
