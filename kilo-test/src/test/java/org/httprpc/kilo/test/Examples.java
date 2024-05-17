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
import org.httprpc.kilo.xml.ElementAdapter;
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

import static org.httprpc.kilo.util.Collections.*;

public class Examples {
    public interface Example {
        void execute() throws Exception;
    }

    public static void main(String[] args) {
        execute("Math Service 1", Examples::mathService1);
        execute("Math Service 2", Examples::mathService2);
        execute("Math Service 3", Examples::mathService3);
        execute("JSON Encoder", Examples::jsonEncoder);
        execute("JSON Decoder", Examples::jsonDecoder);
        execute("CSV Encoder", Examples::csvEncoder);
        execute("CSV Decoder", Examples::csvDecoder);
        execute("Text Encoder/Decoder", Examples::textEncoderAndDecoder);
        execute("Template Encoder", Examples::templateEncoder);
        execute("Custom Property Keys", Examples::customPropertyKeys);
        execute("Required Property 1", Examples::requiredProperty1);
        execute("Required Property 2", Examples::requiredProperty2);
        execute("Tree Node", Examples::treeNode);
        execute("Interface Proxy", Examples::interfaceProxy);
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
        var webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/kilo-test/math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("a", 4),
            entry("b", 2)
        ));

        System.out.println(webServiceProxy.invoke()); // 6.0
    }

    public static void mathService2() throws IOException {
        // GET /math/sum?values=1&values=2&values=3
        var webServiceProxy = new WebServiceProxy("GET", new URL("http://localhost:8080/kilo-test/math/sum"));

        webServiceProxy.setArguments(mapOf(
            entry("values", listOf(1, 2, 3))
        ));

        System.out.println(webServiceProxy.invoke()); // 6.0
    }

    public static void mathService3() throws IOException {
        var mathServiceProxy = WebServiceProxy.of(MathServiceProxy.class, new URL("http://localhost:8080/kilo-test/math/"));

        System.out.println(mathServiceProxy.getSum(4, 2)); // 6.0
        System.out.println(mathServiceProxy.getSum(listOf(1.0, 2.0, 3.0))); // 6.0

        System.out.println(mathServiceProxy.getAverage(listOf(1.0, 2.0, 3.0, 4.0, 5.0))); // 3.0
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

    @SuppressWarnings("unchecked")
    public static void jsonDecoder() throws IOException {
        try (var inputStream = Examples.class.getResourceAsStream("months.json")) {
            var jsonDecoder = new JSONDecoder();

            var months = (List<Map<String, Object>>)jsonDecoder.read(inputStream);

            for (var month : months) {
                System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void csvEncoder() throws IOException {
        List<Map<String, Object>> months;
        try (var inputStream = Examples.class.getResourceAsStream("months.json")) {
            var jsonDecoder = new JSONDecoder();

            months = (List<Map<String, Object>>)jsonDecoder.read(inputStream);
        }

        var csvEncoder = new CSVEncoder(listOf("name", "days"));

        csvEncoder.write(months, System.out);
    }

    public static void csvDecoder() throws IOException {
        try (var inputStream = Examples.class.getResourceAsStream("months.csv")) {
            var csvDecoder = new CSVDecoder();

            var months = csvDecoder.read(inputStream);

            for (var month : months) {
                System.out.println(String.format("%s has %s days", month.get("name"), month.get("days")));
            }
        }
    }

    public static void textEncoderAndDecoder() throws IOException {
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

    public static void templateEncoder() throws IOException {
        var map = mapOf(
            entry("a", "hello"),
            entry("b", 123),
            entry("c", true)
        );

        var templateEncoder = new TemplateEncoder(Examples.class.getResource("example.html"));

        templateEncoder.write(map, System.out);
    }

    public static void customPropertyKeys() throws IOException {
        var person = new Person();

        person.setFirstName("first");
        person.setLastName("last");

        var jsonEncoder = new JSONEncoder();

        jsonEncoder.write(new BeanAdapter(person), System.out);

        System.out.println();
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

    public static void treeNode() throws IOException {
        var writer = new StringWriter();

        encodeTreeNode(writer);

        var text = writer.toString();

        System.out.println(text);
        System.out.println();

        decodeTreeNode(new StringReader(text));
    }

    private static void encodeTreeNode(Writer writer) throws IOException {
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

    private static void decodeTreeNode(Reader reader) throws IOException {
        var jsonDecoder = new JSONDecoder();

        var root = BeanAdapter.coerce(jsonDecoder.read(reader), TreeNode.class);

        System.out.println(root.getName()); // Seasons
        System.out.println(root.getChildren().get(0).getName()); // Winter
        System.out.println(root.getChildren().get(0).getChildren().get(0).getName()); // January
    }

    public static void interfaceProxy() {
        var map = mapOf(
            entry("date", "2024-04-08T00:00:00Z"),
            entry("open", 169.03),
            entry("close", 168.45),
            entry("high", 169.20),
            entry("low", 168.24),
            entry("volume", 37216858)
        );

        var assetPricing = BeanAdapter.coerce(map, AssetPricing.class);

        System.out.println(assetPricing.getDate()); // 2024-04-08T00:00:00Z
        System.out.println(assetPricing.getOpen()); // 169.03
        System.out.println(assetPricing.getClose()); // 168.45
        System.out.println(assetPricing.getHigh()); // 169.2
        System.out.println(assetPricing.getLow()); // 168.24
        System.out.println(assetPricing.getVolume()); // 37216858
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
