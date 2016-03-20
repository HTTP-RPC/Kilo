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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import static org.httprpc.WebService.listOf;
import static org.httprpc.WebService.mapOf;
import static org.httprpc.WebService.entry;

public class TemplateSerializerTest {
    private static final String PLAIN_TEXT_MIME_TYPE = "text/plain";

    @Test
    public void testNull() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "dictionary.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), null);
            result = writer.toString();
        }

        Assert.assertEquals("", result);
    }

    @Test
    public void testDictionary() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "dictionary.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 42),
            entry("c", mapOf(
                entry("d", false)
            ))
        );

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), dictionary);
            result = writer.toString();
        }

        Assert.assertEquals(String.format("{a=%s,b=%s,c.d=%s,e=,f.g=}",
            dictionary.get("a"),
            dictionary.get("b"),
            ((Map<?, ?>)dictionary.get("c")).get("d")), result);
    }

    @Test
    public void testEmptySection() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "section1.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), mapOf(entry("list", Collections.emptyList())));
            result = writer.toString();
        }

        Assert.assertEquals("[]", result);
    }

    @Test
    public void testSingleElementSection() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "section1.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        List<?> list = listOf(dictionary);

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), mapOf(entry("list", list)));
            result = writer.toString();
        }

        Assert.assertEquals(String.format("[{a=%s,b=%s,c=%s}]",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), result);
    }

    @Test
    public void testMultiElementSection() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "section1.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

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
            templateSerializer.writeValue(new PrintWriter(writer), mapOf(entry("list", list)));
            result = writer.toString();
        }

        Assert.assertEquals(String.format("[{a=%s,b=%s,c=%s}{a=%s,b=%s,c=%s}]",
            dictionary1.get("a"),
            dictionary1.get("b"),
            dictionary1.get("c"),
            dictionary2.get("a"),
            dictionary2.get("b"),
            dictionary2.get("c")), result);
    }

    @Test
    public void testNestedSection1() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "section2.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

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
            templateSerializer.writeValue(new PrintWriter(writer), dictionary);
            result = writer.toString();
        }

        Assert.assertEquals("{abc=ABC,list1=[{def=DEF,list2=[{one=1,two=2,three=3}]]}", result);
    }

    @Test
    public void testNestedSection2() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "section3.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        List<?> value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), value);
            result = writer.toString();
        }

        Assert.assertEquals("[[[hello]]]", result);
    }

    @Test
    public void testNestedEmptySection() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "section3.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), Collections.emptyList());
            result = writer.toString();
        }

        Assert.assertEquals("[]", result);
    }

    @Test
    public void testPrimitiveSection() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "section4.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        List<?> value = listOf("hello", 42, false);

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), value);
            result = writer.toString();
        }

        Assert.assertEquals("[(hello)(42)(false)]", result);
    }

    @Test
    public void testComment() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "comment.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), Collections.emptyMap());
            result = writer.toString();
        }

        Assert.assertEquals("><", result);
    }

    @Test
    public void testFormatModifier() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "format.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), 4.5);
            result = writer.toString();
        }

        Assert.assertEquals("4.50", result);
    }

    @Test
    public void testURLEscapeModifier() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "url.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), "abc:def&xyz");
            result = writer.toString();
        }

        Assert.assertEquals("abc%3Adef%26xyz", result);
    }

    @Test
    public void testMarkupEscapeModifier() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "markup.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), "a<b>c&d\"e");
            result = writer.toString();
        }

        Assert.assertEquals("a&lt;b&gt;c&amp;d&quot;e", result);
    }

    @Test
    public void testJSONEscapeModifier() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "json.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), "\"\\\b\f\n\r\t");
            result = writer.toString();
        }

        Assert.assertEquals("\\\"\\\\\\b\\f\\n\\r\\t", result);
    }

    @Test
    public void testCSVEscapeModifier() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "csv.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), "a\\b\"c");
            result = writer.toString();
        }

        Assert.assertEquals("a\\\\b\\\"c", result);
    }

    @Test
    public void testSimpleInclude() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "master1.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), "hello");
            result = writer.toString();
        }

        Assert.assertEquals("(hello)", result);
    }

    @Test
    public void testSectionInclude() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "master2.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), listOf("a", "b", "c"));
            result = writer.toString();
        }

        Assert.assertEquals("[(a)(b)(c)]", result);
    }

    @Test
    public void testRecursion() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "recursion.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

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
            templateSerializer.writeValue(new PrintWriter(writer), list);
            result = writer.toString();
        }

        Assert.assertEquals("[[[][]][[][][]][[]]]", result);
    }

    @Test
    public void testEmptyRecursion() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "recursion.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        List<?> list = Collections.EMPTY_LIST;

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), list);
            result = writer.toString();
        }

        Assert.assertEquals("[]", result);
    }

    @Test
    public void testResource() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(TemplateSerializerTest.class, "resource.txt",
            PLAIN_TEXT_MIME_TYPE, Locale.getDefault());

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), "hello");
            result = writer.toString();
        }

        Assert.assertEquals("value:hello", result);
    }
}
