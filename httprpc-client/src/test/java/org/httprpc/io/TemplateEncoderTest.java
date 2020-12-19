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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(null, writer);
            result = writer.toString();
        }

        assertEquals("", result);
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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(dictionary, writer);
            result = writer.toString();
        }

        assertEquals(String.format("{a=%s,b=%s,c/d=%s,e=,f/g=}",
            dictionary.get("a"),
            dictionary.get("b"),
            ((Map<?, ?>)dictionary.get("c")).get("d")), result);
    }

    @Test
    public void testConditionalSection1() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        Map<String, ?> value = mapOf(
            entry("a", "A"),
            entry("b", "B")
        );

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(value, writer);
            result = writer.toString();
        }

        assertEquals("12", result);
    }

    @Test
    public void testEmptyRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(entry("list", emptyList())), writer);
            result = writer.toString();
        }

        assertEquals("[]", result);
    }

    @Test
    public void testSingleElementRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        List<?> list = listOf(dictionary);

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(entry("list", list)), writer);
            result = writer.toString();
        }

        assertEquals(String.format("[{a=%s,b=%s,c=%s}]",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), result);
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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(entry("list", list)), writer);
            result = writer.toString();
        }

        assertEquals(String.format("[{a=%s,b=%s,c=%s}{a=%s,b=%s,c=%s}]",
            dictionary1.get("a"),
            dictionary1.get("b"),
            dictionary1.get("c"),
            dictionary2.get("a"),
            dictionary2.get("b"),
            dictionary2.get("c")), result);
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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(dictionary, writer);
            result = writer.toString();
        }

        assertEquals("{abc=ABC,list1=[{def=DEF,list2=[{one=1,two=2,three=3}]]}", result);
    }

    @Test
    public void testNestedRepeatingSection2() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        List<?> value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(value, writer);
            result = writer.toString();
        }

        assertEquals("[[[hello]]]", result);
    }

    @Test
    public void testNestedEmptyRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(emptyList(), writer);
            result = writer.toString();
        }

        assertEquals("[]", result);
    }

    @Test
    public void testPrimitiveRepeatingSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating4.txt"));

        List<?> value = listOf("hello", 42, false);

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(value, writer);
            result = writer.toString();
        }

        assertEquals("[(hello)(42)(false)]", result);
    }

    @Test
    public void testRepeatingSectionSeparator() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("repeating5.txt"));

        List<?> value = listOf("a", "b", "c");

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(value, writer);
            result = writer.toString();
        }

        assertEquals("a,b,c", result);
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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(value, writer);
            result = writer.toString();
        }

        assertEquals("one:1,two:2,three:3", result);
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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(value, writer);
            result = writer.toString();
        }

        assertEquals("a:A,b:B,c:C", result);
    }

    @Test
    public void testInvertedSection1() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(), writer);
            result = writer.toString();
        }

        assertEquals("not found", result);
    }

    @Test
    public void testInvertedSection2() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(entry("a", emptyList())), writer);
            result = writer.toString();
        }

        assertEquals("not found", result);
    }

    @Test
    public void testInvertedSection3() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(entry("a", emptyMap())), writer);
            result = writer.toString();
        }

        assertEquals("not found", result);
    }

    @Test
    public void testInvertedSection4() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(entry("a", "A")), writer);
            result = writer.toString();
        }

        assertEquals("", result);
    }

    @Test
    public void testParentContext() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("parent.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "A"),
            entry("b", mapOf(
                entry("c", "C")
            )),
            entry("list", listOf(1, 2, 3)),
            entry("map", mapOf(
                entry("x", "one"),
                entry("y", "two"),
                entry("z", "three")
            ))
        );

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(dictionary, writer);
            result = writer.toString();
        }

        assertEquals("AC1,AC2,AC3 ACone,ACtwo,ACthree", result);
    }

    @Test
    public void testComment() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("comment.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(emptyMap(), writer);
            result = writer.toString();
        }

        assertEquals("><", result);
    }

    @Test
    public void testFloatFormatModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("format1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(4.5, writer);
            result = writer.toString();
        }

        assertEquals("4.50", result);
    }

    @Test
    public void testDateFormatModifiers() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("format2.txt"));

        Date date = new Date();
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        LocalDateTime localDateTime = LocalDateTime.now();

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(mapOf(
                entry("timestamp", date.getTime()),
                entry("date", date),
                entry("localDate", localDate),
                entry("localTime", localTime),
                entry("localDateTime", localDateTime)
            ), writer);
            result = writer.toString();
        }

        ZonedDateTime now = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        assertEquals(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(now) + ",\n"
            + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now) + ",\n"
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(localDate) + ",\n"
            + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(localTime) + ",\n"
            + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(localDateTime), result);
    }

    @Test
    public void testURLEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("url.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write("abc:def&xyz", writer);
            result = writer.toString();
        }

        assertEquals("abc%3Adef%26xyz", result);
    }

    @Test
    public void testJSONEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("json.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write("\"\\\b\f\n\r\t", writer);
            result = writer.toString();
        }

        assertEquals("\\\"\\\\\\b\\f\\n\\r\\t", result);
    }

    @Test
    public void testCSVEscapeModifierNumber() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("csv.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(10, writer);
            result = writer.toString();
        }

        assertEquals("10", result);
    }

    @Test
    public void testCSVEscapeModifierString() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("csv.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write("a\"b\"c", writer);
            result = writer.toString();
        }

        assertEquals("\"a\"\"b\"\"c\"", result);
    }

    @Test
    public void testMarkupEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("html.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write("a<b>c&d\"e", writer);
            result = writer.toString();
        }

        assertEquals("a&lt;b&gt;c&amp;d&quot;e", result);
    }

    @Test
    public void testSimpleInclude() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("master1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write("hello", writer);
            result = writer.toString();
        }

        assertEquals("(hello)", result);
    }

    @Test
    public void testSectionInclude() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("master2.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(listOf("a", "b", "c"), writer);
            result = writer.toString();
        }

        assertEquals("[(a)(b)(c)]", result);
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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(list, writer);
            result = writer.toString();
        }

        assertEquals("[[[][]][[][][]][[]]]", result);
    }

    @Test
    public void testEmptyRecursion() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

        List<?> list = emptyList();

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(list, writer);
            result = writer.toString();
        }

        assertEquals("[]", result);
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

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write("abcdefg", writer);
            result = writer.toString();
        }

        assertEquals("ABCDEFG", result);
    }

    @Test
    public void testEscape() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("escape.txt"));

        Map<String, ?> value = mapOf(
            entry("a", mapOf(
                entry("b/\\c", "A")
            ))
        );

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.write(value, writer);
            result = writer.toString();
        }

        assertEquals("A", result);
    }
}
