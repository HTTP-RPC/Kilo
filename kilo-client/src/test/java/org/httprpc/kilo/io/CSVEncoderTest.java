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
import java.time.Instant;
import java.util.Date;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class CSVEncoderTest {
    @Test
    public void testWrite() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("a", "b", "c", "d", "e"));

        var writer = new StringWriter();

        csvEncoder.write(listOf(
            mapOf(
                entry("a", "ABC"),
                entry("b", 123),
                entry("c", true),
                entry("d", new Date(0)),
                entry("e", Instant.ofEpochMilli(0))
            ),
            mapOf(
                entry("a", "DEF"),
                entry("b", 456),
                entry("c", false)
            )
        ), writer);

        assertEquals("\"a\",\"b\",\"c\",\"d\",\"e\"\r\n\"ABC\",123,true,0,0\r\n\"DEF\",456,false,,\r\n", writer.toString());
    }

    @Test
    public void testMissingKeys() throws IOException {
        var csvEncoder = new CSVEncoder(listOf());

        var writer = new StringWriter();

        csvEncoder.write(listOf(
            mapOf(
                entry("a", 1),
                entry("b", 2),
                entry("c", 3)
            )
        ), writer);

        assertEquals("\r\n\r\n", writer.toString());
    }

    @Test
    public void testMissingValues() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("a", "b", "c"));

        var writer = new StringWriter();

        csvEncoder.write(listOf(
            mapOf(),
            mapOf()
        ), writer);

        assertEquals("\"a\",\"b\",\"c\"\r\n,,\r\n,,\r\n", writer.toString());
    }

    @Test
    public void testMissingRows() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("a", "b", "c"));

        var writer = new StringWriter();

        csvEncoder.write(listOf(), writer);

        assertEquals("\"a\",\"b\",\"c\"\r\n", writer.toString());
    }

    @Test
    public void testEmpty() throws IOException {
        var csvEncoder = new CSVEncoder(listOf());

        var writer = new StringWriter();

        csvEncoder.write(listOf(), writer);

        assertEquals("\r\n", writer.toString());
    }

    @Test
    public void testQuotes() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("a"));

        var writer = new StringWriter();

        csvEncoder.write(listOf(
            mapOf(
                entry("a", "A,B,\"C\",\r\nD,É")
            )
        ), writer);

        assertEquals("\"a\"\r\n\"A,B,\"\"C\"\",\r\nD,É\"\r\n", writer.toString());
    }

    @Test
    public void testNNBSP() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("a"));

        var writer = new StringWriter();

        csvEncoder.write(listOf(
            mapOf(
                entry("a", "a\u202fb\u202fc\u202f")
            )
        ), writer);

        assertEquals("\"a\"\r\n\"a b c \"\r\n", writer.toString());
    }

    @Test
    public void testResourceBundle() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("a", "b", "c"));

        csvEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getPackageName() + ".resource"));

        var writer = new StringWriter();

        csvEncoder.write(listOf(
            mapOf(
                entry("a", 1),
                entry("b", 2),
                entry("c", 3)
            )
        ), writer);

        assertEquals("\"A1\",\"B2\",\"c\"\r\n1,2,3\r\n", writer.toString());
    }

    @Test
    public void testFormat() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("a"));

        csvEncoder.format(Boolean.class, flag -> flag ? "Y" : "N");

        var writer = new StringWriter();

        csvEncoder.write(listOf(
            mapOf(
                entry("a", true)
            )
        ), writer);

        assertEquals("\"a\"\r\n\"Y\"\r\n", writer.toString());
    }
}
