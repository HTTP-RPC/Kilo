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

package org.httprpc.kilo.test;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Part;
import org.httprpc.kilo.Accepts;
import org.httprpc.kilo.Creates;
import org.httprpc.kilo.Description;
import org.httprpc.kilo.FormData;
import org.httprpc.kilo.Name;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TextEncoder;

import java.io.IOException;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.*;

@WebServlet(urlPatterns = {"/test/*"}, loadOnStartup = 1)
@MultipartConfig
public class TestService extends AbstractDatabaseService {
    public interface A {
        int getA();
    }

    public interface B {
        @Description("B's version of B")
        double getB();
    }

    public interface C extends A, B {
        @Override
        @Description("C's version of B")
        double getB();

        @Description("An array of strings")
        String[] getC();
    }

    public static class D {
        public int getD() {
            return 0;
        }
    }

    public static class E extends D {
        public double getE() {
            return 0;
        }
    }

    public static class TestList extends ArrayList<Integer> {
    }

    public static class TestMap extends HashMap<String, Double> {
    }

    public static class TestSet extends HashSet<Double> {
    }

    public interface Response {
        @Required
        String getString();
        List<String> getStrings();
        Integer getNumber();
        Set<Integer> getNumbers();
        boolean getFlag();
        char getCharacter();
        DayOfWeek getDayOfWeek();
        Date getDate();
        List<Date> getDates();
        Instant getInstant();
        LocalDate getLocalDate();
        LocalTime getLocalTime();
        LocalDateTime getLocalDateTime();
        Duration getDuration();
        Period getPeriod();
        @Name("uuid")
        UUID getUUID();
    }

    public interface Body {
        @Required
        String getString();
        List<String> getStrings();
        Integer getNumber();
        Set<Integer> getNumbers();
        boolean getFlag();
    }

    public enum TestEnum {
        ONE,
        TWO,
        @Deprecated THREE
    }

    public record TestRecord(
        int number,
        @Deprecated TestEnum testEnum
    ) {
    }

    private static class FibonacciSequence implements Iterable<Number> {
        private int count;

        FibonacciSequence(int count) {
            this.count = count;
        }

        @Override
        public Iterator<Number> iterator() {
            return new Iterator<>() {
                int i = 0;

                BigInteger a = BigInteger.valueOf(0);
                BigInteger b = BigInteger.valueOf(1);

                @Override
                public boolean hasNext() {
                    return i < count;
                }

                @Override
                public BigInteger next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    BigInteger next;
                    if (i == 0) {
                        next = a;
                    } else {
                        if (i > 1) {
                            var c = a.add(b);

                            a = b;
                            b = c;
                        }

                        next = b;
                    }

                    i++;

                    return next;
                }
            };
        }
    }

    @RequestMethod("GET")
    public Response testGet(@Required String string, List<String> strings,
        Integer number, Set<Integer> numbers, boolean flag, char character, DayOfWeek dayOfWeek,
        Date date, List<Date> dates,
        Instant instant, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
        Duration duration, Period period,
        UUID uuid) {
        return BeanAdapter.coerce(mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("numbers", numbers),
            entry("flag", flag),
            entry("character", character),
            entry("dayOfWeek", dayOfWeek),
            entry("date", date),
            entry("dates", dates),
            entry("instant", instant),
            entry("localDate", localDate),
            entry("localTime", localTime),
            entry("localDateTime", localDateTime),
            entry("duration", duration),
            entry("period", period),
            entry("uuid", uuid)
        ), Response.class);
    }

    @RequestMethod("GET")
    @ResourcePath("a#/?/b*/?/c@/?/d=/?")
    public Map<String, Object> testKeys(
        @Description("First key.") int a,
        @Description("Second key.") String b,
        @Description("Third key.") int c,
        @Description("Fourth key.") String d
    ) {
        return mapOf(
            entry("a", a),
            entry("b", b),
            entry("c", c),
            entry("d", d)
        );
    }

    @RequestMethod("POST")
    @ResourcePath("foo/?/bar/?")
    public Map<String, Object> testParameters(int x, int y, int a, int b, List<Double> values) {
        return mapOf(
            entry("x", x),
            entry("y", y),
            entry("a", a),
            entry("b", b),
            entry("values", values)
        );
    }

    @RequestMethod("POST")
    @ResourcePath("varargs")
    public Map<String, Object> testVarargs(int[] numbers, String... strings) {
        return mapOf(
            entry("numbers", Arrays.stream(numbers).boxed().toList()),
            entry("strings", Arrays.stream(strings).toList())
        );
    }

    @RequestMethod("GET")
    @ResourcePath("fibonacci")
    public Iterable<Number> testGetFibonacci(int count) {
        return new FibonacciSequence(count);
    }

    @RequestMethod("GET")
    @ResourcePath("c")
    public C testGetC() {
        return null;
    }

    @RequestMethod("GET")
    @ResourcePath("e")
    public E testGetE() {
        return null;
    }

    @RequestMethod("GET")
    @ResourcePath("list")
    public TestList testGetList() {
        return new TestList();
    }

    @RequestMethod("GET")
    @ResourcePath("map")
    public TestMap testGetMap() {
        return new TestMap();
    }

    @RequestMethod("GET")
    @ResourcePath("set")
    public TestSet testGetSet() {
        return new TestSet();
    }

    @RequestMethod("POST")
    public void testPost(@Required Integer number, List<String> strings) {
        if (strings.size() != number) {
            throw new IllegalArgumentException("Invalid number.");
        }
    }

    @RequestMethod("POST")
    @ResourcePath("list")
    public List<String> testPostList(List<String> list) {
        return list;
    }

    @RequestMethod("POST")
    @ResourcePath("map")
    public Map<String, Double> testPostMap(Map<String, Double> map) {
        return map;
    }

    @RequestMethod("POST")
    @ResourcePath("body")
    @Creates
    public Body testPostBody(Body body) {
        body.getString();

        return body;
    }

    @RequestMethod("POST")
    @ResourcePath("coordinates")
    public Coordinates testPostCoordinates(Coordinates coordinates) {
        return coordinates;
    }

    @RequestMethod("POST")
    @ResourcePath("form-data")
    @FormData
    public Map<String, Object> testPostFormData(@Required String string, List<String> strings,
        Integer number, Date date,
        Part file, List<Part> files) {
        var fileSize = 0L;

        if (file != null) {
            fileSize += file.getSize();
        }

        var totalFileSize = fileSize;

        if (files != null) {
            for (var part : files) {
                totalFileSize += part.getSize();
            }
        }

        return mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("date", date),
            entry("fileSize", fileSize),
            entry("totalFileSize", totalFileSize)
        );
    }

    @RequestMethod("POST")
    @ResourcePath("image")
    public void testPostImage(Void body) throws IOException {
        echo();
    }

    @RequestMethod("POST")
    @ResourcePath("deferred")
    @Accepts
    public int testPostDeferred(Void body) {
        return 0;
    }

    @RequestMethod("PUT")
    public void testPut(Void body) throws IOException {
        echo();
    }

    private void echo() throws IOException {
        var inputStream = getRequest().getInputStream();
        var outputStream = getResponse().getOutputStream();

        int b;
        while ((b = inputStream.read()) != -1) {
            outputStream.write(b);
        }

        outputStream.flush();
    }

    @RequestMethod("PUT")
    @ResourcePath("?")
    public int testEmptyPut(int id, String value) {
        return id;
    }

    @RequestMethod("DELETE")
    @ResourcePath("?")
    public Integer testDelete(Integer id) {
        return id;
    }

    @RequestMethod("GET")
    @ResourcePath("headers")
    public Map<String, String> testHeaders() {
        var request = getRequest();

        return mapOf(
            entry("X-Header-A", request.getHeader("X-Header-A")),
            entry("X-Header-B", request.getHeader("X-Header-B"))
        );
    }

    @RequestMethod("GET")
    @ResourcePath("deprecated")
    @Deprecated
    public TestRecord testDeprecated() {
        return null;
    }

    @RequestMethod("GET")
    @ResourcePath("error")
    public void testError(boolean committed) throws Exception {
        if (committed) {
            var textEncoder = new TextEncoder();

            textEncoder.write("abc", getResponse().getOutputStream());
        }

        throw new Exception("Sample error message.");
    }

    @RequestMethod("GET")
    @ResourcePath("invalid-result")
    public List<Double> testInvalidResult() {
        return listOf(1.0, 2.0, 3.0, Double.NaN);
    }

    @RequestMethod("GET")
    public int testTimeout(int value, int delay) throws InterruptedException {
        Thread.sleep(delay);

        return value;
    }

    @RequestMethod("GET")
    @ResourcePath("math/sum")
    public double getSum(double a, double b) {
        return getInstance(MathService.class).getSum(a, b);
    }

    @RequestMethod("GET")
    @ResourcePath("math/sum")
    public double getSum(List<Double> values) {
        return getInstance(MathService.class).getSum(values);
    }
}
