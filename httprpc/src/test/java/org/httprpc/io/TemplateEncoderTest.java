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
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import org.httprpc.AbstractTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TemplateEncoderTest extends AbstractTest {
    static class CaseModifier implements TemplateEncoder.Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale) {
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
        }
    }

    static {
        TemplateEncoder.getModifiers().put("case", new CaseModifier());
    }

    @Test
    public void testNull() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("dictionary.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(null, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("", result);
    }

    @Test
    public void testDictionary() throws IOException {
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
            encoder.writeValue(dictionary, writer);
            result = writer.toString();
        }

        Assertions.assertEquals(String.format("{a=%s,b=%s,c.d=%s,e=,f.g=}",
            dictionary.get("a"),
            dictionary.get("b"),
            ((Map<?, ?>)dictionary.get("c")).get("d")), result);
    }

    @Test
    public void testEmptySection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(mapOf(entry("list", Collections.emptyList())), writer);
            result = writer.toString();
        }

        Assertions.assertEquals("[]", result);
    }

    @Test
    public void testSingleElementSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section1.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        List<?> list = listOf(dictionary);

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(mapOf(entry("list", list)), writer);
            result = writer.toString();
        }

        Assertions.assertEquals(String.format("[{a=%s,b=%s,c=%s}]",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), result);
    }

    @Test
    public void testMultiElementSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section1.txt"));

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
            encoder.writeValue(mapOf(entry("list", list)), writer);
            result = writer.toString();
        }

        Assertions.assertEquals(String.format("[{a=%s,b=%s,c=%s}{a=%s,b=%s,c=%s}]",
            dictionary1.get("a"),
            dictionary1.get("b"),
            dictionary1.get("c"),
            dictionary2.get("a"),
            dictionary2.get("b"),
            dictionary2.get("c")), result);
    }

    @Test
    public void testNestedSection1() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section2.txt"));

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
            encoder.writeValue(dictionary, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("{abc=ABC,list1=[{def=DEF,list2=[{one=1,two=2,three=3}]]}", result);
    }

    @Test
    public void testNestedSection2() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section3.txt"));

        List<?> value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(value, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("[[[hello]]]", result);
    }

    @Test
    public void testNestedEmptySection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section3.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(Collections.emptyList(), writer);
            result = writer.toString();
        }

        Assertions.assertEquals("[]", result);
    }

    @Test
    public void testPrimitiveSection() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section4.txt"));

        List<?> value = listOf("hello", 42, false);

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(value, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("[(hello)(42)(false)]", result);
    }

    @Test
    public void testSectionSeparator() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("section5.txt"));

        List<?> value = listOf("a", "b", "c");

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(value, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("a,b,c", result);
    }

    @Test
    public void testComment() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("comment.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(Collections.emptyMap(), writer);
            result = writer.toString();
        }

        Assertions.assertEquals("><", result);
    }

    @Test
    public void testFloatFormatModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("format1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(4.5, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("4.50", result);
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
            encoder.writeValue(mapOf(
                entry("date", date),
                entry("dateInMilliseconds", date.getTime()),
                entry("localDate", localDate),
                entry("localDateAsString", localDate.toString()),
                entry("localTime", localTime),
                entry("localTimeAsString", localTime.toString()),
                entry("localDateTime", localDateTime),
                entry("localDateTimeAsString", localDateTime.toString())
            ), writer);
            result = writer.toString();
        }

        Assertions.assertEquals(DateFormat.getDateInstance(DateFormat.SHORT).format(date) + ",\n"
            + DateFormat.getDateInstance(DateFormat.SHORT).format(date) + ",\n"
            + localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + ",\n"
            + localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)) + ",\n"
            + localTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) + ",\n"
            + localTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) + ",\n"
            + localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)) + ",\n"
            + localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)), result);
    }

    @Test
    public void testURLEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("url.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue("abc:def&xyz", writer);
            result = writer.toString();
        }

        Assertions.assertEquals("abc%3Adef%26xyz", result);
    }

    @Test
    public void testHTMLEscapeModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("html.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue("a<b>c&d\"e", writer);
            result = writer.toString();
        }

        Assertions.assertEquals("a&lt;b&gt;c&amp;d&quot;e", result);
    }

    @Test
    public void testSimpleInclude() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("master1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue("hello", writer);
            result = writer.toString();
        }

        Assertions.assertEquals("(hello)", result);
    }

    @Test
    public void testSectionInclude() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("master2.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(listOf("a", "b", "c"), writer);
            result = writer.toString();
        }

        Assertions.assertEquals("[(a)(b)(c)]", result);
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
            encoder.writeValue(list, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("[[[][]][[][][]][[]]]", result);
    }

    @Test
    public void testEmptyRecursion() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("recursion.txt"));

        List<?> list = Collections.EMPTY_LIST;

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue(list, writer);
            result = writer.toString();
        }

        Assertions.assertEquals("[]", result);
    }

    @Test
    public void testResource() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("resource1.txt"));

        encoder.setBaseName(getClass().getPackage().getName() + ".resource1");

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue("hello", writer);
            result = writer.toString();
        }

        Assertions.assertEquals("value:hello", result);
    }

    @Test
    public void testMissingResourceKey() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("resource2.txt"));

        encoder.setBaseName(getClass().getPackage().getName() + ".resource2");

        try (StringWriter writer = new StringWriter()) {
            Assertions.assertThrows(MissingResourceException.class, () -> encoder.writeValue("hello", writer));
        }
    }

    @Test
    public void testMissingResourceBundle() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("resource3.txt"));

        encoder.setBaseName(getClass().getPackage().getName() + ".resource3");

        try (StringWriter writer = new StringWriter()) {
            Assertions.assertThrows(MissingResourceException.class, () -> encoder.writeValue("hello", writer));
        }
    }

    @Test
    public void testContextProperty() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("context.txt"));

        encoder.getContext().put("a", "A");

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue("B", writer);
            result = writer.toString();
        }

        Assertions.assertEquals("A/B", result);
    }

    @Test
    public void testMissingContextProperty() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("context.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue("B", writer);
            result = writer.toString();
        }

        Assertions.assertEquals("/B", result);
    }

    @Test
    public void testUppercaseModifier() throws IOException {
        TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("upper.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            encoder.writeValue("abcdefg", writer);
            result = writer.toString();
        }

        Assertions.assertEquals("ABCDEFG", result);
    }
}
