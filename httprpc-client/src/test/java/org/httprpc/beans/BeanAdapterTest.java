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

import org.httprpc.io.JSONDecoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanAdapterTest {
    @Test
    public void testPrimitiveAdapt() {
        assertEquals(BeanAdapter.adapt(null, Byte.TYPE), Byte.valueOf((byte)0));
        assertEquals(BeanAdapter.adapt("1", Byte.TYPE), Byte.valueOf((byte)1));

        assertEquals(BeanAdapter.adapt(null, Short.TYPE), Short.valueOf((short)0));
        assertEquals(BeanAdapter.adapt("2", Short.TYPE), Short.valueOf((short)2));

        assertEquals(BeanAdapter.adapt(null, Integer.TYPE), Integer.valueOf(0));
        assertEquals(BeanAdapter.adapt("3", Integer.TYPE), Integer.valueOf(3));

        assertEquals(BeanAdapter.adapt(null, Long.TYPE), Long.valueOf(0));
        assertEquals(BeanAdapter.adapt("4", Long.TYPE), Long.valueOf(4));

        assertEquals(BeanAdapter.adapt(null, Float.TYPE), Float.valueOf(0));
        assertEquals(BeanAdapter.adapt("5.0", Float.TYPE), Float.valueOf(5));

        assertEquals(BeanAdapter.adapt(null, Double.TYPE), Double.valueOf(0));
        assertEquals(BeanAdapter.adapt("6.0", Double.TYPE), Double.valueOf(6));

        assertEquals(BeanAdapter.adapt(null, Boolean.TYPE), Boolean.FALSE);
        assertEquals(BeanAdapter.adapt("true", Boolean.TYPE), Boolean.TRUE);
    }

    @Test
    public void testMapAdapt() {
        Map<String, Object> map1 = new HashMap<String, Object>() {
            @Override
            public String toString() {
                return "abc";
            }
        };

        map1.put("flag", true);

        Map<String, Object> map2 = mapOf(entry("flag", true));

        TestInterface.NestedInterface nestedBean1 = BeanAdapter.adapt(map1, TestInterface.NestedInterface.class);
        TestInterface.NestedInterface nestedBean2 = BeanAdapter.adapt(map2, TestInterface.NestedInterface.class);

        assertEquals(nestedBean1, nestedBean2);
        assertEquals(map1.hashCode(), nestedBean1.hashCode());
        assertEquals(map1.toString(), nestedBean1.toString());
    }

    @Test
    public void testBeanAdapter1() throws MalformedURLException {
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
            entry("URL", new URL("http://localhost:8080")),
            entry("nestedBean", mapOf(
                entry("flag", true)
            )),
            entry("list", listOf(2L, 4.0, mapOf(
                entry("flag", true)
            ))),
            entry("testArrayList", listOf()),
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
            entry("testHashMap", mapOf()),
            entry("nestedBeanMap", mapOf(
                entry("nestedBean", mapOf(
                    entry("flag", false)
                )))
            )
        );

        Map<String, Object> actual = new BeanAdapter(new TestBean());

        assertEquals(expected, actual);
        assertNull(actual.get("ignored"));
    }

    @Test
    public void testBeanAdapter2() throws IOException {
        JSONDecoder jsonDecoder = new JSONDecoder();

        Map<String, ?> map;
        try (InputStream inputStream = getClass().getResourceAsStream("test.json")) {
            map = jsonDecoder.read(inputStream);
        }

        TestInterface result = BeanAdapter.adapt(map, TestInterface.class);

        assertNotNull(result);

        assertEquals(1, result.getInt());
        assertEquals(2L, result.getLong());
        assertEquals(4.0, result.getDouble(), 0.0);
        assertEquals("abc", result.getString());
        assertEquals(new Date(0), result.getDate());
        assertEquals(LocalDate.parse("2018-06-28"), result.getLocalDate());
        assertEquals(LocalTime.parse("10:45"), result.getLocalTime());
        assertEquals(LocalDateTime.parse("2018-06-28T10:45"), result.getLocalDateTime());

        assertTrue(result.getNestedBean().getFlag());

        assertEquals(2L, ((Number)result.getList().get(0)).longValue());
        assertEquals(4.0, ((Number)result.getList().get(1)).doubleValue(), 0.0);
        assertEquals(true, ((Map<?, ?>)result.getList().get(2)).get("flag"));
        assertFalse(result.getNestedBeanList().get(0).getFlag());

        assertEquals(2L, ((Number)result.getMap().get("long")).longValue());
        assertEquals(4.0, ((Number)result.getMap().get("double")).doubleValue(), 0.0);
        assertEquals(true, ((Map<?, ?>)result.getMap().get("nestedBean")).get("flag"));
        assertFalse(result.getNestedBeanMap().get("nestedBean").getFlag());
    }

    @Test
    public void testInterfaceKey() {
        TestInterface testInterface = BeanAdapter.adapt(mapOf(entry("i", 10)), TestInterface.class);

        Map<String, ?> map = new BeanAdapter(testInterface);

        assertEquals(10, map.get("i"));
    }

    @Test
    public void testGetProperties() {
        Map<String, Type> properties = BeanAdapter.getProperties(TestBean.class);

        assertEquals(Integer.TYPE, properties.get("i"));
        assertEquals(Long.TYPE, properties.get("long"));
        assertEquals(Double.TYPE, properties.get("double"));
        assertEquals(String.class, properties.get("string"));
        assertEquals(BigInteger.class, properties.get("bigInteger"));
        assertEquals(DayOfWeek.class, properties.get("dayOfWeek"));
        assertEquals(Date.class, properties.get("date"));
        assertEquals(LocalDate.class, properties.get("localDate"));
        assertEquals(LocalTime.class, properties.get("localTime"));
        assertEquals(LocalDateTime.class, properties.get("localDateTime"));
        assertEquals(URL.class, properties.get("URL"));
        assertEquals(TestBean.NestedBean.class, properties.get("nestedBean"));

        assertTrue(properties.get("list") instanceof ParameterizedType);

        Type[] listTypeArguments = ((ParameterizedType)properties.get("list")).getActualTypeArguments();

        assertEquals(1, listTypeArguments.length);
        assertTrue(listTypeArguments[0] instanceof WildcardType);

        assertEquals(TestBean.TestArrayList.class, properties.get("testArrayList"));

        Type[] nestedBeanListTypeArguments = ((ParameterizedType)properties.get("nestedBeanList")).getActualTypeArguments();

        assertEquals(1, nestedBeanListTypeArguments.length);
        assertEquals(TestBean.NestedBean.class, nestedBeanListTypeArguments[0]);

        assertTrue(properties.get("map") instanceof ParameterizedType);

        Type[] mapTypeArguments = ((ParameterizedType)properties.get("map")).getActualTypeArguments();

        assertEquals(2, mapTypeArguments.length);
        assertEquals(String.class, mapTypeArguments[0]);
        assertTrue(mapTypeArguments[1] instanceof WildcardType);

        assertEquals(TestBean.TestHashMap.class, properties.get("testHashMap"));

        Type[] nestedBeanMapTypeArguments = ((ParameterizedType)properties.get("nestedBeanMap")).getActualTypeArguments();

        assertEquals(2, nestedBeanMapTypeArguments.length);
        assertEquals(String.class, nestedBeanMapTypeArguments[0]);
        assertEquals(TestBean.NestedBean.class, nestedBeanMapTypeArguments[1]);
    }
}
