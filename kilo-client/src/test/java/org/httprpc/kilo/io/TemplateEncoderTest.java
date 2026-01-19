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

import org.httprpc.kilo.xml.ElementAdapter;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class TemplateEncoderTest {
    @Test
    public void testNull() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "dictionary.txt");

        var writer = new StringWriter();

        templateEncoder.write(null, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testMap() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "dictionary.txt");

        var a = "abc";
        var b = 123;
        var c = false;

        var dictionary = mapOf(
            entry("a", a),
            entry("b", b),
            entry("c", c)
        );

        var writer = new StringWriter();

        templateEncoder.write(dictionary, writer);

        assertEquals(String.format("{a=%s,b=%s,c=%s,d=}", a, b, c), writer.toString());
    }

    @Test
    public void testPath() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "path.txt");

        var dictionary = mapOf(
            entry("a", mapOf(
                entry("b", 0),
                entry("d", mapOf(
                    entry("e", 1)
                ))
            ))
        );

        var writer = new StringWriter();

        templateEncoder.write(dictionary, writer);

        assertEquals("01", writer.toString());
    }

    @Test
    public void testConditionalSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSectionList() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf(1))), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSectionEmptyList() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf())), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSectionString() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "abc")), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSectionEmptyString() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "")), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSectionNonZero() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", 123)), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSectionZero() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", 0)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSectionTrue() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", true)), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSectionFalse() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "conditional.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", false)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testEmptyRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating1.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("list", listOf())), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testSingleElementRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating1.txt");

        var a = "hello";
        var b = 1L;
        var c = 2.0;

        var dictionary = mapOf(
            entry("a", a),
            entry("b", b),
            entry("c", c));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("list", listOf(dictionary))), writer);

        assertEquals(String.format("[{a=%s,b=%s,c=%s}]", a, b, c), writer.toString());
    }

    @Test
    public void testMultiElementRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating1.txt");

        var a1 = "hello";
        var b1 = 1L;
        var c1 = 2.0;

        var dictionary1 = mapOf(
            entry("a", a1),
            entry("b", b1),
            entry("c", c1));

        var a2 = "goodbye";
        var b2 = 2L;
        var c2 = 4.0;

        var dictionary2 = mapOf(
            entry("a", a2),
            entry("b", b2),
            entry("c", c2));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("list", listOf(dictionary1, dictionary2))), writer);

        assertEquals(String.format("[{a=%s,b=%s,c=%s}{a=%s,b=%s,c=%s}]", a1, b1, c1, a2, b2, c2), writer.toString());
    }

    @Test
    public void testNestedRepeatingSection1() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating2.txt");

        var dictionary = mapOf(
            entry("abc", "ABC"),
            entry("list1", listOf(mapOf(
                entry("def", "DEF"),
                entry("list2", listOf(mapOf(
                    entry("one", 1),
                    entry("two", 2),
                    entry("three", 3)
                )))
            )))
        );

        var writer = new StringWriter();

        templateEncoder.write(dictionary, writer);

        assertEquals("{abc=ABC,list1=[{def=DEF,list2=[{one=1,two=2,three=3}]]}", writer.toString());
    }

    @Test
    public void testNestedRepeatingSection2() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating3.txt");

        var value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("[[[hello]]]", writer.toString());
    }

    @Test
    public void testNestedEmptyRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating3.txt");

        var writer = new StringWriter();

        templateEncoder.write(listOf(), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testPrimitiveRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating4.txt");

        var value = listOf("hello", 42, false);

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("[(hello)(42)(false)]", writer.toString());
    }

    @Test
    public void testRepeatingSectionSeparator() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating5.txt");

        var value = listOf("a", "b", "c");

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("a,b,c", writer.toString());
    }

    @Test
    public void testMapRepeatingSection1() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating6.txt");

        var value = mapOf(
            entry("entries", mapOf(
                entry("one", mapOf(entry("value", 1))),
                entry("two", mapOf(entry("value", 2))),
                entry("three", mapOf(entry("value", 3)))
            ))
        );

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("one:1,two:2,three:3", writer.toString());
    }

    @Test
    public void testMapRepeatingSection2() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "repeating7.txt");

        var value = mapOf(
            entry("entries", mapOf(
                entry("a", "A"),
                entry("b", "B"),
                entry("c", "C")
            ))
        );

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("a:A,b:B,c:C", writer.toString());
    }

    @Test
    public void testInvertedSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSectionList() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf(1))), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSectionEmptyList() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf())), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSectionString() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "abc")), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSectionEmptyString() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "")), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSectionNonZero() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", 123)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSectionZero() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", 0)), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSectionTrue() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", true)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSectionFalse() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inverted.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", false)), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testResources() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "resource1.txt");

        templateEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getPackageName() + ".resource"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("A1B2c", writer.toString());
    }

    @Test
    public void testInheritance() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "inheritance.txt");

        var dictionary = mapOf(
            entry("a", "$"),
            entry("b", mapOf(
                entry("c", "C")
            )),
            entry("d", mapOf(
                entry("a", "A"),
                entry("list", listOf(1, 2, 3)),
                entry("map", mapOf(
                    entry("x", "one"),
                    entry("y", "two"),
                    entry("z", "three")
                )),
                entry("e", null)
            )),
            entry("e", "E")
        );

        var writer = new StringWriter();

        templateEncoder.write(dictionary, writer);

        assertEquals("AC1,AC2,AC3 ACone,ACtwo,ACthree", writer.toString());
    }

    @Test
    public void testComment() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "comment.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("><", writer.toString());
    }

    @Test
    public void testFloatFormatModifier() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "format1.txt");

        var writer = new StringWriter();

        templateEncoder.write(4.5, writer);

        assertEquals("4.50", writer.toString());
    }

    @Test
    public void testDateFormatModifiers() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "format2.txt");

        var date = new Date();
        var instant = date.toInstant();
        var localDate = LocalDate.now();
        var localTime = LocalTime.now();
        var localDateTime = LocalDateTime.now();

        var zoneId = ZoneId.systemDefault();

        var zonedLocalDate = ZonedDateTime.of(LocalDateTime.of(localDate, LocalTime.MIDNIGHT), zoneId);
        var zonedLocalTime = ZonedDateTime.of(LocalDateTime.of(LocalDate.now(), localTime), zoneId);
        var zonedLocalDateTime = ZonedDateTime.of(localDateTime, zoneId);

        var writer = new StringWriter();

        templateEncoder.write(mapOf(
            entry("timestamp", date.getTime()),
            entry("date", date),
            entry("instant", instant),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime)
        ), writer);

        var now = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        var lineSeparator = System.lineSeparator();

        assertEquals(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + "," + lineSeparator
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + "," + lineSeparator
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + "," + lineSeparator
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDateTime.of(localDate, LocalTime.MIDNIGHT)) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalDateTime.of(LocalDate.now(), localTime)) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(localDateTime) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(zonedLocalDate) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).format(zonedLocalTime) + "," + lineSeparator
            + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).format(zonedLocalDateTime), writer.toString());
    }

    @Test
    public void testDefaultContentType() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "default-content-type.txt");

        templateEncoder.setResourceBundle(ResourceBundle.getBundle(getClass().getPackageName() + ".test"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(
            entry("b", "a<b>c&d\"e")
        ), writer);

        assertEquals("<?xml version=\"1.0\"?><a b=\"a&lt;b&gt;c&amp;d&quot;e\">f&lt;g&gt;h&amp;i&quot;j</a>", writer.toString());
    }

    @Test
    public void testSimpleInclude() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "master1.txt");

        var writer = new StringWriter();

        templateEncoder.write("hello", writer);

        assertEquals("(hello)", writer.toString());
    }

    @Test
    public void testSectionInclude() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "master2.txt");

        var writer = new StringWriter();

        templateEncoder.write(listOf("a", "b", "c"), writer);

        assertEquals("[(a)(b)(c)]", writer.toString());
    }

    @Test
    public void testRecursion() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "recursion.txt");

        var list = listOf(
            listOf(
                listOf(), listOf()
            ),
            listOf(
                listOf(), listOf(), listOf()
            ),
            listOf(
                listOf()
            )
        );

        var writer = new StringWriter();

        templateEncoder.write(list, writer);

        assertEquals("[[[][]][[][][]][[]]]", writer.toString());
    }

    @Test
    public void testEmptyRecursion() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "recursion.txt");

        var list = listOf();

        var writer = new StringWriter();

        templateEncoder.write(list, writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testEmbeddedXML() throws IOException {
        var xml = "<abc>123</abc>";

        var documentBuilder = ElementAdapter.newDocumentBuilder();

        Document document;
        try {
            document = documentBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException exception) {
            throw new RuntimeException(exception);
        }

        var templateEncoder = new TemplateEncoder(getClass(), "embedded-xml.txt");

        var writer = new StringWriter();

        templateEncoder.write(mapOf(
            entry("xml", document)
        ), writer);

        assertEquals(xml, writer.toString());
    }

    @Test
    public void testInvalidPath1() {
        var templateEncoder = new TemplateEncoder(getClass(), "invalid-path.txt");

        var root = mapOf(
            entry("a", "xyz")
        );

        assertThrows(IllegalArgumentException.class, () -> templateEncoder.write(root, new StringWriter()));
    }

    @Test
    public void testInvalidPath2() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "invalid-path.txt");

        var root = mapOf(
            entry("a", null)
        );

        var writer = new StringWriter();

        templateEncoder.write(root, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testUppercaseModifier() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass(), "upper.txt");

        templateEncoder.bind("upper", (value, argument, locale, timeZone) -> value.toString().toUpperCase(locale));

        var writer = new StringWriter();

        templateEncoder.write("hello", writer);

        assertEquals("HELLO", writer.toString());
    }

    @Test
    public void testInvalidModifier() {
        var templateEncoder = new TemplateEncoder(getClass(), "invalid-modifier.txt");

        var root = mapOf(
            entry("a", "xyz")
        );

        assertThrows(IOException.class, () -> templateEncoder.write(root, new StringWriter()));
    }
}
