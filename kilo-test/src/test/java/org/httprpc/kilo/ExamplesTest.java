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

package org.httprpc.kilo;

import org.httprpc.kilo.beans.BeanAdapter;
import org.httprpc.kilo.beans.Key;
import org.httprpc.kilo.io.CSVDecoder;
import org.httprpc.kilo.io.CSVEncoder;
import org.httprpc.kilo.io.JSONDecoder;
import org.httprpc.kilo.io.JSONEncoder;
import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.io.TextDecoder;
import org.httprpc.kilo.io.TextEncoder;
import org.httprpc.kilo.util.ResourceBundleAdapter;
import org.httprpc.kilo.xml.ElementAdapter;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;

public class ExamplesTest {
    public static class Person {
        private String firstName = null;
        private String lastName = null;

        @Key("first_name")
        @Required
        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        @Key("last_name")
        @Required
        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    public static class Vehicle {
        private String manufacturer;
        private Integer year;

        @Required
        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        @Required
        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }
    }

    public record Employee(String badgeNumber, String firstName, String lastName) {
    }

    public interface TreeNode {
        String getName();
        List<WebServiceProxyTest.TreeNode> getChildren();
    }

    @Test
    public void testMathService() throws IOException {
        var baseURL = new URL("http://localhost:8080/kilo-test/");

        var webServiceProxy = new WebServiceProxy("GET", new URL(baseURL, "math/sum"));

        // GET /math/sum?a=2&b=4
        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        System.out.println(webServiceProxy.invoke(Double.class)); // 6.0

        // GET /math/sum?values=1&values=2&values=3
        webServiceProxy.setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        ));

        System.out.println(webServiceProxy.invoke(Double.class)); // 6.0

        // GET /math/sum?a=2&b=4
        System.out.println(WebServiceProxy.get(baseURL, "math/sum").setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        )).invoke(Double.class)); // 6.0

        // GET /math/sum?values=1&values=2&values=3
        System.out.println(WebServiceProxy.get(baseURL, "math/sum").setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        )).invoke(Double.class)); // 6.0
    }

    @Test
    public void testJSONEncoder() throws IOException {
        Map<String, Object> map = mapOf(
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
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJSONDecoder() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("months.json")) {
            var jsonDecoder = new JSONDecoder();

            var months = (List<Map<String, Object>>)jsonDecoder.read(inputStream);

            for (var month : months) {
                System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCSVEncoder() throws IOException {
        List<Map<String, Object>> months;
        try (var inputStream = getClass().getResourceAsStream("months.json")) {
            var jsonDecoder = new JSONDecoder();

            months = (List<Map<String, Object>>)jsonDecoder.read(inputStream);
        }

        var csvEncoder = new CSVEncoder(listOf("name", "days"));

        csvEncoder.write(months, System.out);
    }

    @Test
    public void testCSVDecoder() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("months.csv")) {
            var csvDecoder = new CSVDecoder();

            var months = csvDecoder.read(inputStream);

            for (var month : months) {
                System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
            }
        }
    }

    @Test
    public void testTextEncoderAndDecoder() throws IOException {
        var file = File.createTempFile("kilo", ".txt");

        try {
            try (var outputStream = new FileOutputStream(file)) {
                var textEncoder = new TextEncoder();

                textEncoder.write("Hello, World!", outputStream);
            }

            String text;
            try (var inputStream = new FileInputStream(file)) {
                var textDecoder = new TextDecoder();

                text = textDecoder.read(inputStream);
            }

            System.out.println(text);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testTemplateEncoder() throws IOException {
        Map<String, Object> map = mapOf(
            entry("a", "hello"),
            entry("b", 123),
            entry("c", true)
        );

        var templateEncoder = new TemplateEncoder(getClass().getResource("example.txt"));

        templateEncoder.write(map, System.out);
    }

    @Test
    public void testModifier() throws IOException {
        var templateEncoder = new TemplateEncoder(getClass().getResource("modifier.txt"));

        templateEncoder.getModifiers().put("uppercase", (value, argument, locale, timeZone) -> value.toString().toUpperCase(locale));

        templateEncoder.write(mapOf(
            entry("text", "hello")
        ), System.out);
    }

    @Test
    public void testDoubleCoercion() {
        var value = BeanAdapter.coerce("2.5", Double.class);

        System.out.println(value + 10); // 12.5
    }

    @Test
    public void testListCoercion() throws IOException {
        var list = BeanAdapter.coerce(listOf(
            "1",
            "2",
            "3"
        ), List.class, Integer.class);

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(list, System.out);
    }

    @Test
    public void testCustomPropertyKeys() throws IOException {
        var person = new Person();

        person.setFirstName("first");
        person.setLastName("last");

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(new BeanAdapter(person), System.out);
    }

    @Test
    public void testRequiredProperty1() {
        try {
            var vehicle = BeanAdapter.coerce(mapOf(), Vehicle.class);
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @Test
    public void testRequiredProperty2() {
        var vehicle = new Vehicle();

        var vehicleAdapter = new BeanAdapter(vehicle);

        try {
            vehicleAdapter.put("manufacturer", null);
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
        }

        try {
            vehicleAdapter.get("manufacturer");
        } catch (IllegalStateException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @Test
    public void testRecord() throws IOException {
        var employee = new Employee("123", "John", "Smith");

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(BeanAdapter.adapt(employee), System.out);
    }

    @Test
    public void testTreeNode() throws IOException {
        try (var inputStream = getClass().getResourceAsStream("tree.json")) {
            var jsonDecoder = new JSONDecoder();

            var root = BeanAdapter.coerce(jsonDecoder.read(inputStream), TreeNode.class);

            System.out.println(root.getName()); // Seasons
            System.out.println(root.getChildren().get(0).getName()); // Winter
            System.out.println(root.getChildren().get(0).getChildren().get(0).getName()); // January
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testElementAdapter() throws ParserConfigurationException, SAXException, IOException {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilderFactory.setExpandEntityReferences(false);
        documentBuilderFactory.setIgnoringComments(true);

        var documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document document;
        try (var inputStream = getClass().getResourceAsStream("account.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        var accountAdapter = new ElementAdapter(document.getDocumentElement());

        var holder = (Map<String, Object>)accountAdapter.get("holder");

        var firstName = holder.get("firstName");
        var lastName = holder.get("lastName");

        System.out.println(String.format("%s, %s", lastName, firstName)); // Smith, John

        var id = accountAdapter.get("@id");

        System.out.println(id); // 101

        var transactions = (Map<String, Object>)accountAdapter.get("transactions");
        var credits = (List<Map<String, Object>>)transactions.get("credit*");

        for (var credit : credits) {
            System.out.println(credit.get("amount"));
            System.out.println(credit.get("date"));
        }
    }

    @Test
    public void testResourceBundleAdapter() throws IOException {
        var csvEncoder = new CSVEncoder(listOf("name", "description", "quantity"));

        var resourceBundle = ResourceBundle.getBundle(getClass().getPackageName() + ".labels");

        csvEncoder.setLabels(new ResourceBundleAdapter(resourceBundle));

        csvEncoder.write(listOf(
            mapOf(
                entry("name", "Item 1"),
                entry("description", "Item number 1"),
                entry("quantity", 3)
            ),
            mapOf(
                entry("name", "Item 2"),
                entry("description", "Item number 2"),
                entry("quantity", 5)
            ),
            mapOf(
                entry("name", "Item 3"),
                entry("description", "Item number 3"),
                entry("quantity", 7)
            )
        ), System.out);
    }
}
