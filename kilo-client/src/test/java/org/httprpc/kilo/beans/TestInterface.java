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

import org.httprpc.kilo.Required;

import java.math.BigInteger;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TestInterface {
    interface NestedInterface {
        @Required
        Boolean getFlag();
        void setFlag(Boolean flag);
    }

    @Key("i")
    int getInteger();
    void setInteger(int i);

    @Required
    long getLong();
    double getDouble();

    @Required
    String getString();
    void setString(String string);

    BigInteger getBigInteger();
    DayOfWeek getDayOfWeek();
    Date getDate();
    Instant getInstant();
    LocalDate getLocalDate();
    LocalTime getLocalTime();
    LocalDateTime getLocalDateTime();
    Duration getDuration();
    Period getPeriod();
    UUID getUUID();
    URL getURL();

    @Ignore
    String getIgnored();

    NestedInterface getNestedBean();

    @Required
    List<Integer> getIntegerList();
    List<NestedInterface> getNestedBeanList();

    Map<String, Double> getDoubleMap();
    Map<String, NestedInterface> getNestedBeanMap();

    TestRecord getTestRecord();
    List<TestRecord> getTestRecordList();
}
