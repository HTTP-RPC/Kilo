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

package org.httprpc.util;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import static org.httprpc.WebService.listOf;
import static org.httprpc.WebService.mapOf;
import static org.httprpc.WebService.entry;

public class IteratorAdapterTest {
    @Test
    public void testIteratorAdapter1() {
        List<?> list1 = listOf(2L, 4.0, "abc", new Date(0), new Object() {
            @Override
            public String toString() {
                return "hello";
            }
        });

        LinkedList<Object> list2 = new LinkedList<>();

        for (Object element : new IteratorAdapter(list1.iterator())) {
            list2.add(element);
        }

        Assert.assertEquals(listOf(2L, 4.0, "abc", 0L, "hello"), list2);
    }

    @Test
    public void testIteratorAdapter2() {
        List<?> list1 = listOf(listOf(2L, 4.0, "abc", new Date(0), new Object() {
            @Override
            public String toString() {
                return "hello";
            }
        }));

        LinkedList<Object> list2 = new LinkedList<>();

        for (Object element : new IteratorAdapter(list1.iterator())) {
            list2.add(element);
        }

        Assert.assertEquals(listOf(listOf(2L, 4.0, "abc", 0L, "hello")), list2);
    }

    @Test
    public void testIteratorAdapter3() {
        List<?> list1 = listOf(mapOf(entry("a", 2L), entry("b", 4.0), entry("c", "abc"), entry("d", new Date(0)), entry("e", new Object() {
            @Override
            public String toString() {
                return "hello";
            }
        })));

        LinkedList<Object> list2 = new LinkedList<>();

        for (Object element : new IteratorAdapter(list1.iterator())) {
            list2.add(element);
        }

        Assert.assertEquals(listOf(mapOf(entry("a", 2L), entry("b", 4.0), entry("c", "abc"), entry("d", 0L), entry("e", "hello"))), list2);
    }
}
