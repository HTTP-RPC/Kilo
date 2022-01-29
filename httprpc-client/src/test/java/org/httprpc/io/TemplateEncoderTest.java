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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateEncoderTest {
    @Test
    public void testNull() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("dictionary.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(null, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testMap() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("dictionary.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 42),
            entry("c", mapOf(
                entry("d", false)
            ))
        );

        StringWriter writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals(String.format("{a=%s,b=%s,c/d=%s,e=,f/g=}",
            dictionary.get("a"),
            dictionary.get("b"),
            ((Map<?, ?>)dictionary.get("c")).get("d")), writer.toString());
    }

    @Test
    public void testPath() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("path.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", mapOf(
                entry("b", 0),
                entry("d", mapOf(
                    entry("e", 1)
                ))
            ))
        );

        StringWriter writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals("1", writer.toString());
    }

    @Test
    public void testConditionalSection1() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection2() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(entry("a", emptyList())), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection3() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(entry("a", "A")), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testEmptyRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(entry("list", emptyList())), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testSingleElementRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        List<?> list = listOf(dictionary);

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(entry("list", list)), writer);

        assertEquals(String.format("[{a=%s,b=%s,c=%s}]",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), writer.toString());
    }

    @Test
    public void testMultiElementRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        Map<String, ?> dictionary1 = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        Map<String, ?> dictionary2 = mapOf(
            entry("a", "goodbye"),
            entry("b", 2L),
            entry("c", 4.0));

        List<?> list = listOf(dictionary1, dictionary2);

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(entry("list", list)), writer);

        assertEquals(String.format("[{a=%s,b=%s,c=%s}{a=%s,b=%s,c=%s}]",
            dictionary1.get("a"),
            dictionary1.get("b"),
            dictionary1.get("c"),
            dictionary2.get("a"),
            dictionary2.get("b"),
            dictionary2.get("c")), writer.toString());
    }

    @Test
    public void testNestedRepeatingSection1() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating2.txt"));

        Map<String, ?> dictionary = mapOf(
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

        StringWriter writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals("{abc=ABC,list1=[{def=DEF,list2=[{one=1,two=2,three=3}]]}", writer.toString());
    }

    @Test
    public void testNestedRepeatingSection2() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        List<?> value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        StringWriter writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("[[[hello]]]", writer.toString());
    }

    @Test
    public void testNestedEmptyRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(emptyList(), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testPrimitiveRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating4.txt"));

        List<?> value = listOf("hello", 42, false);

        StringWriter writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("[(hello)(42)(false)]", writer.toString());
    }

    @Test
    public void testRepeatingSectionSeparator() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating5.txt"));

        List<?> value = listOf("a", "b", "c");

        StringWriter writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("a,b,c", writer.toString());
    }

    @Test
    public void testMapRepeatingSection1() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating6.txt"));

        Map<String, ?> value = mapOf(
            entry("entries", mapOf(
                entry("one", mapOf(entry("value", 1))),
                entry("two", mapOf(entry("value", 2))),
                entry("three", mapOf(entry("value", 3)))
            ))
        );

        StringWriter writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("one:1,two:2,three:3", writer.toString());
    }

    @Test
    public void testMapRepeatingSection2() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating7.txt"));

        Map<String, ?> value = mapOf(
            entry("entries", mapOf(
                entry("a", "A"),
                entry("b", "B"),
                entry("c", "C")
            ))
        );

        StringWriter writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("a:A,b:B,c:C", writer.toString());
    }

    @Test
    public void testInvertedSection1() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSection2() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(entry("a", emptyList())), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSection3() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(mapOf(entry("a", "A")), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInheritance() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inheritance.txt"));

        Map<String, ?> dictionary = mapOf(
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
                ))
            ))
        );

        StringWriter writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals("AC1,AC2,AC3 ACone,ACtwo,ACthree", writer.toString());
    }

    @Test
    public void testComment() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("comment.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(emptyMap(), writer);

        assertEquals("><", writer.toString());
    }

    @Test
    public void testFloatFormatModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("format1.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(4.5, writer);

        assertEquals("4.50", writer.toString());
    }

    @Test
    public void testDateFormatModifiers() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("format2.txt"));

        Date date = new Date();
        Instant instant = date.toInstant();
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        LocalDateTime localDateTime = LocalDateTime.now();


        StringWriter writer = new StringWriter();

        encoder.write(mapOf(
            entry("timestamp", date.getTime()),
            entry("date", date),
            entry("instant", instant),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime)
        ), writer);

        ZonedDateTime now = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        assertEquals(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(localDate) + ",\n"
            + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(localTime) + ",\n"
            + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(localDateTime), writer.toString());
    }

    @Test
    public void testURLEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("url.txt"));

        StringWriter writer = new StringWriter();

        encoder.write("abc:def&xyz", writer);

        assertEquals("abc%3Adef%26xyz", writer.toString());
    }

    @Test
    public void testJSONEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("json.txt"));

        StringWriter writer = new StringWriter();

        encoder.write("\"\\\b\f\n\r\t", writer);

        assertEquals("\\\"\\\\\\b\\f\\n\\r\\t", writer.toString());
    }

    @Test
    public void testCSVEscapeModifierNumber() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("csv.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(10, writer);

        assertEquals("10", writer.toString());
    }

    @Test
    public void testCSVEscapeModifierString() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("csv.txt"));

        StringWriter writer = new StringWriter();

        encoder.write("a\"b\"c", writer);

        assertEquals("\"a\"\"b\"\"c\"", writer.toString());
    }

    @Test
    public void testMarkupEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("html.txt"));

        StringWriter writer = new StringWriter();

        encoder.write("a<b>c&d\"e", writer);

        assertEquals("a&lt;b&gt;c&amp;d&quot;e", writer.toString());
    }

    @Test
    public void testSimpleInclude() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("master1.txt"));

        StringWriter writer = new StringWriter();

        encoder.write("hello", writer);

        assertEquals("(hello)", writer.toString());
    }

    @Test
    public void testSectionInclude() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("master2.txt"));

        StringWriter writer = new StringWriter();

        encoder.write(listOf("a", "b", "c"), writer);

        assertEquals("[(a)(b)(c)]", writer.toString());
    }

    @Test
    public void testRecursion() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

        List<?> list = listOf(
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

        StringWriter writer = new StringWriter();

        encoder.write(list, writer);

        assertEquals("[[[][]][[][][]][[]]]", writer.toString());
    }

    @Test
    public void testEmptyRecursion() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

        List<?> list = emptyList();

        StringWriter writer = new StringWriter();

        encoder.write(list, writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testUppercaseModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("upper.txt"));

        encoder.getModifiers().put("case", (value, argument, locale, timeZone) -> {
            String result = value.toString();

            if (argument != null) {
                if (argument.equals("upper")) {
                    result = result.toUpperCase(locale);
                } else if (argument.equals("lower")) {
                    result = result.toLowerCase(locale);
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            return result;
        });

        StringWriter writer = new StringWriter();

        encoder.write("abcdefg", writer);

        assertEquals("ABCDEFG", writer.toString());
    }

    @Test
    public void testEscape() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("escape.txt"));

        Map<String, ?> value = mapOf(
            entry("a", mapOf(
                entry("b/\\c", "A")
            ))
        );

        StringWriter writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("A", writer.toString());
    }
}
