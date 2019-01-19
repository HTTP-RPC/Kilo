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
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.httprpc.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class FlatFileEncoderTest extends AbstractTest {
    @Test
    public void testWrite() throws IOException {
        String expected = " AB C     1    2.35   \r\n" +
            "  DEFGH IJ2    4.57   \r\n";

        List<Map<String, ?>> values = listOf(
            mapOf(
                entry("a", " AB C "),
                entry("b", 1),
                entry("c", 2.345)
            ),
            mapOf(
                entry("a", "  DEFGH IJKL "),
                entry("b", 2),
                entry("c", 4.5678)
            )
        );

        StringWriter writer = new StringWriter();

        FlatFileEncoder flatFileEncoder = new FlatFileEncoder(listOf(
            new FlatFileEncoder.Field("a", 10),
            new FlatFileEncoder.Field("b", 5, "%d"),
            new FlatFileEncoder.Field("c", 7, "%.2f")
        ));

        flatFileEncoder.write(values, writer);

        Assert.assertEquals(expected, writer.toString());
    }
}
