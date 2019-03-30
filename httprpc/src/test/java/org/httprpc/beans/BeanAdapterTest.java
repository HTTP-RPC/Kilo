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

import org.httprpc.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

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

public class BeanAdapterTest extends AbstractTest {
    @Test
    public void testBeanAdapter() throws MalformedURLException {
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

        Assert.assertEquals(expected, new BeanAdapter(new TestBean()));
    }

    @Test
    public void testValueAt() {
        Map<String, ?> map = mapOf(
            entry("a", mapOf(
                entry("b", mapOf(
                    entry("c", 123)
                ))
            ))
        );

        Assert.assertEquals(Integer.valueOf(123), BeanAdapter.valueAt(map, "a.b.c"));
    }

    @Test
    public void testDescribe() {
        HashMap<Class<?>, String> structures = new HashMap<>();

        BeanAdapter.describe(TestBean.class, structures);

        Assert.assertEquals(structures.get(TestBean.class),
            "{\n" +
            "  URL: url,\n" +
            "  bigInteger: number,\n" +
            "  date: date,\n" +
            "  dayOfWeek: enum,\n" +
            "  double: double,\n" +
            "  i: integer,\n" +
            "  list: [any],\n" +
            "  localDate: date-local,\n" +
            "  localDateTime: datetime-local,\n" +
            "  localTime: time-local,\n" +
            "  long: long,\n" +
            "  map: [string: any],\n" +
            "  nestedBean: NestedBean,\n" +
            "  nestedBeanList: [NestedBean],\n" +
            "  nestedBeanMap: [string: NestedBean],\n" +
            "  string: string\n" +
            "}"
        );

        Assert.assertEquals(structures.get(TestBean.NestedBean.class),
            "{\n" +
            "  flag: boolean\n" +
            "}"
        );
    }
}
