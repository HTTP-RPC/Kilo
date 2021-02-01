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
import java.io.StringWriter;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVEncoderTest {
    @Test
    public void testWrite() throws IOException {
        String expected = "\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"G\"\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true,3,0,\"12%\"\r\n"
            + "\" D\r\nÉ\r\nF\r\n\",2,4.0,,,,\r\n";

        List<Map<String, ?>> values = listOf(
            mapOf(
                entry("a", "A,B,\"C\" "),
                entry("b", 1),
                entry("c", 2.0),
                entry("d", true),
                entry("e", DayOfWeek.THURSDAY),
                entry("f", new Date(0)),
                entry("g", 0.12)
            ),
            mapOf(
                entry("a", " D\r\nÉ\r\nF\r\n"),
                entry("b", 2),
                entry("c", 4.0),
                entry("g", null)
            )
        );

        StringWriter writer = new StringWriter();

        CSVEncoder csvEncoder = new CSVEncoder(listOf("a", "b", "c", "d", "e", "f", "g"));

        csvEncoder.setLabels(mapOf(
            entry("g", "G")
        ));

        csvEncoder.setFormats(mapOf(
            entry("g", NumberFormat.getPercentInstance())
        ));

        csvEncoder.write(values, writer);

        assertEquals(expected, writer.toString());
    }
}
