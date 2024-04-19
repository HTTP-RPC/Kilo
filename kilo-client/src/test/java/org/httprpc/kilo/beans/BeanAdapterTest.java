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

package org.httprpc.kilo.beans;

import org.httprpc.kilo.Name;
import org.httprpc.kilo.util.Collections;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.httprpc.kilo.util.Collections.setOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BeanAdapterTest {
    public static class ReadOnly {
        private int x;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return 100;
        }
    }

    public static class MissingAccessor {
        public void setValue(int value) {
            // No-op
        }
    }

    public static class DuplicateKey {
        @Name("x")
        public String getFoo() {
            return "foo";
        }

        @Name("x")
        public String getBar() {
            return "bar";
        }
    }

    public static class PropertyTypeMismatch {
        public int getX() {
            return 0;
        }

        public void setX(String x) {
            // No-op
        }
    }

    @Test
    public void testBeanAdapter() throws MalformedURLException {
        var map = mapOf(
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
                entry("flag", true),
                entry("character", 'y')
            )),
            entry("integerList", listOf(
                1, 2, 3, 4
            )),
            entry("nestedBeanList", listOf(
                mapOf(
                    entry("flag", true),
                    entry("character", 'y')
                ))
            ),
            entry("doubleMap", mapOf(
                entry("a", 1.0),
                entry("b", 2.0),
                entry("c", 3.0),
                entry("d", 4.0)
            )),
            entry("nestedBeanMap", mapOf(
                entry("nestedBean", mapOf(
                    entry("flag", true),
                    entry("character", 'y')
                ))
            )),
            entry("testRecord", mapOf(
                entry("i", 10),
                entry("d", 123.0),
                entry("s", "abc")
            )),
            entry("testRecordList", listOf(
                mapOf(
                    entry("i", 20),
                    entry("d", 456.0),
                    entry("s", "xyz")
                )
            ))
        );

        assertEquals(map, new BeanAdapter(BeanAdapter.coerce(map, TestInterface.class)));
        assertEquals(map, new BeanAdapter(BeanAdapter.coerce(map, TestBean.class)));
    }

    @Test
    public void testInvalidGet() {
        var beanAdapter = new BeanAdapter(new TestBean());

        assertNull(beanAdapter.get("foo"));
    }

    @Test
    public void testInvalidPut() {
        var beanAdapter = new BeanAdapter(new TestBean());

        assertThrows(IllegalArgumentException.class, () -> beanAdapter.put("dayOfWeek", "abc"));
        assertThrows(IllegalArgumentException.class, () -> beanAdapter.put("date", "xyz"));

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("foo", 101));
    }

    @Test
    public void testBeanCoercion() {
        var testBean = BeanAdapter.coerce(mapOf(
            entry("long", 10),
            entry("string", "xyz"),
            entry("integerList", listOf(1, 2, 3)),
            entry("foo", "bar")
        ), TestBean.class);

        assertEquals(10, testBean.getLong());
        assertEquals("xyz", testBean.getString());
        assertEquals(listOf(1, 2, 3), testBean.getIntegerList());
    }

    @Test
    public void testPrimitiveCoercion() {
        assertEquals((byte)0, BeanAdapter.coerce(null, Byte.TYPE));
        assertEquals((byte)1, BeanAdapter.coerce("1", Byte.TYPE));

        assertEquals((short)0, BeanAdapter.coerce(null, Short.TYPE));
        assertEquals((short)2, BeanAdapter.coerce("2", Short.TYPE));

        assertEquals(0, BeanAdapter.coerce(null, Integer.TYPE));
        assertEquals(3, BeanAdapter.coerce("3", Integer.TYPE));

        assertEquals(0L, BeanAdapter.coerce(null, Long.TYPE));
        assertEquals(4L, BeanAdapter.coerce("4", Long.TYPE));

        assertEquals(0.0f, BeanAdapter.coerce(null, Float.TYPE));
        assertEquals(5.0f, BeanAdapter.coerce("5.0", Float.TYPE));

        assertEquals(0.0, BeanAdapter.coerce(null, Double.TYPE));
        assertEquals(6.0, BeanAdapter.coerce("6.0", Double.TYPE));

        assertEquals(Boolean.FALSE, BeanAdapter.coerce(null, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce("true", Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(1, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(-1, Boolean.TYPE));
        assertEquals(Boolean.FALSE, BeanAdapter.coerce(0, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(1.0, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(-1.0, Boolean.TYPE));
        assertEquals(Boolean.FALSE, BeanAdapter.coerce(0.0, Boolean.TYPE));

        assertEquals('\0', BeanAdapter.coerce(null, Character.TYPE));
        assertEquals('a', BeanAdapter.coerce("abc", Character.TYPE));
    }

    @Test
    public void testArrayAdapter() {
        assertEquals(listOf(1, 2, 3), BeanAdapter.adapt(new int[] {1, 2, 3}));
    }

    @Test
    public void testArrayCoercion() {
        assertArrayEquals(new int[]{1, 2, 3}, (int[]) BeanAdapter.coerce(new String[] {"1", "2", "3"}, Integer.TYPE.arrayType()));
        assertArrayEquals(new int[]{1, 2, 3}, (int[]) BeanAdapter.coerce(listOf("1", "2", "3"), Integer.TYPE.arrayType()));
    }

    @Test
    public void testEnumCoercion() {
        assertEquals(DayOfWeek.MONDAY, BeanAdapter.coerce(DayOfWeek.MONDAY.toString(), DayOfWeek.class));
    }

    @Test
    public void testDateCoercion() {
        assertEquals(new Date(0), BeanAdapter.coerce(0, Date.class));
    }

    @Test
    public void testTemporalAccessorCoercion() {
        assertEquals(Instant.ofEpochMilli(1), BeanAdapter.coerce(new Date(1), Instant.class));

        assertEquals(Instant.parse("1970-01-01T00:00:00.001Z"), BeanAdapter.coerce("1970-01-01T00:00:00.001Z", Instant.class));
        assertEquals(LocalDate.parse("2018-06-28"), BeanAdapter.coerce("2018-06-28", LocalDate.class));
        assertEquals(LocalTime.parse("10:45"), BeanAdapter.coerce("10:45", LocalTime.class));
        assertEquals(LocalDateTime.parse("2018-06-28T10:45"), BeanAdapter.coerce("2018-06-28T10:45", LocalDateTime.class));
    }

    @Test
    public void testTemporalAmountCoercion() {
        var duration = BeanAdapter.coerce(12345, Duration.class);

        assertEquals(12, duration.getSeconds());
        assertEquals(345, duration.getNano() / 1000000);

        assertEquals(Duration.parse("PT2H30M"), BeanAdapter.coerce("PT2H30M", Duration.class));
        assertEquals(Period.parse("P3Y2M"), BeanAdapter.coerce("P3Y2M", Period.class));
    }

    @Test
    public void testUUIDCoercion() {
        var uuid = UUID.randomUUID();

        assertEquals(uuid, BeanAdapter.coerce(uuid.toString(), UUID.class));
    }

    @Test
    public void testURLCoercion() throws MalformedURLException {
        assertEquals(new URL("http://localhost:8080"), BeanAdapter.coerce("http://localhost:8080", URL.class));
    }

    @Test
    public void testListCoercion() {
        assertEquals(listOf(
            1,
            2,
            3
        ), BeanAdapter.coerceList(listOf(
            "1",
            "2",
            "3"
        ), Integer.class));

        assertNull(BeanAdapter.coerceList(null, Object.class));

        assertInstanceOf(List.class, BeanAdapter.coerce(listOf(), List.class));
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(123, List.class));
    }

    @Test
    public void testMapCoercion() {
        assertEquals(mapOf(
            entry("a", 1.0),
            entry("b", 2.0),
            entry("c", 3.0)
        ), BeanAdapter.coerceMap(mapOf(
            entry("a", "1.0"),
            entry("b", "2.0"),
            entry("c", "3.0")
        ), Double.class));

        assertEquals(mapOf(
            entry(1, 1.0),
            entry(2, 2.0),
            entry(3, 3.0)
        ), BeanAdapter.coerceMap(mapOf(
            entry(1, "1.0"),
            entry(2, "2.0"),
            entry(3, "3.0")
        ), Double.class));

        assertNull(BeanAdapter.coerceMap(null, Object.class));

        assertInstanceOf(Map.class, BeanAdapter.coerce(mapOf(), Map.class));
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(123, Map.class));
    }

    @Test
    public void testSetCoercion() {
        assertEquals(setOf(
            1,
            2,
            3
        ), BeanAdapter.coerceSet(setOf(
            "1",
            "2",
            "3"
        ), Integer.class));

        assertNull(BeanAdapter.coerceSet(null, Object.class));

        assertInstanceOf(Set.class, BeanAdapter.coerce(setOf(), Set.class));
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(123, Set.class));
    }

    @Test
    public void testMutableListCoercion() {
        var strings = new ArrayList<String>();

        strings.add("1");
        strings.add("2");
        strings.add("3");
        strings.add(null);

        var integers = BeanAdapter.coerceList(strings, Integer.class);

        assertEquals(listOf(1, 2, 3, null), integers);

        integers.set(1, 4);

        assertEquals(listOf(1, 4, 3, null), integers);

        integers.remove(1);

        assertEquals(listOf(1, 3, null), integers);
    }

    @Test
    public void testMutableMapCoercion() {
        var strings = new HashMap<Integer, String>();

        strings.put(1, "1.0");
        strings.put(2, "2.0");
        strings.put(3, "3.0");
        strings.put(4, null);

        var doubles = BeanAdapter.coerceMap(strings, Double.class);

        assertEquals(mapOf(
            entry(1, 1.0),
            entry(2, 2.0),
            entry(3, 3.0),
            entry(4, null)
        ), doubles);

        doubles.put(2, 4.0);

        assertEquals(mapOf(
            entry(1, 1.0),
            entry(2, 4.0),
            entry(3, 3.0),
            entry(4, null)
        ), doubles);

        doubles.remove(2);

        assertEquals(mapOf(
            entry(1, 1.0),
            entry(3, 3.0),
            entry(4, null)
        ), doubles);
    }

    @Test
    public void testMutableSetCoercion() {
        var strings = new HashSet<>();

        strings.add("1");
        strings.add("2");
        strings.add("3");
        strings.add(null);

        var integers = BeanAdapter.coerceSet(strings, Integer.class);

        assertEquals(setOf(1, 2, 3, null), integers);

        integers.remove(2);

        assertEquals(setOf(1, 3, null), integers);
    }

    @Test
    public void testInterfaceCoercion() {
        var map = new HashMap<String, Object>();

        map.put("nestedBean", mapOf());

        var testInterface = BeanAdapter.coerce(map, TestInterface.class);

        testInterface.setInteger(150);

        assertEquals(150, map.get("i"));

        testInterface.getNestedBean().setFlag(true);

        assertEquals(true, Collections.valueAt(map, "nestedBean", "flag"));

        var nestedBean = new TestBean.NestedBean();

        nestedBean.setFlag(true);

        testInterface.setNestedBean(nestedBean);

        assertInstanceOf(Map.class, map.get("nestedBean"));
        assertEquals(true, Collections.valueAt(map, "nestedBean", "flag"));
    }

    @Test
    public void testInterfaceEquality() {
        var map1 = new HashMap<String, Object>();

        map1.put("flag", true);
        map1.put("character", 'a');
        map1.put("foo", 1);

        var nestedBean1 = BeanAdapter.coerce(map1, TestInterface.NestedInterface.class);

        assertEquals(0, nestedBean1.hashCode());

        var map2 = new HashMap<String, Object>() {
            @Override
            public String toString() {
                return "xyz";
            }
        };

        map2.put("flag", 1);
        map2.put("character", "abc");
        map2.put("foo", 2);

        var nestedBean2 = BeanAdapter.coerce(map2, TestInterface.NestedInterface.class);

        assertEquals(nestedBean1, nestedBean2);

        assertEquals(map2.toString(), nestedBean2.toString());
    }

    @Test
    public void testRequired() {
        var testBean = new TestBean();

        testBean.setNestedBean(new TestBean.NestedBean());

        var beanAdapter = new BeanAdapter(testBean);

        assertEquals(0L, beanAdapter.get("long"));
        assertEquals(false, Collections.valueAt(beanAdapter, "nestedBean", "flag"));
        assertEquals('x', Collections.valueAt(beanAdapter, "nestedBean", "character"));

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.get("string"));
        assertThrows(IllegalArgumentException.class, () -> beanAdapter.put("string", null));

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.get("integerList"));
        assertThrows(IllegalArgumentException.class, () -> beanAdapter.put("integerList", null));

        assertThrows(UnsupportedOperationException.class, () -> {
            var iterator = beanAdapter.entrySet().iterator();

            while (iterator.hasNext()) {
                iterator.next();
            }
        });

        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(mapOf(), TestBean.class));

        var map = new HashMap<String, Object>();

        map.put("long", null);
        map.put("nestedBean", mapOf());

        var testInterface = BeanAdapter.coerce(map, TestInterface.class);

        assertThrows(UnsupportedOperationException.class, testInterface::getLong);

        assertThrows(UnsupportedOperationException.class, testInterface::getString);
        assertThrows(IllegalArgumentException.class, () -> testInterface.setString(null));

        assertThrows(UnsupportedOperationException.class, testInterface::getIntegerList);
        assertThrows(IllegalArgumentException.class, () -> testInterface.setIntegerList(null));

        var nestedInterface = testInterface.getNestedBean();

        assertThrows(UnsupportedOperationException.class, nestedInterface::getFlag);
        assertThrows(IllegalArgumentException.class, () -> nestedInterface.setFlag(null));

        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(mapOf(), TestRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInternal() {
        var testInterface = BeanAdapter.coerce(mapOf(
            entry("long", 0),
            entry("string", ""),
            entry("integerList", listOf()),
            entry("value", 100)
        ), TestInterface.class);

        assertEquals(100, testInterface.getValue());

        testInterface.setValue(150);

        assertEquals(150, testInterface.getValue());

        var interfaceAdapter = new BeanAdapter(testInterface);

        assertNull(interfaceAdapter.get("value"));
        assertFalse(interfaceAdapter.containsKey("value"));

        assertThrows(UnsupportedOperationException.class, () -> interfaceAdapter.put("value", 200));

        var testBean = BeanAdapter.coerce(mapOf(
            entry("long", 0),
            entry("string", ""),
            entry("integerList", listOf()),
            entry("value", 100)
        ), TestBean.class);

        assertEquals(100, testBean.getValue());

        var beanAdapter = new BeanAdapter(testBean);

        assertNull(beanAdapter.get("value"));
        assertFalse(beanAdapter.containsKey("value"));

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("value", 200));

        var now = LocalDate.now();

        var testRecord = BeanAdapter.coerce(mapOf(
            entry("s", ""),
            entry("localDate", now)
        ), TestRecord.class);

        assertEquals(now, testRecord.localDate());

        var recordAdapter = (Map<String, Object>)BeanAdapter.adapt(testRecord);

        assertNull(recordAdapter.get("localDate"));
        assertFalse(recordAdapter.containsKey("localDate"));
    }

    @Test
    public void testGetProperties() {
        var properties = BeanAdapter.getProperties(TestBean.class).entrySet().stream()
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

        assertEquals(TestInterface.NestedInterface.class, properties.get("nestedBean"));

        assertInstanceOf(ParameterizedType.class, properties.get("integerList"));

        var integerListTypeArguments = ((ParameterizedType)properties.get("integerList")).getActualTypeArguments();

        assertEquals(1, integerListTypeArguments.length);
        assertEquals(Integer.class, integerListTypeArguments[0]);

        var nestedBeanListTypeArguments = ((ParameterizedType)properties.get("nestedBeanList")).getActualTypeArguments();

        assertEquals(1, nestedBeanListTypeArguments.length);
        assertEquals(TestInterface.NestedInterface.class, nestedBeanListTypeArguments[0]);

        assertInstanceOf(ParameterizedType.class, properties.get("doubleMap"));

        var doubleMapTypeArguments = ((ParameterizedType)properties.get("doubleMap")).getActualTypeArguments();

        assertEquals(2, doubleMapTypeArguments.length);
        assertEquals(String.class, doubleMapTypeArguments[0]);
        assertEquals(Double.class, doubleMapTypeArguments[1]);

        var nestedBeanMapTypeArguments = ((ParameterizedType)properties.get("nestedBeanMap")).getActualTypeArguments();

        assertEquals(2, nestedBeanMapTypeArguments.length);
        assertEquals(String.class, nestedBeanMapTypeArguments[0]);
        assertEquals(TestInterface.NestedInterface.class, nestedBeanMapTypeArguments[1]);

        assertNull(properties.get("xyz"));
    }

    @Test
    public void testReadOnly() {
        var readOnly = BeanAdapter.coerce(mapOf(
            entry("x", 10),
            entry("y", 200)
        ), ReadOnly.class);

        assertEquals(10, readOnly.getX());
        assertEquals(100, readOnly.getY());
    }

    @Test
    public void testMissingAccessor() {
        assertThrows(UnsupportedOperationException.class, () -> new BeanAdapter(new MissingAccessor()));
    }

    @Test
    public void testDuplicateKey() {
        assertThrows(UnsupportedOperationException.class, () -> new BeanAdapter(new DuplicateKey()));
    }

    @Test
    public void testPropertyTypeMismatch() {
        assertThrows(UnsupportedOperationException.class, () -> new BeanAdapter(new PropertyTypeMismatch()));
    }
}
