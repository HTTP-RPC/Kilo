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
import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVEncoderTest {
    @Test
    public void testWrite() throws IOException {
        String expected = "\"1\",\"2\",\"3\",\"d.e\",\"f\",\"g\"\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true,0,3\r\n"
            + "\" D\r\nÉ\r\nF\r\n\",2,4.0,,,\r\n";

        List<Map<String, ?>> values = listOf(
            mapOf(
                entry("a", "A,B,\"C\" "),
                entry("b", 1),
                entry("c", 2.0),
                entry("d", mapOf(
                    entry("e", true)
                )),
                entry("f", new Date(0)),
                entry("g", DayOfWeek.THURSDAY)
            ),
            mapOf(
                entry("a", " D\r\nÉ\r\nF\r\n"),
                entry("b", 2),
                entry("c", 4.0)
            )
        );

        StringWriter writer = new StringWriter();

        List<CSVEncoder.Column> columns = listOf(
            new CSVEncoder.Column("a", "1"),
            new CSVEncoder.Column("b", "2"),
            new CSVEncoder.Column("c", "3"),
            new CSVEncoder.Column("d.e"),
            new CSVEncoder.Column("f"),
            new CSVEncoder.Column("g")
        );

        CSVEncoder csvEncoder = new CSVEncoder(columns);

        csvEncoder.write(values, writer);

        assertEquals(expected, writer.toString());
    }
}
