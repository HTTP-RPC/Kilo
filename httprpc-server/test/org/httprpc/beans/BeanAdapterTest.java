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
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class BeanAdapterTest extends AbstractTest {
    @Test
    public void testBeanAdapter() {
        BeanAdapter adapter = new BeanAdapter(new TestBean());

        Assert.assertEquals(mapOf(
            entry("long", 2L),
            entry("double", 4.0),
            entry("string", "abc"),
            entry("date", new Date(0)),
            entry("localDate", LocalDate.parse("2018-06-28")),
            entry("localTime", LocalTime.parse("10:45")),
            entry("localDateTime", LocalDateTime.parse("2018-06-28T10:45")),
            entry("nestedBean", mapOf(entry("flag", true))),
            entry("nestedBeanList", listOf(mapOf(entry("flag", true)))),
            entry("nestedBeanMap", mapOf(entry("xyz", mapOf(entry("flag", true)))))
        ), adapter);
    }
}
