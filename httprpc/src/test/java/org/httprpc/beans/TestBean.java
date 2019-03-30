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
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestBean {
    public static class NestedBean {
        private boolean flag;

        public NestedBean(boolean flag) {
            this.flag = flag;
        }

        public boolean getFlag() {
            return flag;
        }
    }

    @Key("i")
    public int getInt() {
        return 1;
    }

    public long getLong() {
        return 2L;
    }

    public double getDouble() {
        return 4.0;
    }

    public String getString() {
        return "abc";
    }

    public BigInteger getBigInteger() {
        return BigInteger.valueOf(8192L);
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.values()[3];
    }

    public Date getDate() {
        return new Date(0);
    }

    public LocalDate getLocalDate() {
        return LocalDate.parse("2018-06-28");
    }

    public LocalTime getLocalTime() {
        return LocalTime.parse("10:45");
    }

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.parse("2018-06-28T10:45");
    }

    public URL getURL() throws MalformedURLException {
        return new URL("http://localhost:8080");
    }

    public List<?> getList() {
        return listOf(2L, 4.0, mapOf(entry("flag", true)));
    }

    public List<NestedBean> getNestedBeanList() {
        return listOf(new NestedBean(false));
    }

    public Map<String, ?> getMap() {
        return mapOf(
            entry("long", 2L),
            entry("double", 4.0),
            entry("nestedBean", mapOf(
                entry("flag", true)
            )));
    }

    public Map<String, NestedBean> getNestedBeanMap() {
        return mapOf(entry("nestedBean", new NestedBean(false)));
    }

    public NestedBean getNestedBean() {
        return new NestedBean(true);
    }
}
