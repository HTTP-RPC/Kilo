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

package org.httprpc.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Test;

public class TemplateEngineTest {
    @Test
    public void testNull() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("dictionary.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, null);
            result = writer.toString();
        }

        Assert.assertEquals("", result);
    }

    @Test
    public void testDictionary() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("dictionary.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 42),
            entry("c", mapOf(
                entry("d", false)
            ))
        );

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, dictionary);
            result = writer.toString();
        }

        Assert.assertEquals(String.format("{a=%s,b=%s,c.d=%s,e=,f.g=}",
            dictionary.get("a"),
            dictionary.get("b"),
            ((Map<?, ?>)dictionary.get("c")).get("d")), result);
    }

    @Test
    public void testEmptySection() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("section1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, mapOf(entry("list", Collections.emptyList())));
            result = writer.toString();
        }

        Assert.assertEquals("[]", result);
    }

    @Test
    public void testSingleElementSection() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("section1.txt"));

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        List<?> list = listOf(dictionary);

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, mapOf(entry("list", list)));
            result = writer.toString();
        }

        Assert.assertEquals(String.format("[{a=%s,b=%s,c=%s}]",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), result);
    }

    @Test
    public void testMultiElementSection() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("section1.txt"));

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
            engine.writeObject(writer, mapOf(entry("list", list)));
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
        TemplateEngine engine = new TemplateEngine(getClass().getResource("section2.txt"));

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
            engine.writeObject(writer, dictionary);
            result = writer.toString();
        }

        Assert.assertEquals("{abc=ABC,list1=[{def=DEF,list2=[{one=1,two=2,three=3}]]}", result);
    }

    @Test
    public void testNestedSection2() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("section3.txt"));

        List<?> value = listOf(listOf(listOf(mapOf(entry("a", "hello")))));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, value);
            result = writer.toString();
        }

        Assert.assertEquals("[[[hello]]]", result);
    }

    @Test
    public void testNestedEmptySection() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("section3.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, Collections.emptyList());
            result = writer.toString();
        }

        Assert.assertEquals("[]", result);
    }

    @Test
    public void testPrimitiveSection() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("section4.txt"));

        List<?> value = listOf("hello", 42, false);

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, value);
            result = writer.toString();
        }

        Assert.assertEquals("[(hello)(42)(false)]", result);
    }

    @Test
    public void testComment() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("comment.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, Collections.emptyMap());
            result = writer.toString();
        }

        Assert.assertEquals("><", result);
    }

    @Test
    public void testFormatModifier() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("format.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, 4.5);
            result = writer.toString();
        }

        Assert.assertEquals("4.50", result);
    }

    @Test
    public void testURLEscapeModifier() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("url.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "abc:def&xyz");
            result = writer.toString();
        }

        Assert.assertEquals("abc%3Adef%26xyz", result);
    }

    @Test
    public void testMarkupEscapeModifier() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("markup.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "a<b>c&d\"e");
            result = writer.toString();
        }

        Assert.assertEquals("a&lt;b&gt;c&amp;d&quot;e", result);
    }

    @Test
    public void testJSONEscapeModifier() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("json.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "\"\\\b\f\n\r\t");
            result = writer.toString();
        }

        Assert.assertEquals("\\\"\\\\\\b\\f\\n\\r\\t", result);
    }

    @Test
    public void testCSVEscapeModifier() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("csv.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "a\\b\"c");
            result = writer.toString();
        }

        Assert.assertEquals("a\\\\b\\\"c", result);
    }

    @Test
    public void testSimpleInclude() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("master1.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "hello");
            result = writer.toString();
        }

        Assert.assertEquals("(hello)", result);
    }

    @Test
    public void testSectionInclude() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("master2.txt"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, listOf("a", "b", "c"));
            result = writer.toString();
        }

        Assert.assertEquals("[(a)(b)(c)]", result);
    }

    @Test
    public void testRecursion() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("recursion.txt"));

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
            engine.writeObject(writer, list);
            result = writer.toString();
        }

        Assert.assertEquals("[[[][]][[][][]][[]]]", result);
    }

    @Test
    public void testEmptyRecursion() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("recursion.txt"));

        List<?> list = Collections.EMPTY_LIST;

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, list);
            result = writer.toString();
        }

        Assert.assertEquals("[]", result);
    }

    @Test
    public void testResource() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("resource1.txt"),
            ResourceBundle.getBundle(getClass().getPackage().getName() + ".resource1"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "hello");
            result = writer.toString();
        }

        Assert.assertEquals("value:hello", result);
    }

    @Test
    public void testMissingResourceKey() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("resource2.txt"),
            ResourceBundle.getBundle(getClass().getPackage().getName() + ".resource2"));

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "hello");
            result = writer.toString();
        }

        Assert.assertEquals("label:hello", result);
    }

    @Test
    public void testMissingResourceBundle() throws IOException {
        TemplateEngine engine = new TemplateEngine(getClass().getResource("resource3.txt"), null);

        String result;
        try (StringWriter writer = new StringWriter()) {
            engine.writeObject(writer, "hello");
            result = writer.toString();
        }

        Assert.assertEquals("label:hello", result);
    }

    @SafeVarargs
    private static List<?> listOf(Object...elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

    @SafeVarargs
    private static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) {
        HashMap<K, Object> map = new HashMap<>();

        for (Map.Entry<K, ?> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(map);
    }

    private static <K> Map.Entry<K, ?> entry(K key, Object value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
