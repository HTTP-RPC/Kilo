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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        private int value;

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class DuplicateKey {
        @Key("x")
        public String getFoo() {
            return "foo";
        }

        @Key("x")
        public String getBar() {
            return "bar";
        }
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
            entry("integerList", listOf(
                1, 2, 3, 4
            )),
            entry("nestedBeanList", listOf(
                mapOf(
                    entry("flag", true)
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
                    entry("flag", true)
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

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("foo", 101));
        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("dayOfWeek", "abc"));
        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("date", "xyz"));
    }

    @Test
    public void testPrimitiveCoercion() {
        assertEquals(BeanAdapter.coerce(null, Byte.TYPE), Byte.valueOf((byte)0));
        assertEquals(BeanAdapter.coerce("1", Byte.TYPE), Byte.valueOf((byte)1));

        assertEquals(BeanAdapter.coerce(null, Short.TYPE), Short.valueOf((short)0));
        assertEquals(BeanAdapter.coerce("2", Short.TYPE), Short.valueOf((short)2));

        assertEquals(BeanAdapter.coerce(null, Integer.TYPE), Integer.valueOf(0));
        assertEquals(BeanAdapter.coerce("3", Integer.TYPE), Integer.valueOf(3));

        assertEquals(BeanAdapter.coerce(null, Long.TYPE), Long.valueOf(0));
        assertEquals(BeanAdapter.coerce("4", Long.TYPE), Long.valueOf(4));

        assertEquals(BeanAdapter.coerce(null, Float.TYPE), Float.valueOf(0));
        assertEquals(BeanAdapter.coerce("5.0", Float.TYPE), Float.valueOf(5));

        assertEquals(BeanAdapter.coerce(null, Double.TYPE), Double.valueOf(0));
        assertEquals(BeanAdapter.coerce("6.0", Double.TYPE), Double.valueOf(6));

        assertEquals(Boolean.FALSE, BeanAdapter.coerce(null, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce("true", Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(1, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(-1, Boolean.TYPE));
        assertEquals(Boolean.FALSE, BeanAdapter.coerce(0, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(1.0, Boolean.TYPE));
        assertEquals(Boolean.TRUE, BeanAdapter.coerce(-1.0, Boolean.TYPE));
        assertEquals(Boolean.FALSE, BeanAdapter.coerce(0.0, Boolean.TYPE));
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
        ), BeanAdapter.coerce(listOf(
            "1",
            "2",
            "3"
        ), List.class, Integer.class));

        assertNull(BeanAdapter.coerce(null, List.class, Object.class));
    }

    @Test
    public void testMapCoercion() {
        assertEquals(mapOf(
            entry("a", 1.0),
            entry("b", 2.0),
            entry("c", 3.0)
        ), BeanAdapter.coerce(mapOf(
            entry("a", "1.0"),
            entry("b", "2.0"),
            entry("c", "3.0")
        ), Map.class, String.class, Double.class));

        assertEquals(mapOf(
            entry(1, 1.0),
            entry(2, 2.0),
            entry(3, 3.0)
        ), BeanAdapter.coerce(mapOf(
            entry("1", "1.0"),
            entry("2", "2.0"),
            entry("3", "3.0")
        ), Map.class, Integer.class, Double.class));

        assertNull(BeanAdapter.coerce(null, Map.class, Object.class, Object.class));
    }

    @Test
    public void testNestedCoercion() {
        assertEquals(listOf(
            listOf(
                1,
                2,
                3
            )
        ), BeanAdapter.coerce(listOf(
            listOf(
                "1",
                "2",
                "3"
            )
        ), List.class, BeanAdapter.typeOf(List.class, Integer.class)));

        assertEquals(listOf(
            mapOf(
                entry(1, 1.0),
                entry(2, 2.0),
                entry(3, 3.0)
            )
        ), BeanAdapter.coerce(listOf(
            mapOf(
                entry("1", "1.0"),
                entry("2", "2.0"),
                entry("3", "3.0")
            )
        ), List.class, BeanAdapter.typeOf(Map.class, Integer.class, Double.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMutableListCoercion() {
        var strings = new ArrayList<String>();

        strings.add("1");
        strings.add("2");
        strings.add("3");
        strings.add(null);

        var integers = (List<Integer>)BeanAdapter.coerce(strings, List.class, Integer.class);

        assertEquals(listOf(1, 2, 3, null), integers);

        integers.set(1, 4);

        assertEquals(listOf(1, 4, 3, null), integers);

        integers.remove(1);

        assertEquals(listOf(1, 3, null), integers);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMutableMapCoercion() {
        var strings = new HashMap<Integer, String>();

        strings.put(1, "1.0");
        strings.put(2, "2.0");
        strings.put(3, "3.0");
        strings.put(4, null);

        var doubles = (Map<Integer, Double>)BeanAdapter.coerce(strings, Map.class, Integer.class, Double.class);

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
    public void testTypedInvocationProxy() {
        var map = new HashMap<String, Object>();

        map.put("nestedBean", mapOf());

        var testInterface = BeanAdapter.coerce(map, TestInterface.class);

        testInterface.setInteger(150);

        assertEquals(150, Collections.valueAt(map, "i"));

        var nestedInterface = testInterface.getNestedBean();

        nestedInterface.setFlag(true);

        assertEquals(true, Collections.valueAt(map, "nestedBean", "flag"));

        var nestedBean = new TestBean.NestedBean();

        nestedBean.setFlag(true);

        testInterface.setNestedBean(nestedBean);

        assertTrue(map.get("nestedBean") instanceof Map<?, ?>);
        assertEquals(true, Collections.valueAt(map, "nestedBean", "flag"));
    }

    @Test
    public void testMissingProperty() {
        var testBean = BeanAdapter.coerce(mapOf(
            entry("long", 10),
            entry("string", "xyz"),
            entry("integerList", listOf()),
            entry("foo", "bar")
        ), TestBean.class);

        assertNotNull(testBean);
    }

    @Test
    public void testInvalidTypeOf() {
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.typeOf(List.class));
    }

    @Test
    public void testObjectMethodDelegation() {
        Map<String, Object> map1 = new HashMap<>() {
            @Override
            public String toString() {
                return "abc";
            }
        };

        map1.put("flag", true);

        Map<String, Object> map2 = mapOf(entry("flag", true));

        var nestedBean1 = BeanAdapter.coerce(map1, TestInterface.NestedInterface.class);
        var nestedBean2 = BeanAdapter.coerce(map2, TestInterface.NestedInterface.class);

        assertEquals(nestedBean1, nestedBean2);
        assertEquals(map1.hashCode(), nestedBean1.hashCode());
        assertEquals(map1.toString(), nestedBean1.toString());
    }

    @Test
    public void testRequired() {
        var testBean = new TestBean();

        testBean.setNestedBean(new TestBean.NestedBean());

        var beanAdapter = new BeanAdapter(testBean);

        assertEquals(0L, Collections.valueAt(beanAdapter, "long"));
        assertEquals(false, Collections.valueAt(beanAdapter, "nestedBean", "flag"));

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.get("string"));
        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.get("integerList"));

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

        var nestedInterface = testInterface.getNestedBean();

        assertThrows(UnsupportedOperationException.class, nestedInterface::getFlag);
        assertThrows(IllegalArgumentException.class, () -> nestedInterface.setFlag(null));

        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(mapOf(), TestRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIgnore() {
        var testBean = new TestBean();

        testBean.setIgnored("abc");

        var beanAdapter = new BeanAdapter(testBean);

        assertNull(beanAdapter.get("ignored"));

        assertThrows(UnsupportedOperationException.class, () -> beanAdapter.put("ignored", "xyz"));

        var testRecord = BeanAdapter.coerce(mapOf(
            entry("s", "abc"),
            entry("localDate", LocalDate.now())
        ), TestRecord.class);

        assertEquals(0, testRecord.i());
        assertEquals(0.0, testRecord.d());

        assertNull(testRecord.localDate());

        var recordAdapter = (Map<String, ?>)BeanAdapter.adapt(new TestRecord(1, 2.0, "xyx", LocalDate.now()));

        assertNull(recordAdapter.get("localDate"));
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

        assertTrue(properties.get("integerList") instanceof ParameterizedType);

        var integerListTypeArguments = ((ParameterizedType)properties.get("integerList")).getActualTypeArguments();

        assertEquals(1, integerListTypeArguments.length);
        assertEquals(Integer.class, integerListTypeArguments[0]);

        var nestedBeanListTypeArguments = ((ParameterizedType)properties.get("nestedBeanList")).getActualTypeArguments();

        assertEquals(1, nestedBeanListTypeArguments.length);
        assertEquals(TestInterface.NestedInterface.class, nestedBeanListTypeArguments[0]);

        assertTrue(properties.get("doubleMap") instanceof ParameterizedType);

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
}
