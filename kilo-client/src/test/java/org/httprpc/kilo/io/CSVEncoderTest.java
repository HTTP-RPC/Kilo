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
import java.io.StringWriter;
import java.util.Date;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class CSVEncoderTest {
    @Test
    public void testWrite() throws IOException {
        var now = new Date(0);

        var row = listOf("a,b,\"c\",\r\nd,é", 123, true, now);

        var csvEncoder = new CSVEncoder();

        csvEncoder.format(Boolean.class, flag -> flag ? "Y" : "N");

        var writer = new StringWriter();

        csvEncoder.write(row, writer);

        assertEquals("\"a,b,\"\"c\"\",\r\nd,é\",123,\"Y\",0\r\n", writer.toString());
    }

    @Test
    public void testWriteAll() throws IOException {
        var rows = listOf(
            listOf("abc", 123, true),
            listOf("def", 456, false)
        );

        var csvEncoder = new CSVEncoder();

        var writer = new StringWriter();

        csvEncoder.writeAll(rows, writer);

        assertEquals("\"abc\",123,true\r\n\"def\",456,false\r\n", writer.toString());
    }
}
