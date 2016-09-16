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

package org.httprpc;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import static org.httprpc.WebService.listOf;
import static org.httprpc.WebService.mapOf;
import static org.httprpc.WebService.entry;

public class JSONEncoderTest {
    @Test
    public void testString() throws IOException {
        Assert.assertTrue(encode("abcdéfg").equals("\"abcdéfg\""));
        Assert.assertTrue(encode("\b\f\r\n\t").equals("\"\\b\\f\\r\\n\\t\""));
    }

    @Test
    public void testNumber() throws IOException {
        Assert.assertTrue(encode(42).equals("42"));
        Assert.assertTrue(encode(42L).equals("42"));
        Assert.assertTrue(encode(123F).equals("123.0"));
        Assert.assertTrue(encode(123.0).equals("123.0"));
    }

    @Test
    public void testBoolean() throws IOException {
        Assert.assertTrue(encode(true).equals("true"));
        Assert.assertTrue(encode(false).equals("false"));
    }

    @Test
    public void testArray() throws IOException {
        String json = encode(listOf(
            "abc",
            123,
            true,
            listOf(1, 2L, 3.0),
            mapOf(entry("x", 1), entry("y", 2F), entry("z", 3.0))
        )).replaceAll("\\s+", "");

        Assert.assertTrue(json.equals("[\"abc\",123,true,[1,2,3.0],{\"x\":1,\"y\":2.0,\"z\":3.0}]"));
    }

    @Test
    public void testObject() throws IOException {
        Object json = encode(mapOf(
            entry("a", "abc"),
            entry("b", 123),
            entry("c", true),
            entry("d", listOf(1, 2L, 3.0)),
            entry("e", mapOf(entry("x", 1), entry("y", 2F), entry("z", 3.0)))
        )).replaceAll("\\s+", "");

        Assert.assertTrue(json.equals("{\"a\":\"abc\",\"b\":123,\"c\":true,\"d\":[1,2,3.0],\"e\":{\"x\":1,\"y\":2.0,\"z\":3.0}}"));
    }

    @Test
    public void testDates() throws IOException {
        Assert.assertTrue(encode(new Date(0)).equals("0"));

        LocalDate date = LocalDate.now();
        Assert.assertTrue(encode(date).equals("\"" + date.format(DateTimeFormatter.ISO_DATE) + "\""));

        LocalTime time = LocalTime.now();
        Assert.assertTrue(encode(time).equals("\"" + time.format(DateTimeFormatter.ISO_TIME) + "\""));

        LocalDateTime dateTime = LocalDateTime.now();
        Assert.assertTrue(encode(dateTime).equals("\"" + dateTime.format(DateTimeFormatter.ISO_DATE_TIME) + "\""));
    }

    private String encode(Object value) throws IOException {
        StringWriter writer = new StringWriter();

        JSONEncoder encoder = new JSONEncoder();

        encoder.writeValue(value, writer);

        return writer.toString();
    }
}
