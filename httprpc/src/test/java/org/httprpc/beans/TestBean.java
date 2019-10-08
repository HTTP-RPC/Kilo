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

import static org.httprpc.AbstractTest.*;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBean implements TestInterface {
    public static class NestedBean implements NestedInterface {
        private boolean flag;

        public NestedBean(boolean flag) {
            this.flag = flag;
        }

        @Override
        public boolean getFlag() {
            return flag;
        }
    }

    public static class TestArrayList extends ArrayList<Object> {
    }

    public static class TestHashMap extends HashMap<String, Object> {
    }

    @Override
    @Key("i")
    public int getInt() {
        return 1;
    }

    @Override
    public long getLong() {
        return 2L;
    }

    @Override
    public double getDouble() {
        return 4.0;
    }

    @Override
    public String getString() {
        return "abc";
    }

    public BigInteger getBigInteger() {
        return BigInteger.valueOf(8192L);
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[3];
    }

    @Override
    public Date getDate() {
        return new Date(0);
    }

    @Override
    public LocalDate getLocalDate() {
        return LocalDate.parse("2018-06-28");
    }

    @Override
    public LocalTime getLocalTime() {
        return LocalTime.parse("10:45");
    }

    @Override
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.parse("2018-06-28T10:45");
    }

    @Override
    public URL getURL() {
        try {
            return new URL("http://localhost:8080");
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public NestedBean getNestedBean() {
        return new NestedBean(true);
    }

    @Override
    public List<?> getList() {
        return listOf(2L, 4.0, mapOf(entry("flag", true)));
    }

    public TestArrayList getTestArrayList() {
        return new TestArrayList();
    }

    @Override
    public List<NestedBean> getNestedBeanList() {
        return listOf(new NestedBean(false));
    }

    @Override
    public Map<String, ?> getMap() {
        return mapOf(
            entry("long", 2L),
            entry("double", 4.0),
            entry("nestedBean", mapOf(
                entry("flag", true)
            )));
    }

    public TestHashMap getTestHashMap() {
        return new TestHashMap();
    }

    @Override
    public Map<String, NestedBean> getNestedBeanMap() {
        return mapOf(entry("nestedBean", new NestedBean(false)));
    }

    @Ignore
    public int getIgnored() {
        return -1;
    }
}
