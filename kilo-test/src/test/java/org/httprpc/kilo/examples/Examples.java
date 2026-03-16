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

package org.httprpc.kilo.examples;

import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.io.CSVEncoder;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.io.TextEncoder;
import org.httprpc.kilo.xml.ElementAdapter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.*;

public class Examples {
    public interface Example {
        void execute() throws Exception;
    }

    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    public static void main(String[] args) {
        execute("Math Service 1", Examples::mathService1);
        execute("Math Service 2", Examples::mathService2);

        execute("JSON Encoder", Examples::jsonEncoder);
        execute("Text Encoder", Examples::textEncoder);
        execute("CSV Encoder", Examples::csvEncoder);

        execute("Template Encoder", Examples::templateEncoder);
        execute("Variables", Examples::variables);
        execute("Repeating Sections", Examples::repeatingSections);
        execute("Conditional Sections", Examples::conditionalSections);
        execute("Inverted Sections", Examples::invertedSections);
        execute("Resources", Examples::resources);
        execute("Includes", Examples::includes);
        execute("Comments", Examples::comments);

        execute("Adapt Bean", Examples::adaptBean);
        execute("Coerce Bean", Examples::coerceBean);
        execute("Interface Proxy", Examples::interfaceProxy);

        execute("Element Adapter", Examples::elementAdapter);

        execute("Collections", Examples::collections);
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

    public static void jsonEncoder() throws IOException {
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

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(map, System.out);

        System.out.println();
    }

    public static void textEncoder() throws IOException {
        var text = "Hello, World!";

        var textEncoder = new TextEncoder();

        textEncoder.write(text, System.out);

        System.out.println();
    }

    public static void csvEncoder() throws IOException {
        var rows = listOf(
            listOf("a", "b", "c"),
            listOf("d", "e", "f")
        );

        var csvEncoder = new CSVEncoder();

        csvEncoder.writeAll(rows, System.out);
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

    public static void variables() throws IOException {
        templateExample("variables");
    }

    public static void repeatingSections() throws IOException {
        templateExample("repeating-sections");
    }

    public static void conditionalSections() throws IOException {
        templateExample("conditional-sections");
    }

    public static void invertedSections() throws IOException {
        templateExample("inverted-sections");
    }

    public static void resources() throws IOException {
        templateExample("resources");
    }

    public static void includes() throws IOException {
        templateExample("includes");
    }

    public static void comments() throws IOException {
        templateExample("comments");
    }

    private static void templateExample(String name) throws IOException {
        var jsonDecoder = new JSONDecoder();

        Object dictionary;
        try (var inputStream = Examples.class.getResourceAsStream(String.format("%s.json", name))) {
            dictionary = jsonDecoder.read(new InputStreamReader(inputStream));
        }

        var templateEncoder = new TemplateEncoder(Examples.class, String.format("%s.html", name));

        try {
            var resourceBundle = ResourceBundle.getBundle(String.format("%s.%s", Examples.class.getPackageName(), name));

            templateEncoder.setResourceBundle(resourceBundle);
        } catch (MissingResourceException exception) {
            // No-op
        }

        templateEncoder.write(dictionary, System.out);
    }

    public static void adaptBean() {
        var course = new Course();

        course.setName("CS 101");
        course.setBuilding("Technology Lab");
        course.setRoomNumber(210);

        var map = new BeanAdapter(course);

        System.out.println(map.get("name")); // CS 101
        System.out.println(map.get("building")); // Technology Lab
        System.out.println(map.get("roomNumber")); // 210
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

    @SuppressWarnings("unchecked")
    public static void elementAdapter() throws SAXException, IOException {
        var documentBuilder = ElementAdapter.newDocumentBuilder();

        Document document;
        try (var inputStream = Examples.class.getResourceAsStream("account.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        var account = new ElementAdapter(document.getDocumentElement());

        var id = account.get("@id");

        System.out.println(id); // 101

        var holder = (Map<String, Object>)account.get("holder");

        var firstName = holder.get("firstName");
        var lastName = holder.get("lastName");

        System.out.println(String.format("%s, %s", lastName, firstName)); // Smith, John

        var transactions = (Map<String, Object>)account.get("transactions");
        var credits = (List<Map<String, Object>>)transactions.get("credit*");

        for (var credit : credits) {
            System.out.println(credit.get("amount"));
            System.out.println(credit.get("date"));
        }
    }

    public static void collections() {
        var list = listOf(1, 2, 3);

        System.out.println(list.getFirst()); // 1

        var map = mapOf(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3)
        );

        System.out.println(map.get("a")); // 1

        var set = setOf("a", "b", "c");

        System.out.println(set.contains("a")); // true
    }
}
