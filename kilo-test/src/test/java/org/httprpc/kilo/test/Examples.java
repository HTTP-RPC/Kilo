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

import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.CSVEncoder;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.io.TextEncoder;
import org.httprpc.kilo.xml.ElementAdapter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.*;

public class Examples {
    public interface Example {
        void execute() throws Exception;
    }

    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    public static void main(String[] args) {
        execute("Math Service 1", Examples::mathService1);
        execute("Math Service 2", Examples::mathService2);
        execute("Math Service 3", Examples::mathService3);
        execute("JSON Encoder", Examples::jsonEncoderAndDecoder);
        execute("Text Encoder/Decoder", Examples::textEncoderAndDecoder);
        execute("CSV Encoder", Examples::csvEncoder);
        execute("Template Encoder", Examples::templateEncoder);
        execute("Adapt Bean", Examples::adaptBean);
        execute("Coerce Bean", Examples::coerceBean);
        execute("Interface Proxy", Examples::interfaceProxy);
        execute("Required Property 1", Examples::requiredProperty1);
        execute("Required Property 2", Examples::requiredProperty2);
        execute("Custom Property Keys", Examples::customPropertyKeys);
        execute("Element Adapter", Examples::elementAdapter);
    }

    private static void execute(String label, Example example) {
        System.out.println(label);

        try {
            example.execute();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }

        System.out.println();
    }

    public static void mathService1() throws IOException {
        // GET /math/sum?a=2&b=4
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        System.out.println(webServiceProxy.invoke()); // 6.0
    }

    public static void mathService2() throws IOException {
        // GET /math/sum?values=1&values=2&values=3
        var webServiceProxy = new WebServiceProxy("GET", baseURI.resolve("math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        ));

        System.out.println(webServiceProxy.invoke()); // 6.0
    }

    public static void mathService3() throws IOException {
        var mathServiceProxy = WebServiceProxy.of(MathServiceProxy.class, baseURI);

        System.out.println(mathServiceProxy.getSum(4, 2)); // 6.0
        System.out.println(mathServiceProxy.getSum(listOf(1.0, 2.0, 3.0))); // 6.0

        System.out.println(mathServiceProxy.getAverage(listOf(1.0, 2.0, 3.0, 4.0, 5.0))); // 3.0
    }

    @SuppressWarnings("unchecked")
    public static void jsonEncoderAndDecoder() throws IOException {
        var file = Files.createTempFile("kilo", ".json");

        var map = mapOf(
            entry("vegetables", listOf(
                "carrots",
                "peas",
                "potatoes"
            )),
            entry("desserts", listOf(
                "cookies",
                "cake",
                "ice cream"
            ))
        );

        try {
            try (var outputStream = Files.newOutputStream(file)) {
                var jsonEncoder = new JSONEncoder();

                jsonEncoder.write(map, outputStream);
            }

            try (var inputStream = Files.newInputStream(file)) {
                var jsonDecoder = new JSONDecoder();

                map = (Map<String, List<String>>)jsonDecoder.read(inputStream);
            }

            System.out.println(map.get("vegetables").get(0)); // carrots
        } finally {
            Files.delete(file);
        }
    }

    public static void textEncoderAndDecoder() throws IOException {
        var file = Files.createTempFile("kilo", ".txt");

        var text = "Hello, World!";

        try {
            try (var outputStream = Files.newOutputStream(file)) {
                var textEncoder = new TextEncoder();

                textEncoder.write(text, outputStream);
            }

            try (var inputStream = Files.newInputStream(file)) {
                var textDecoder = new TextDecoder();

                text = textDecoder.read(inputStream);
            }

            System.out.println(text); // Hello, World!
        } finally {
            Files.delete(file);
        }
    }

    public static void csvEncoder() throws IOException {
        var maps = listOf(
            mapOf(
                entry("a", "hello"),
                entry("b", 123),
                entry("c", true)
            ),
            mapOf(
                entry("a", "goodbye"),
                entry("b", 456),
                entry("c", false)
            )
        );

        var csvEncoder = new CSVEncoder(listOf("a", "b", "c"));

        csvEncoder.write(maps, System.out);
    }

    public static void templateEncoder() throws Exception {
        var map = mapOf(
            entry("a", "hello"),
            entry("b", 123),
            entry("c", true)
        );

        var templateEncoder = new TemplateEncoder(Examples.class, "example.html");

        templateEncoder.write(map, System.out);
    }

    public static void adaptBean() {
        var course = new Course();

        course.setName("CS 101");
        course.setBuilding("Technology Lab");
        course.setRoomNumber(210);

        var courseAdapter = new BeanAdapter(course);

        System.out.println(courseAdapter.get("name")); // CS 101
        System.out.println(courseAdapter.get("building")); // Technology Lab
        System.out.println(courseAdapter.get("roomNumber")); // 210
    }

    public static void coerceBean() {
        var map = mapOf(
            entry("name", "CS 101"),
            entry("building", "Technology Lab"),
            entry("roomNumber", 210)
        );

        var course = BeanAdapter.coerce(map, Course.class);

        System.out.println(course.getName()); // CS 101
        System.out.println(course.getBuilding()); // Technology Lab
        System.out.println(course.getRoomNumber()); // 210
    }

    public static void interfaceProxy() {
        var map = mapOf(
            entry("date", "2024-04-08"),
            entry("conditions", "cloudy"),
            entry("high", 52.1),
            entry("low", 43.5)
        );

        var weather = BeanAdapter.coerce(map, Weather.class);

        System.out.println(weather.getDate()); // 2024-04-08
        System.out.println(weather.getConditions()); // cloudy
        System.out.println(weather.getHigh()); // 52.1
        System.out.println(weather.getLow()); // 43.5
    }

    public static void requiredProperty1() {
        try {
            BeanAdapter.coerce(mapOf(), Vehicle.class);
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
    }

    public static void requiredProperty2() {
        var vehicle = new Vehicle();

        var vehicleAdapter = new BeanAdapter(vehicle);

        try {
            vehicleAdapter.put("manufacturer", null);
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }

        try {
            vehicleAdapter.get("manufacturer");
        } catch (UnsupportedOperationException exception) {
            System.out.println(exception.getMessage());
        }
    }

    public static void customPropertyKeys() throws IOException {
        var person = new Person();

        person.setFirstName("first");
        person.setLastName("last");

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(person, System.out);

        System.out.println();
    }

    @SuppressWarnings("unchecked")
    public static void elementAdapter() throws ParserConfigurationException, SAXException, IOException {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        var documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document;
        try (var inputStream = Examples.class.getResourceAsStream("account.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        var accountAdapter = new ElementAdapter(document.getDocumentElement());

        var id = accountAdapter.get("@id");

        System.out.println(id); // 101

        var holder = (Map<String, Object>)accountAdapter.get("holder");

        var firstName = holder.get("firstName");
        var lastName = holder.get("lastName");

        System.out.println(String.format("%s, %s", lastName, firstName)); // Smith, John

        var transactions = (Map<String, Object>)accountAdapter.get("transactions");
        var credits = (List<Map<String, Object>>)transactions.get("credit*");

        for (var credit : credits) {
            System.out.println(credit.get("amount"));
            System.out.println(credit.get("date"));
        }
    }
}
