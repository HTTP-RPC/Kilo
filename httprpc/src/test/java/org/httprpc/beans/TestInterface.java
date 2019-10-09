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
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface TestInterface {
    public interface NestedInterface {
        public boolean getFlag();
    }

    @Key("i")
    public int getInt();
    public long getLong();
    public double getDouble();
    public String getString();
    public Date getDate();
    public LocalDate getLocalDate();
    public LocalTime getLocalTime();
    public LocalDateTime getLocalDateTime();
    public NestedInterface getNestedBean();
    public List<?> getList();
    public List<? extends NestedInterface> getNestedBeanList();
    public Map<String, ?> getMap();
    public Map<String, ? extends NestedInterface> getNestedBeanMap();
}
