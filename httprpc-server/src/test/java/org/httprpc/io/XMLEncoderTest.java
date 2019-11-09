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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.httprpc.util.Collections.*;

public class XMLEncoderTest {
    @Test
    public void testWrite() throws IOException {
        String expected = "<?xml version=\"1.0\" ?>"
            + "<root>"
            + "<item a=\"ABC\" b=\"1\" c=\"2.345\" f=\"0\" g=\"3\">"
            + "<d><e f=\"XYZ\"></e></d>"
            + "<e><item f=\"true\"></item><item f=\"false\"></item></e>"
            + "</item>"
            + "<item a=\"DÉF\" b=\"2\" c=\"4.5678\"></item>"
            + "</root>";

        List<Map<String, ?>> values = listOf(
            mapOf(
                entry("a", "ABC"),
                entry("b", 1),
                entry("c", 2.345),
                entry("d", mapOf(
                    entry("e", mapOf(
                        entry("f", "XYZ")
                    ))
                )),
                entry("e", listOf(
                    mapOf(entry("f", true)),
                    mapOf(entry("f", false))
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

        Assertions.assertEquals(expected, writer.toString());
    }

    @Test
    public void testTransform() throws IOException {
        String expected = "1,2,3\r\n"
            + "4,5,6\r\n"
            + "7,8,9\r\n";

        List<Map<String, ?>> values = listOf(
            mapOf(
                entry("a", "1"),
                entry("b", "2"),
                entry("c", "3")
            ),
            mapOf(
                entry("a", "4"),
                entry("b", "5"),
                entry("c", "6")
            ),
            mapOf(
                entry("a", "7"),
                entry("b", "8"),
                entry("c", "9")
            )
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        XMLEncoder xmlEncoder = new XMLEncoder();

        xmlEncoder.write(values, outputStream);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        Transformer transformer;
        try {
            StreamSource source = new StreamSource(getClass().getResourceAsStream("test.xslt"));

            transformer = transformerFactory.newTransformer(source);
        } catch (TransformerConfigurationException exception) {
            throw new RuntimeException(exception);
        }

        StringWriter writer = new StringWriter();

        try {
            StreamSource source = new StreamSource(new ByteArrayInputStream(outputStream.toByteArray()));
            StreamResult result = new StreamResult(writer);

            transformer.transform(source, result);
        } catch (TransformerException exception) {
            throw new IOException(exception);
        }

        Assertions.assertEquals(expected, writer.toString());
    }
}
