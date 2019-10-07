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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBean {
    public static class NestedBean {
        private boolean flag;

        public boolean getFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }
    }

    public static class TestArrayList extends ArrayList<Object> {
    }

    public static class TestHashMap extends HashMap<String, Object> {
    }

    private int i = 0;
    private long l = 0;
    private double d = 0;
    private String string = null;

    private BigInteger bigInteger = null;
    private DayOfWeek dayOfWeek = null;

    private Date date = null;
    private LocalDate localDate = null;
    private LocalTime localTime = null;
    private LocalDateTime localDateTime = null;

    private URL url = null;

    private NestedBean nestedBean = null;

    private List<?> list = null;
    private List<NestedBean> nestedBeanList = null;

    private Map<String, ?> map = null;
    private Map<String, NestedBean> nestedBeanMap = null;

    @Key("i")
    public int getInt() {
        return i;
    }

    @Key("i")
    public void setInt(int i) {
        this.i = i;
    }

    public long getLong() {
        return l;
    }

    public void setLong(long l) {
        this.l = l;
    }

    public double getDouble() {
        return d;
    }

    public void setDouble(double d) {
        this.d = d;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    public void setBigInteger(long bigInteger) {
        setBigInteger(BigInteger.valueOf(bigInteger));
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        setDayOfWeek(DayOfWeek.values()[dayOfWeek]);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public NestedBean getNestedBean() {
        return nestedBean;
    }

    public void setNestedBean(NestedBean nestedBean) {
        this.nestedBean = nestedBean;
    }

    public List<?> getList() {
        return list;
    }

    public void setList(List<?> list) {
        this.list = list;
    }

    public TestArrayList getTestArrayList() {
        return new TestArrayList();
    }

    public List<NestedBean> getNestedBeanList() {
        return nestedBeanList;
    }

    public void setNestedBeanList(List<NestedBean> nestedBeanList) {
        this.nestedBeanList = nestedBeanList;
    }

    public Map<String, ?> getMap() {
        return map;
    }

    public void setMap(Map<String, ?> map) {
        this.map = map;
    }

    public TestHashMap getTestHashMap() {
        return new TestHashMap();
    }

    public Map<String, NestedBean> getNestedBeanMap() {
        return nestedBeanMap;
    }

    public void setNestedBeanMap(Map<String, NestedBean> nestedBeanMap) {
        this.nestedBeanMap = nestedBeanMap;
    }

    @Ignore
    public int getFoo() {
        return -1;
    }

    public int getBar() {
        return 100;
    }

    @Ignore
    public void setBar(int bar) {
        throw new UnsupportedOperationException();
    }
}
