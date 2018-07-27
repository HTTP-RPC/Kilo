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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBean implements TestInterface {
    public static class NestedBean implements TestInterface.NestedInterface {
        @Override
        public boolean getFlag() {
            return true;
        }
    }

    @Key("i")
    @Override
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
    public List<?> getList() {
        return Arrays.asList(2L, 4.0, new NestedBean());
    }

    @Override
    public List<NestedBean> getNestedBeanList() {
        return Collections.singletonList(new NestedBean());
    }

    @Override
    public Map<String, ?> getMap() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("long", 2L);
        map.put("double", 4.0);
        map.put("nestedBean", new NestedBean());

        return map;
    }

    @Override
    public Map<String, NestedBean> getNestedBeanMap() {
        return Collections.singletonMap("nestedBean", new NestedBean());
    }

    @Override
    public NestedBean getNestedBean() {
        return new NestedBean();
    }
}
