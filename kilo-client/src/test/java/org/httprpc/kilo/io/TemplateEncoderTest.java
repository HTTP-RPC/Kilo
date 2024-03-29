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

import org.httprpc.kilo.util.Collections;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplateEncoderTest {
    @Test
    public void testNull() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("dictionary.txt"));

        var writer = new StringWriter();

        templateEncoder.write(null, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testMap() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("dictionary.txt"));

        var dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 42),
            entry("c", mapOf(
                entry("d", false)
            ))
        );

        var writer = new StringWriter();

        templateEncoder.write(dictionary, writer);

        assertEquals(String.format("{a=%s,b=%s,c/d=%s,e=,f/g=}",
            dictionary.get("a"),
            dictionary.get("b"),
            Collections.valueAt(dictionary, "c", "d")), writer.toString());
    }

    @Test
    public void testPath() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("path.txt"));

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
    public void testConditionalSection1() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection2a() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf(1))), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSection2b() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf())), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection3a() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "A")), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSection3b() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "")), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection4a() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", true)), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSection4b() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", false)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection5() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", true)), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testEmptyRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("list", listOf())), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testSingleElementRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        var dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("list", listOf(dictionary))), writer);

        assertEquals(String.format("[{a=%s,b=%s,c=%s}]",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), writer.toString());
    }

    @Test
    public void testMultiElementRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        var dictionary1 = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        var dictionary2 = mapOf(
            entry("a", "goodbye"),
            entry("b", 2L),
            entry("c", 4.0));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("list", listOf(dictionary1, dictionary2))), writer);

        assertEquals(String.format("[{a=%s,b=%s,c=%s}{a=%s,b=%s,c=%s}]",
            dictionary1.get("a"),
            dictionary1.get("b"),
            dictionary1.get("c"),
            dictionary2.get("a"),
            dictionary2.get( "b"),
            dictionary2.get("c")), writer.toString());
    }

    @Test
    public void testNestedRepeatingSection1() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating2.txt"));

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
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        var value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("[[[hello]]]", writer.toString());
    }

    @Test
    public void testNestedEmptyRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        var writer = new StringWriter();

        templateEncoder.write(listOf(), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testPrimitiveRepeatingSection() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating4.txt"));

        var value = listOf("hello", 42, false);

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("[(hello)(42)(false)]", writer.toString());
    }

    @Test
    public void testRepeatingSectionSeparator() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating5.txt"));

        var value = listOf("a", "b", "c");

        var writer = new StringWriter();

        templateEncoder.write(value, writer);

        assertEquals("a,b,c", writer.toString());
    }

    @Test
    public void testMapRepeatingSection1() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating6.txt"));

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
        var templateEncoder = new TemplateEncoder(getClass().getResource("repeating7.txt"));

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
    public void testInvertedSection1() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSection2a() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf(1))), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSection2b() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", listOf())), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSection3a() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "A")), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSection3b() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", "")), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSection4a() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", true)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSection4b() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(entry("a", false)), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testResources() throws IOException {
        var url = getClass().getResource("resource1.txt");
        var resourceBundle = ResourceBundle.getBundle(getClass().getPackageName() + ".resource");

        var templateEncoder = new TemplateEncoder(url, resourceBundle);

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("A1B2c", writer.toString());
    }

    @Test
    public void testInheritance() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("inheritance.txt"));

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
        var templateEncoder = new TemplateEncoder(getClass().getResource("comment.txt"));

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("><", writer.toString());
    }

    @Test
    public void testFloatFormatModifier() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("format1.txt"));

        var writer = new StringWriter();

        templateEncoder.write(4.5, writer);

        assertEquals("4.50", writer.toString());
    }

    @Test
    public void testDateFormatModifiers() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("format2.txt"));

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

        assertEquals(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(LocalDateTime.of(localDate, LocalTime.MIDNIGHT)) + ",\n"
            + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalDateTime.of(LocalDate.now(), localTime)) + ",\n"
            + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(localDateTime) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(zonedLocalDate) + ",\n"
            + DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).format(zonedLocalTime) + ",\n"
            + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).format(zonedLocalDateTime), writer.toString());
    }

    @Test
    public void testDefaultContentType() throws IOException {
        var url = getClass().getResource("xml.txt");
        var resourceBundle = ResourceBundle.getBundle(getClass().getPackageName() + ".test");

        var templateEncoder = new TemplateEncoder(url, resourceBundle);

        var writer = new StringWriter();

        templateEncoder.write(mapOf(
            entry("b", "a<b>c&d\"e")
        ), writer);

        assertEquals("<?xml version=\"1.0\"?><a b=\"a&lt;b&gt;c&amp;d&quot;e\">f&lt;g&gt;h&amp;i&quot;j</a>", writer.toString());
    }

    @Test
    public void testJSONContentType() throws IOException {
        var url = getClass().getResource("json.txt");
        var resourceBundle = ResourceBundle.getBundle(getClass().getPackageName() + ".test");

        var templateEncoder = new TemplateEncoder(url, resourceBundle);

        templateEncoder.setContentType(TemplateEncoder.ContentType.JSON);

        var writer = new StringWriter();

        templateEncoder.write(mapOf(
            entry("b", mapOf(
                entry("c", 1)
            ))
        ), writer);

        assertEquals("{\"b\": {\"c\":1}, \"d\": \"\\\"klm\\n\\\"\"}", writer.toString());
    }

    @Test
    public void testCSVContentType() throws IOException {
        var url = getClass().getResource("csv.txt");
        var resourceBundle = ResourceBundle.getBundle(getClass().getPackageName() + ".test");

        var templateEncoder = new TemplateEncoder(url, resourceBundle);

        templateEncoder.setContentType(TemplateEncoder.ContentType.CSV);

        var writer = new StringWriter();

        templateEncoder.write(mapOf(
            entry("g", listOf(
                mapOf(
                    entry("h", 1),
                    entry("i", true)
                )
            ))
        ), writer);

        assertEquals("\"\"\"n,\top\"\"\",f\n1,true\n", writer.toString());
    }

    @Test
    public void testUnspecifiedContentType() throws IOException {
        var url = getClass().getResource("unspecified.txt");
        var resourceBundle = ResourceBundle.getBundle(getClass().getPackageName() + ".test");

        var templateEncoder = new TemplateEncoder(url, resourceBundle);

        templateEncoder.setContentType(TemplateEncoder.ContentType.UNSPECIFIED);

        var writer = new StringWriter();

        templateEncoder.write(mapOf(), writer);

        assertEquals("f<g>h&i\"j\"klm\n\"\"n,\top\"", writer.toString());
    }

    @Test
    public void testSimpleInclude() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("master1.txt"));

        var writer = new StringWriter();

        templateEncoder.write("hello", writer);

        assertEquals("(hello)", writer.toString());
    }

    @Test
    public void testSectionInclude() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("master2.txt"));

        var writer = new StringWriter();

        templateEncoder.write(listOf("a", "b", "c"), writer);

        assertEquals("[(a)(b)(c)]", writer.toString());
    }

    @Test
    public void testRecursion() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

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
        var templateEncoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

        var list = listOf();

        var writer = new StringWriter();

        templateEncoder.write(list, writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testInvalidPath1() {
        var templateEncoder = new TemplateEncoder(getClass().getResource("invalid-path.txt"));

        var root = mapOf(
            entry("a", "xyz")
        );

        assertThrows(IllegalArgumentException.class, () -> templateEncoder.write(root, new StringWriter()));
    }

    @Test
    public void testInvalidPath2() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("invalid-path.txt"));

        var root = mapOf(
            entry("a", null)
        );

        var writer = new StringWriter();

        templateEncoder.write(root, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testUppercaseModifier() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("upper.txt"));

        templateEncoder.bind("upper", (value, argument, locale, timeZone) -> value.toString().toUpperCase(locale));

        var writer = new StringWriter();

        templateEncoder.write("hello", writer);

        assertEquals("HELLO", writer.toString());
    }

    @Test
    public void testInvalidModifier() {
        var templateEncoder = new TemplateEncoder(getClass().getResource("invalid-modifier.txt"));

        var root = mapOf(
            entry("a", "xyz")
        );

        assertThrows(IOException.class, () -> templateEncoder.write(root, new StringWriter()));
    }
}
