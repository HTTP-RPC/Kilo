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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import static org.httprpc.WebService.mapOf;
import static org.httprpc.WebService.entry;

public class TemplateSerializerTest {
    @Test
    public void testDictionary() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(WebService.class, "dictionary.txt");

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 42),
            entry("c", false)
        );

        String result;
        try (StringWriter writer = new StringWriter()) {
            templateSerializer.writeValue(new PrintWriter(writer), dictionary);
            result = writer.toString();
        }

        Assert.assertEquals(String.format("{a=%s,b=%s,c=%s,d=}",
            dictionary.get("a"),
            dictionary.get("b"),
            dictionary.get("c")), result);
    }

    @Test
    public void testSingleElementSection() throws IOException {
        TemplateSerializer templateSerializer = new TemplateSerializer(WebService.class, "section.txt");

        Map<String, ?> dictionary = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        List<?> list = Arrays.asList(dictionary);

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
        TemplateSerializer templateSerializer = new TemplateSerializer(WebService.class, "section.txt");

        Map<String, ?> dictionary1 = mapOf(
            entry("a", "hello"),
            entry("b", 1L),
            entry("c", 2.0));

        Map<String, ?> dictionary2 = mapOf(
            entry("a", "goodbye"),
            entry("b", 2L),
            entry("c", 4.0));

        List<?> list = Arrays.asList(dictionary1, dictionary2);

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
}
