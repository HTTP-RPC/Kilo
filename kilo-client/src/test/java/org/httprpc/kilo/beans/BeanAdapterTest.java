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
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

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

    public static class DuplicateName {
        @Name("x")
        public String getFoo() {
            return "foo";
        }

        @Name("x")
        public String getBar() {
            return "bar";
        }
    }

    public interface DefaultMethod {
        int getX();

        @Name("z")
        default int getY() {
            return getX() * 2;
        }
    }

    @Test
    public void testBeanAdapter() {
        var now = LocalDate.now();

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
            entry("URI", URI.create("http://localhost:8080")),
            entry("path", Paths.get(System.getProperty("user.home"))),
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
                entry("s", "abc"),
                entry("localDate", now)
            )),
            entry("testRecordList", listOf(
                mapOf(
                    entry("i", 20),
                    entry("d", 456.0),
                    entry("s", "xyz"),
                    entry("localDate", now)
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

        var recordAdapter = new BeanAdapter(new TestRecord(1, 2.0, "three", null));

        assertThrows(UnsupportedOperationException.class, () -> recordAdapter.put("localDate", LocalDate.now()));
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
    public void testURICoercion() {
        var uri = URI.create("http://localhost:8080");

        assertEquals(uri, BeanAdapter.coerce(uri.toString(), URI.class));
    }

    @Test
    public void testPathCoercion() {
        var path = Paths.get(System.getProperty("user.home"));

        assertEquals(path, BeanAdapter.coerce(path.toString(), Path.class));
    }

    @Test
    public void testListCoercion() {
        assertInstanceOf(List.class, BeanAdapter.coerce(listOf(), List.class));
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(123, List.class));
    }

    @Test
    public void testMapCoercion() {
        assertInstanceOf(Map.class, BeanAdapter.coerce(mapOf(), Map.class));
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(123, Map.class));
    }

    @Test
    public void testSetCoercion() {
        assertInstanceOf(Set.class, BeanAdapter.coerce(setOf(), Set.class));
        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(123, Set.class));
    }

    @Test
    public void testInterfaceCoercion() {
        var map = new HashMap<String, Object>();

        map.put("nestedBean", mapOf());

        var testInterface = BeanAdapter.coerce(map, TestInterface.class);

        testInterface.setInteger(150);

        assertEquals(150, map.get("i"));

        testInterface.getNestedBean().setFlag(true);

        assertTrue(testInterface.getNestedBean().getFlag());

        testInterface.setNestedBean(new TestBean.NestedBean());

        assertInstanceOf(Map.class, map.get("nestedBean"));

        assertFalse(testInterface.getNestedBean().getFlag());
    }

    @Test
    public void testObjectMethods() {
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
    @SuppressWarnings("unchecked")
    public void testRequired() {
        var testBean = new TestBean();

        testBean.setNestedBean(new TestBean.NestedBean());

        var beanAdapter = new BeanAdapter(testBean);

        assertEquals(0L, beanAdapter.get("long"));

        var nestedBeanAdapter = (Map<String, ?>)beanAdapter.get("nestedBean");

        assertEquals(false, nestedBeanAdapter.get("flag"));
        assertEquals('x', nestedBeanAdapter.get("character"));

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

        var testInterfaceAdapter = new BeanAdapter(testInterface);

        assertThrows(UnsupportedOperationException.class, () -> testInterfaceAdapter.get("string"));
        assertThrows(IllegalArgumentException.class, () -> testInterfaceAdapter.put("string", null));

        assertThrows(UnsupportedOperationException.class, () -> {
            var iterator = testInterfaceAdapter.entrySet().iterator();

            while (iterator.hasNext()) {
                iterator.next();
            }
        });

        var nestedInterface = testInterface.getNestedBean();

        assertThrows(UnsupportedOperationException.class, nestedInterface::getFlag);
        assertThrows(IllegalArgumentException.class, () -> nestedInterface.setFlag(null));

        assertThrows(IllegalArgumentException.class, () -> BeanAdapter.coerce(mapOf(), TestRecord.class));
    }

    @Test
    public void testGetProperties() {
        var properties = BeanAdapter.getProperties(TestBean.class);

        var propertyTypes = mapOf(mapAll(properties.entrySet(), entry -> entry(entry.getKey(), entry.getValue().getAccessor().getGenericReturnType())));

        assertEquals(Integer.TYPE, propertyTypes.get("i"));
        assertEquals(Long.TYPE, propertyTypes.get("long"));
        assertEquals(Double.TYPE, propertyTypes.get("double"));
        assertEquals(String.class, propertyTypes.get("string"));
        assertEquals(BigInteger.class, propertyTypes.get("bigInteger"));
        assertEquals(DayOfWeek.class, propertyTypes.get("dayOfWeek"));
        assertEquals(Date.class, propertyTypes.get("date"));
        assertEquals(LocalDate.class, propertyTypes.get("localDate"));
        assertEquals(LocalTime.class, propertyTypes.get("localTime"));
        assertEquals(LocalDateTime.class, propertyTypes.get("localDateTime"));
        assertEquals(Duration.class, propertyTypes.get("duration"));
        assertEquals(Period.class, propertyTypes.get("period"));
        assertEquals(UUID.class, propertyTypes.get("UUID"));

        assertEquals(TestInterface.NestedInterface.class, propertyTypes.get("nestedBean"));

        assertInstanceOf(ParameterizedType.class, propertyTypes.get("integerList"));

        var integerListTypeArguments = ((ParameterizedType)propertyTypes.get("integerList")).getActualTypeArguments();

        assertEquals(1, integerListTypeArguments.length);
        assertEquals(Integer.class, integerListTypeArguments[0]);

        var nestedBeanListTypeArguments = ((ParameterizedType)propertyTypes.get("nestedBeanList")).getActualTypeArguments();

        assertEquals(1, nestedBeanListTypeArguments.length);
        assertEquals(TestInterface.NestedInterface.class, nestedBeanListTypeArguments[0]);

        assertInstanceOf(ParameterizedType.class, propertyTypes.get("doubleMap"));

        var doubleMapTypeArguments = ((ParameterizedType)propertyTypes.get("doubleMap")).getActualTypeArguments();

        assertEquals(2, doubleMapTypeArguments.length);
        assertEquals(String.class, doubleMapTypeArguments[0]);
        assertEquals(Double.class, doubleMapTypeArguments[1]);

        var nestedBeanMapTypeArguments = ((ParameterizedType)propertyTypes.get("nestedBeanMap")).getActualTypeArguments();

        assertEquals(2, nestedBeanMapTypeArguments.length);
        assertEquals(String.class, nestedBeanMapTypeArguments[0]);
        assertEquals(TestInterface.NestedInterface.class, nestedBeanMapTypeArguments[1]);

        assertNull(propertyTypes.get("xyz"));

        assertThrows(UnsupportedOperationException.class, () -> properties.remove("i"));
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
    public void testDuplicateName() {
        assertThrows(UnsupportedOperationException.class, () -> new BeanAdapter(new DuplicateName()));
    }

    @Test
    public void testDefaultMethod() {
        var defaultMethod = BeanAdapter.coerce(mapOf(
            entry("x", 1)
        ), DefaultMethod.class);

        assertEquals(2, defaultMethod.getY());

        var beanAdapter = new BeanAdapter(defaultMethod);

        assertEquals(2, beanAdapter.get("z"));
    }
}
