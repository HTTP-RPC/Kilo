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

import java.io.IOException;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class CSVEncoderTest {
    @Test
    public void testWrite() throws IOException {
        var expected = "\"a\",\"b\",\"c\",\"d\",\"e\",\"F\"\r\n"
            + "\"A,B,\"\"C\"\" \",1,2.0,true,0,\"12%\"\r\n"
            + "\" D\r\nÉ\r\nF\r\n\",2,4.0,,,\r\n";

        var values = listOf(
            mapOf(
                entry("a", "A,B,\"C\" "),
                entry("b", 1),
                entry("c", 2.0),
                entry("d", true),
                entry("e", new Date(0)),
                entry("f", 0.12)
            ),
            mapOf(
                entry("a", " D\r\nÉ\r\nF\r\n"),
                entry("b", 2),
                entry("c", 4.0),
                entry("f", null)
            )
        );

        var csvEncoder = new CSVEncoder(listOf("a", "b", "c", "d", "e", "f"));

        csvEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getPackageName() + ".csv"));

        csvEncoder.format("f", NumberFormat.getPercentInstance());

        var writer = new StringWriter();

        csvEncoder.write(values, writer);

        var actual = writer.toString();

        assertEquals(expected, actual);
    }
}
