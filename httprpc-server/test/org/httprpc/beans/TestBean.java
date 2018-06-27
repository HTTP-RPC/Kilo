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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestBean {
    public static class NestedBean {
        public boolean getFlag() {
            return true;
        }
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

    public NestedBean getNestedBean() {
        return new NestedBean();
    }

    public List<NestedBean> getNestedBeanList() {
        return Collections.singletonList(new NestedBean());
    }

    public Map<String, NestedBean> getNestedBeanMap() {
        return Collections.singletonMap("xyz", new NestedBean());
    }
}
