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
import org.httprpc.kilo.Description;
import org.httprpc.kilo.Name;
import org.httprpc.kilo.RequestMethod;
import org.httprpc.kilo.Required;
import org.httprpc.kilo.ResourcePath;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.TextEncoder;
import org.httprpc.kilo.util.Collections;
import org.w3c.dom.Document;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.UUID;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.httprpc.kilo.util.Optionals.*;

@WebServlet(urlPatterns = "/test/*", loadOnStartup = 0)
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

    public interface Response {
        @Required
        String getString();
        List<String> getStrings();
        Integer getNumber();
        Set<Integer> getNumbers();
        boolean getFlag();
        DayOfWeek getDayOfWeek();
        Instant getDate();
        List<Instant> getDates();
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

    public record FormContents(
        @Required
        String string,
        List<String> strings,
        Integer number,
        Instant date,
        Path file,
        List<Path> files
    ) {
    }

    public record TestRecord(
        int number,
        @Deprecated
        TestEnum testEnum
    ) {
        public enum TestEnum {
            ONE,
            TWO,
            @Deprecated THREE
        }
    }

    private static class FibonacciIterator implements Iterator<Number> {
        int count;

        int i = 0;

        BigInteger a = BigInteger.valueOf(0);
        BigInteger b = BigInteger.valueOf(1);

        FibonacciIterator(int count) {
            this.count = count;
        }

        @Override
        public boolean hasNext() {
            return i < count;
        }

        @Override
        public Number next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            try {
                if (i == 0) {
                    return a;
                } else {
                    if (i > 1) {
                        var c = a.add(b);

                        a = b;
                        b = c;
                    }

                    return b;
                }
            } finally {
                i++;
            }
        }
    }

    private @Instance MathService mathService = null;

    @RequestMethod("GET")
    public Response testGet(@Required String string, List<String> strings,
        int number, SequencedSet<Integer> numbers,
        boolean flag, DayOfWeek dayOfWeek,
        Instant date, List<Instant> dates,
        LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
        Duration duration, Period period,
        UUID uuid) {
        return BeanAdapter.coerce(mapOf(
            entry("string", string),
            entry("strings", strings),
            entry("number", number),
            entry("numbers", numbers),
            entry("flag", flag),
            entry("dayOfWeek", dayOfWeek),
            entry("date", date),
            entry("dates", dates),
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
            entry("strings", listOf(strings))
        );
    }

    @RequestMethod("GET")
    @ResourcePath("fibonacci")
    public Iterable<Number> testGetFibonacci(int count) {
        return () -> new FibonacciIterator(count);
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
    public SequencedMap<String, Double> testPostMap(SequencedMap<String, Double> map) {
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
    public Map<String, Object> testPostFormData(FormContents formContents) {
        var fileSize = coalesce(map(formContents.file(), TestService::lengthOf), () -> 0L);
        var totalFileSize = sumOf(coalesce(mapAll(formContents.files(), TestService::lengthOf), Collections::listOf), Long::longValue) + fileSize;

        return mapOf(
            entry("string", formContents.string()),
            entry("strings", formContents.strings()),
            entry("number", formContents.number()),
            entry("date", formContents.date()),
            entry("fileSize", fileSize),
            entry("totalFileSize", totalFileSize)
        );
    }

    private static long lengthOf(Path path) {
        return path.toFile().length();
    }

    @RequestMethod("POST")
    @ResourcePath("xml")
    public Document testPostXML(int a, int b, Document document) {
        return document;
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
    public int testEmptyPut(int id, String value, Void body) {
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
        return mathService.getSum(a, b);
    }

    @RequestMethod("GET")
    @ResourcePath("math/sum")
    public double getSum(List<Double> values) {
        return getInstance(MathService.class).getSum(values);
    }
}
