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
import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.httprpc.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

public class XMLEncoderTest extends AbstractTest {
    @Test
    public void testWrite() throws IOException {
        String expected = "<?xml version=\"1.0\" ?>"
            + "<root>"
            + "<item a=\"ABC\" b=\"1\" c=\"2.345\" f=\"0\" g=\"3\">"
            + "<d><item e=\"true\"></item><item e=\"false\"></item></d>"
            + "<e><f g=\"XYZ\"></f></e>"
            + "</item>"
            + "<item a=\"DÉF\" b=\"2\" c=\"4.5678\"></item>"
            + "</root>";

        List<Map<String, ?>> values = listOf(
            mapOf(
                entry("a", "ABC"),
                entry("b", 1),
                entry("c", 2.345),
                entry("d", listOf(
                    mapOf(entry("e", true)),
                    mapOf(entry("e", false))
                )),
                entry("e", mapOf(
                    entry("f", mapOf(
                        entry("g", "XYZ")
                    ))
                )),
                entry("f", new Date(0)),
                entry("g", DayOfWeek.THURSDAY)
            ),
            mapOf(
                entry("a", "DÉF"),
                entry("b", 2),
                entry("c", 4.5678)
            )
        );

        StringWriter writer = new StringWriter();

        XMLEncoder xmlEncoder = new XMLEncoder();

        xmlEncoder.write(values, writer);

        Assert.assertEquals(expected, writer.toString());
    }
}
