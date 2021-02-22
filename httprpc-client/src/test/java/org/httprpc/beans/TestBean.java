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

import java.math.BigInteger;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestBean implements TestInterface {
    public static class NestedBean implements NestedInterface {
        private boolean flag = false;

        @Override
        public boolean getFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }
    }

    private int i = 0;
    private long l = 0;
    private double d = 0;
    private String string = null;
    private BigInteger bigInteger = null;
    private DayOfWeek dayOfWeek = null;
    private Date date = null;
    private Instant instant = null;
    private LocalDate localDate = null;
    private LocalTime localTime = null;
    private LocalDateTime localDateTime = null;
    private URL url = null;

    private NestedBean nestedBean = null;

    private List<?> list = null;
    private List<Integer> integerList = null;
    private List<NestedBean> nestedBeanList = null;

    private Map<String, ?> map = null;
    private Map<String, Double> doubleMap = null;
    private Map<String, NestedBean> nestedBeanMap = null;

    @Key("i")
    @Override
    public int getInteger() {
        return i;
    }

    @Key("i")
    public void setInteger(int i) {
        this.i = i;
    }

    @Override
    public long getLong() {
        return l;
    }

    public void setLong(long l) {
        this.l = l;
    }

    @Override
    public double getDouble() {
        return d;
    }

    public void setDouble(double d) {
        this.d = d;
    }

    @Override
    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    @Override
    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    @Override
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    @Override
    public LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    @Override
    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    @Override
    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    @Override
    public NestedBean getNestedBean() {
        return nestedBean;
    }

    public void setNestedBean(NestedBean nestedBean) {
        this.nestedBean = nestedBean;
    }

    @Override
    public List<?> getList() {
        return list;
    }

    public void setList(List<?> list) {
        this.list = list;
    }

    @Override
    public List<Integer> getIntegerList() {
        return integerList;
    }

    public void setIntegerList(List<Integer> integerList) {
        this.integerList = integerList;
    }

    @Override
    public List<NestedBean> getNestedBeanList() {
        return nestedBeanList;
    }

    public void setNestedBeanList(List<NestedBean> nestedBeanList) {
        this.nestedBeanList = nestedBeanList;
    }

    @Override
    public Map<String, ?> getMap() {
        return map;
    }

    public void setMap(Map<String, ?> map) {
        this.map = map;
    }

    @Override
    public Map<String, Double> getDoubleMap() {
        return doubleMap;
    }

    public void setDoubleMap(Map<String, Double> doubleMap) {
        this.doubleMap = doubleMap;
    }

    @Override
    public Map<String, NestedBean> getNestedBeanMap() {
        return nestedBeanMap;
    }

    public void setNestedBeanMap(Map<String, NestedBean> nestedBeanMap) {
        this.nestedBeanMap = nestedBeanMap;
    }
}
