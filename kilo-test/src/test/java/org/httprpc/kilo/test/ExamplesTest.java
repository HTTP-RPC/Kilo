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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;

public class ExamplesTest {
    @Test
    public void testMathService() throws IOException {
        var baseURL = new URL("http://localhost:8080/kilo-test/");

        var webServiceProxy = WebServiceProxy.get(new URL(baseURL, "math/sum"));

        // GET /math/sum?a=2&b=4
        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        System.out.println(webServiceProxy.invoke()); // 6.0

        // GET /math/sum?values=1&values=2&values=3
        webServiceProxy.setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        ));

        System.out.println(webServiceProxy.invoke()); // 6.0

        // GET /math/sum?a=2&b=4
        System.out.println(WebServiceProxy.get(baseURL, "math/sum").setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        )).invoke()); // 6.0

        // GET /math/sum?values=1&values=2&values=3
        System.out.println(WebServiceProxy.get(baseURL, "math/sum").setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        )).invoke()); // 6.0
    }

    @Test
    public void testJSONEncoder() throws IOException {
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
        var map = mapOf(
            entry("a", "hello"),
            entry("b", 123),
            entry("c", true)
        );

        var templateEncoder = new TemplateEncoder(getClass().getResource("example.html"));

        templateEncoder.write(map, System.out);
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
        } catch (UnsupportedOperationException exception) {
            System.out.println(exception.getMessage());
        }
    }

    @Test
    public void testTreeNode() throws IOException {
        var writer = new StringWriter();

        testEncodeTreeNode(writer);

        var text = writer.toString();

        System.out.println(text);

        testDecodeTreeNode(new StringReader(text));
    }

    private void testEncodeTreeNode(Writer writer) throws IOException {
        var root = new TreeNode("Seasons", listOf(
            new TreeNode("Winter", listOf(
                new TreeNode("January", null),
                new TreeNode("February", null),
                new TreeNode("March", null)
            )),
            new TreeNode("Spring", listOf(
                new TreeNode("April", null),
                new TreeNode("May", null),
                new TreeNode("June", null)
            )),
            new TreeNode("Summer", listOf(
                new TreeNode("July", null),
                new TreeNode("August", null),
                new TreeNode("September", null)
            )),
            new TreeNode("Fall", listOf(
                new TreeNode("October", null),
                new TreeNode("November", null),
                new TreeNode("December", null)
            ))
        ));

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(new BeanAdapter(root), writer);
    }

    private void testDecodeTreeNode(Reader reader) throws IOException {
        var jsonDecoder = new JSONDecoder();

        var root = BeanAdapter.coerce(jsonDecoder.read(reader), TreeNode.class);

        System.out.println(root.getName()); // Seasons
        System.out.println(root.getChildren().get(0).getName()); // Winter
        System.out.println(root.getChildren().get(0).getChildren().get(0).getName()); // January
    }

    @Test
    public void testInterfaceProxy() {
        var map = mapOf(
            entry("employeeNumber", 10001),
            entry("firstName", "Georgi"),
            entry("lastName", "Facello"),
            entry("gender", "M"),
            entry("birthDate", "1953-09-02"),
            entry("hireDate", "1986-06-26")
        );

        var employee = BeanAdapter.coerce(map, Employee.class);

        System.out.println(employee.getEmployeeNumber()); // 10001
        System.out.println(employee.getFirstName()); // Georgi
        System.out.println(employee.getLastName()); // Facello
        System.out.println(employee.getGender()); // M
        System.out.println(employee.getBirthDate()); // 1953-09-02
        System.out.println(employee.getHireDate()); // 1986-06-26
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
