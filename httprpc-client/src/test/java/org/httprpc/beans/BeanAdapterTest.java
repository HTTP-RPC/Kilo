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

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.httprpc.util.Collections.entry;
import static org.httprpc.util.Collections.listOf;
import static org.httprpc.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BeanAdapterTest {
    public static class TestList extends ArrayList<Integer> {
    }

    public static class TestMap extends HashMap<String, Double> {
    }

    @Test
    public void testBeanAdapter() throws MalformedURLException {
        Map<String, ?> map = mapOf(
            entry("i", 1),
            entry("long", 2L),
            entry("double", 4.0),
            entry("string", "abc"),
            entry("bigInteger", BigInteger.valueOf(8192L)),
            entry("dayOfWeek", DayOfWeek.MONDAY),
            entry("date", new Date(0)),
            entry("instant", Instant.ofEpochMilli(1)),
            entry("localDate", LocalDate.parse("2018-06-28")),
            entry("localTime", LocalTime.parse("10:45")),
            entry("localDateTime", LocalDateTime.parse("2018-06-28T10:45")),
            entry("duration", Duration.parse("PT2H30M")),
            entry("period", Period.parse("P3Y2M")),
            entry("UUID", UUID.randomUUID()),
            entry("URL", new URL("http://localhost:8080")),
            entry("nestedBean", mapOf(
                entry("flag", true)
            )),
            entry("list", listOf(
                1, 2L, 4.0, "abc"
            )),
            entry("integerList", listOf(
                1, 2, 3, 4
            )),
            entry("nestedBeanList", listOf(
                mapOf(
                    entry("flag", true)
                ))
            ),
            entry("map", mapOf(
                entry("a", 1),
                entry("b", 2L),
                entry("c", 4.0),
                entry("d", "abc")
            )),
            entry("doubleMap", mapOf(
                entry("a", 1.0),
                entry("b", 2.0),
                entry("c", 3.0),
                entry("d", 4.0)
            )),
            entry("nestedBeanMap", mapOf(
                entry("nestedBean", mapOf(
                    entry("flag", true)
                ))
            ))
        );

        assertEquals(map, new BeanAdapter(BeanAdapter.adapt(map, TestInterface.class)));
        assertEquals(map, new BeanAdapter(BeanAdapter.adapt(map, TestBean.class)));
    }

    @Test
    public void testInvalidGet() {
        BeanAdapter beanAdapter = new BeanAdapter(new TestBean());

        assertNull(beanAdapter.get("foo"));
    }

    @Test
    public void testInvalidPut() {
        BeanAdapter beanAdapter = new BeanAdapter(new TestBean());

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("foo", 101));
        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("dayOfWeek", "abc"));
        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("date", "xyz"));
    }

    @Test
    public void testPrimitiveCoercion() {
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
        assertEquals(BeanAdapter.adapt(1, Boolean.TYPE), Boolean.TRUE);
        assertEquals(BeanAdapter.adapt(0, Boolean.TYPE), Boolean.FALSE);
        assertEquals(BeanAdapter.adapt(0.5, Boolean.TYPE), Boolean.TRUE);
        assertEquals(BeanAdapter.adapt(1.0, Boolean.TYPE), Boolean.TRUE);
        assertEquals(BeanAdapter.adapt(0.0, Boolean.TYPE), Boolean.FALSE);
    }

    @Test
    public void testEnumCoercion() {
        assertEquals(DayOfWeek.MONDAY, BeanAdapter.adapt(DayOfWeek.MONDAY.toString(), DayOfWeek.class));
    }

    @Test
    public void testDateCoercion() {
        assertEquals(new Date(0), BeanAdapter.adapt(0, Date.class));
    }

    @Test
    public void testTemporalAccessorCoercion() {
        assertEquals(Instant.ofEpochMilli(1), BeanAdapter.adapt(new Date(1), Instant.class));

        assertEquals(Instant.parse("1970-01-01T00:00:00.001Z"), BeanAdapter.adapt("1970-01-01T00:00:00.001Z", Instant.class));
        assertEquals(LocalDate.parse("2018-06-28"), BeanAdapter.adapt("2018-06-28", LocalDate.class));
        assertEquals(LocalTime.parse("10:45"), BeanAdapter.adapt("10:45", LocalTime.class));
        assertEquals(LocalDateTime.parse("2018-06-28T10:45"), BeanAdapter.adapt("2018-06-28T10:45", LocalDateTime.class));
    }

    @Test
    public void testTemporalAmountCoercion() {
        assertEquals(Duration.parse("PT2H30M"), BeanAdapter.adapt("PT2H30M", Duration.class));
        assertEquals(Period.parse("P3Y2M"), BeanAdapter.adapt("P3Y2M", Period.class));
    }

    @Test
    public void testUUIDCoercion() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, BeanAdapter.adapt(uuid.toString(), UUID.class));
    }

    @Test
    public void testURLCoercion() throws MalformedURLException {
        assertEquals(new URL("http://localhost:8080"), BeanAdapter.adapt("http://localhost:8080", URL.class));
    }

    @Test
    public void testListCoercion() {
        int i = BeanAdapter.adaptList(listOf("1", "2", "3"), Integer.class).get(0);

        assertEquals(1, i);

        assertNull(BeanAdapter.adaptList(null, Object.class));
    }

    @Test
    public void testMapCoercion() {
        int i = BeanAdapter.adaptMap(mapOf(
            entry("a", "1"),
            entry("b", "2"),
            entry("c", "3")
        ), Integer.class).get("a");

        assertEquals(1, i);

        assertNull(BeanAdapter.adaptMap(null, Object.class));
    }

    @Test
    public void testTypeOf() {
        List<Integer> values = BeanAdapter.adapt(listOf("1", "2", "3"), BeanAdapter.typeOf(List.class, Integer.class));

        assertEquals(listOf(1, 2, 3), values);
    }

    @Test
    public void testObjectMethodDelegation() {
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
    public void testReifiedList() {
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.adapt(emptyList(), TestList.class));
    }

    @Test
    public void testReifiedMap() {
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.adapt(emptyMap(), TestMap.class));
    }

    @Test
    public void testMissingProperty() {
        BeanAdapter.adapt(mapOf(
            entry("foo", "bar")
        ), TestBean.class);
    }

    @Test
    public void testGetProperties() {
        Map<String, Type> properties = BeanAdapter.getProperties(TestBean.class).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAccessor().getGenericReturnType()));

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
        assertEquals(Duration.class, properties.get("duration"));
        assertEquals(Period.class, properties.get("period"));
        assertEquals(URL.class, properties.get("URL"));

        assertEquals(TestBean.NestedBean.class, properties.get("nestedBean"));

        assertTrue(properties.get("list") instanceof ParameterizedType);

        Type[] listTypeArguments = ((ParameterizedType)properties.get("list")).getActualTypeArguments();

        assertEquals(1, listTypeArguments.length);
        assertTrue(listTypeArguments[0] instanceof WildcardType);

        assertTrue(properties.get("integerList") instanceof ParameterizedType);

        Type[] integerListTypeArguments = ((ParameterizedType)properties.get("integerList")).getActualTypeArguments();

        assertEquals(1, integerListTypeArguments.length);
        assertEquals(Integer.class, integerListTypeArguments[0]);

        Type[] nestedBeanListTypeArguments = ((ParameterizedType)properties.get("nestedBeanList")).getActualTypeArguments();

        assertEquals(1, nestedBeanListTypeArguments.length);
        assertEquals(TestBean.NestedBean.class, nestedBeanListTypeArguments[0]);

        assertTrue(properties.get("map") instanceof ParameterizedType);

        Type[] mapTypeArguments = ((ParameterizedType)properties.get("map")).getActualTypeArguments();

        assertEquals(2, mapTypeArguments.length);
        assertEquals(String.class, mapTypeArguments[0]);
        assertTrue(mapTypeArguments[1] instanceof WildcardType);

        assertTrue(properties.get("doubleMap") instanceof ParameterizedType);

        Type[] doubleMapTypeArguments = ((ParameterizedType)properties.get("doubleMap")).getActualTypeArguments();

        assertEquals(2, doubleMapTypeArguments.length);
        assertEquals(String.class, doubleMapTypeArguments[0]);
        assertEquals(Double.class, doubleMapTypeArguments[1]);

        Type[] nestedBeanMapTypeArguments = ((ParameterizedType)properties.get("nestedBeanMap")).getActualTypeArguments();

        assertEquals(2, nestedBeanMapTypeArguments.length);
        assertEquals(String.class, nestedBeanMapTypeArguments[0]);
        assertEquals(TestBean.NestedBean.class, nestedBeanMapTypeArguments[1]);

        assertNull(properties.get("x"));
        assertNull(properties.get("y"));
        assertNull(properties.get("z"));
    }
}
