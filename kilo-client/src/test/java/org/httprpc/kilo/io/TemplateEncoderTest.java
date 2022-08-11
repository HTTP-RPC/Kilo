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

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TemplateEncoderTest {
    @Test
    public void testNull() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("dictionary.txt"));

        var writer = new StringWriter();

        encoder.write(null, writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testMap() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("dictionary.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 42),
            entry("c", mapOf(
                entry("d", false)
            ))
        );

        var writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals(String.format("{a=%s,b=%s,c/d=%s,e=,f/g=}",
            dictionary.get("a"),
            dictionary.get("b"),
            ((Map<?, ?>)dictionary.get("c")).get("d")), writer.toString());
    }

    @Test
    public void testPath() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("path.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", mapOf(
                entry("b", 0),
                entry("d", mapOf(
                    entry("e", 1)
                ))
            ))
        );

        var writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals("01", writer.toString());
    }

    @Test
    public void testConditionalSection1() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection2() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", listOf())), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection3() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", "A")), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testConditionalSection4() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", false)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testConditionalSection5() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("conditional.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", true)), writer);

        assertEquals("found", writer.toString());
    }

    @Test
    public void testEmptyRepeatingSection() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("list", listOf())), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testSingleElementRepeatingSection() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        List<?> list = listOf(dictionary);

        var writer = new StringWriter();

        encoder.write(mapOf(entry("list", list)), writer);

        assertEquals(String.format("[{a=%s,b=%s,c=%s}]",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), writer.toString());
    }

    @Test
    public void testMultiElementRepeatingSection() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating1.txt"));

        Map<String, ?> dictionary1 = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        Map<String, ?> dictionary2 = mapOf(
            entry("a", "goodbye"),
            entry("b", 2L),
            entry("c", 4.0));

        List<?> list = listOf(dictionary1, dictionary2);

        var writer = new StringWriter();

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
        var encoder = new TemplateEncoder(getClass().getResource("repeating2.txt"));

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

        var writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals("{abc=ABC,list1=[{def=DEF,list2=[{one=1,two=2,three=3}]]}", writer.toString());
    }

    @Test
    public void testNestedRepeatingSection2() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        List<?> value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        var writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("[[[hello]]]", writer.toString());
    }

    @Test
    public void testNestedEmptyRepeatingSection() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating3.txt"));

        var writer = new StringWriter();

        encoder.write(listOf(), writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testPrimitiveRepeatingSection() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating4.txt"));

        List<?> value = listOf("hello", 42, false);

        var writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("[(hello)(42)(false)]", writer.toString());
    }

    @Test
    public void testRepeatingSectionSeparator() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating5.txt"));

        List<?> value = listOf("a", "b", "c");

        var writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("a,b,c", writer.toString());
    }

    @Test
    public void testMapRepeatingSection1() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating6.txt"));

        Map<String, ?> value = mapOf(
            entry("entries", mapOf(
                entry("one", mapOf(entry("value", 1))),
                entry("two", mapOf(entry("value", 2))),
                entry("three", mapOf(entry("value", 3)))
            ))
        );

        var writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("one:1,two:2,three:3", writer.toString());
    }

    @Test
    public void testMapRepeatingSection2() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("repeating7.txt"));

        Map<String, ?> value = mapOf(
            entry("entries", mapOf(
                entry("a", "A"),
                entry("b", "B"),
                entry("c", "C")
            ))
        );

        var writer = new StringWriter();

        encoder.write(value, writer);

        assertEquals("a:A,b:B,c:C", writer.toString());
    }

    @Test
    public void testInvertedSection1() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSection2() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", listOf())), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInvertedSection3() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", "A")), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSection4() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", true)), writer);

        assertEquals("", writer.toString());
    }

    @Test
    public void testInvertedSection5() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("inverted.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(entry("a", false)), writer);

        assertEquals("not found", writer.toString());
    }

    @Test
    public void testInheritance() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("inheritance.txt"));

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
                )),
                entry("e", null)
            )),
            entry("e", "E")
        );

        var writer = new StringWriter();

        encoder.write(dictionary, writer);

        assertEquals("AC1,AC2,AC3 ACone,ACtwo,ACthree", writer.toString());
    }

    @Test
    public void testComment() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("comment.txt"));

        var writer = new StringWriter();

        encoder.write(mapOf(), writer);

        assertEquals("><", writer.toString());
    }

    @Test
    public void testFloatFormatModifier() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("format1.txt"));

        var writer = new StringWriter();

        encoder.write(4.5, writer);

        assertEquals("4.50", writer.toString());
    }

    @Test
    public void testDateFormatModifiers() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("format2.txt"));

        var date = new Date();
        var instant = date.toInstant();
        var localDate = LocalDate.now();
        var localTime = LocalTime.now();
        var localDateTime = LocalDateTime.now();


        var writer = new StringWriter();

        encoder.write(mapOf(
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
            + DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(localDate) + ",\n"
            + DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(localTime) + ",\n"
            + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(localDateTime), writer.toString());
    }

    @Test
    public void testURLEscapeModifier() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("url.txt"));

        var writer = new StringWriter();

        encoder.write("abc:def&xyz", writer);

        assertEquals("abc%3Adef%26xyz", writer.toString());
    }

    @Test
    public void testJSONEscapeModifier() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("json.txt"));

        var writer = new StringWriter();

        encoder.write("\"\\\b\f\n\r\t", writer);

        assertEquals("\\\"\\\\\\b\\f\\n\\r\\t", writer.toString());
    }

    @Test
    public void testCSVEscapeModifierNumber() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("csv.txt"));

        var writer = new StringWriter();

        encoder.write(10, writer);

        assertEquals("10", writer.toString());
    }

    @Test
    public void testCSVEscapeModifierString() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("csv.txt"));

        var writer = new StringWriter();

        encoder.write("a\"b\"c", writer);

        assertEquals("\"a\"\"b\"\"c\"", writer.toString());
    }

    @Test
    public void testMarkupEscapeModifier() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("html.txt"));

        var writer = new StringWriter();

        encoder.write("a<b>c&d\"e", writer);

        assertEquals("a&lt;b&gt;c&amp;d&quot;e", writer.toString());
    }

    @Test
    public void testSimpleInclude() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("master1.txt"));

        var writer = new StringWriter();

        encoder.write("hello", writer);

        assertEquals("(hello)", writer.toString());
    }

    @Test
    public void testSectionInclude() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("master2.txt"));

        var writer = new StringWriter();

        encoder.write(listOf("a", "b", "c"), writer);

        assertEquals("[(a)(b)(c)]", writer.toString());
    }

    @Test
    public void testRecursion() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

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

        var writer = new StringWriter();

        encoder.write(list, writer);

        assertEquals("[[[][]][[][][]][[]]]", writer.toString());
    }

    @Test
    public void testEmptyRecursion() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

        List<?> list = listOf();

        var writer = new StringWriter();

        encoder.write(list, writer);

        assertEquals("[]", writer.toString());
    }

    @Test
    public void testInvalidMapValue() {
        var encoder = new TemplateEncoder(getClass().getResource("invalid.txt"));

        Map<String, ?> root = mapOf(
            entry("a", "xyz")
        );

        assertThrows(IllegalArgumentException.class, () -> encoder.write(root, new StringWriter()));
    }

    @Test
    public void testUppercaseModifier() throws IOException {
        var encoder = new TemplateEncoder(getClass().getResource("upper.txt"));

        encoder.getModifiers().put("case", (value, argument, locale, timeZone) -> {
            var result = value.toString();

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

        var writer = new StringWriter();

        encoder.write("abcdefg", writer);

        assertEquals("ABCDEFG", writer.toString());
    }
}
