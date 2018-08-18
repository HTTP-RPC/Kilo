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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

public class BeanAdapterTest extends AbstractTest {
    @Test
    public void testBeanAdapter1() throws IOException {
        Map<String, ?> expected = mapOf(
            entry("i", 1),
            entry("long", 2L),
            entry("double", 4.0),
            entry("string", "abc"),
            entry("bigInteger", BigInteger.valueOf(8192L)),
            entry("dayOfWeek", DayOfWeek.values()[3]),
            entry("date", new Date(0)),
            entry("localDate", LocalDate.parse("2018-06-28")),
            entry("localTime", LocalTime.parse("10:45")),
            entry("localDateTime", LocalDateTime.parse("2018-06-28T10:45")),
            entry("list", listOf(2L, 4.0, mapOf(
                entry("flag", true)
            ))),
            entry("nestedBeanList", listOf(mapOf(
                entry("flag", false)
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
                    entry("flag", false)
                )))
            ),
            entry("nestedBean", mapOf(
                entry("flag", true)
            ))
        );

        JSONDecoder jsonDecoder = new JSONDecoder();

        Map<String, ?> map;
        try (InputStream inputStream = getClass().getResourceAsStream("test.json")) {
            map = jsonDecoder.readValue(inputStream);
        }

        BeanAdapter adapter = new BeanAdapter(BeanAdapter.adapt(map, TestBean.class));

        Assert.assertEquals(expected, adapter);
    }

    @Test
    public void testBeanAdapter2() throws IOException {
        JSONDecoder jsonDecoder = new JSONDecoder();

        Map<String, ?> map;
        try (InputStream inputStream = getClass().getResourceAsStream("test.json")) {
            map = jsonDecoder.readValue(inputStream);
        }

        TestInterface result = BeanAdapter.adapt(map, TestInterface.class);

        Assert.assertEquals(1, result.getInt());
        Assert.assertEquals(2L, result.getLong());
        Assert.assertEquals(4.0, result.getDouble(), 0.0);
        Assert.assertEquals("abc", result.getString());
        Assert.assertEquals(new Date(0), result.getDate());
        Assert.assertEquals(LocalDate.parse("2018-06-28"), result.getLocalDate());
        Assert.assertEquals(LocalTime.parse("10:45"), result.getLocalTime());
        Assert.assertEquals(LocalDateTime.parse("2018-06-28T10:45"), result.getLocalDateTime());

        Assert.assertEquals(2L, ((Number)result.getList().get(0)).longValue());
        Assert.assertEquals(4.0, ((Number)result.getList().get(1)).doubleValue(), 0.0);
        Assert.assertEquals(true, ((Map<?, ?>)result.getList().get(2)).get("flag"));
        Assert.assertEquals(false, result.getNestedBeanList().get(0).getFlag());

        Assert.assertEquals(2L, ((Number)result.getMap().get("long")).longValue());
        Assert.assertEquals(4.0, ((Number)result.getMap().get("double")).doubleValue(), 0.0);
        Assert.assertEquals(true, ((Map<?, ?>)result.getMap().get("nestedBean")).get("flag"));
        Assert.assertEquals(false, result.getNestedBeanMap().get("nestedBean").getFlag());

        Assert.assertEquals(true, result.getNestedBean().getFlag());
    }
}
