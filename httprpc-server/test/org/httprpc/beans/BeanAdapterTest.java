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

package org.httprpc.beans;

import org.httprpc.beans.BeanAdapter;
import org.httprpc.AbstractTest;
import org.httprpc.JSONDecoder;
import org.httprpc.JSONEncoder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

public class BeanAdapterTest extends AbstractTest {
    @Test
    public void testBeanAdapter1() {
        Assert.assertEquals(mapOf(
            entry("long", 2L),
            entry("double", 4.0),
            entry("string", "abc"),
            entry("date", new Date(0)),
            entry("localDate", LocalDate.parse("2018-06-28")),
            entry("localTime", LocalTime.parse("10:45")),
            entry("localDateTime", LocalDateTime.parse("2018-06-28T10:45")),
            entry("list", listOf(2L, 4.0, mapOf(
                entry("flag", true)
            ))),
            entry("nestedBeanList", listOf(mapOf(
                entry("flag", true)
            ))),
            entry("map", mapOf(
                entry("long", 2L),
                entry("double", 4.0),
                entry("nestedBean", mapOf(
                    entry("flag", true)
                )))
            ),
            entry("nestedBeanMap", mapOf(
                entry("nestedBean", mapOf(
                    entry("flag", true)
                )))
            ),
            entry("nestedBean", mapOf(
                entry("flag", true)
            ))
        ), new BeanAdapter(new TestBean()));

        Assert.assertEquals(listOf(2L, 4.0, mapOf(
            entry("flag", true)
        )), BeanAdapter.adapt(listOf(2L, 4.0, new TestBean.NestedBean())));

        Assert.assertEquals(mapOf(
            entry("long", 2L),
            entry("double", 4.0),
            entry("nestedBean", mapOf(
                entry("flag", true)
            ))
        ), BeanAdapter.adapt(mapOf(
            entry("long", 2L),
            entry("double", 4.0),
            entry("nestedBean", new TestBean.NestedBean())
        )));
    }

    @Test
    public void testBeanAdapter2() throws IOException {
        JSONDecoder jsonDecoder = new JSONDecoder();

        Map<String, ?> map1;
        try (InputStream inputStream = getClass().getResourceAsStream("test.json")) {
            map1 = jsonDecoder.readValue(inputStream);
        }

        JSONEncoder jsonEncoder = new JSONEncoder();

        String json;
        try (StringWriter jsonWriter = new StringWriter()) {
            jsonEncoder.writeValue(new BeanAdapter(BeanAdapter.adapt(map1, TestInterface.class)), jsonWriter);

            json = jsonWriter.toString();
        }

        Map<String, ?> map2;
        try (StringReader jsonReader = new StringReader(json)) {
            map2 = jsonDecoder.readValue(jsonReader);
        }

        Assert.assertEquals(map1, map2);
    }
}
