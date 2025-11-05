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

import org.httprpc.kilo.io.TemplateEncoder;
import org.httprpc.kilo.xml.ElementAdapter;
import org.w3c.dom.Document;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.nio.file.Files;
import java.nio.file.Paths;

public class XMLTransformTest {
    public static void main(String[] args) throws Exception {
        var xmlTransformTest = new XMLTransformTest();

        var t0 = System.currentTimeMillis();

        xmlTransformTest.transformXML1();

        var t1 = System.currentTimeMillis();

        System.out.printf("%s %dms\n", Transformer.class.getSimpleName(), (t1 - t0));

        xmlTransformTest.transformXML2();

        var t2 = System.currentTimeMillis();

        System.out.printf("%s %dms\n", TemplateEncoder.class.getSimpleName(), (t2 - t1));
    }

    private void transformXML1() throws Exception {
        var source = new StreamSource(getClass().getResourceAsStream("breakfast-menu.xslt"));

        var transformer = TransformerFactory.newInstance().newTransformer(source);

        var xmlSource = new StreamSource(getClass().getResourceAsStream("breakfast-menu.xml"));

        var outputFile = Paths.get(System.getProperty("user.home"), "breakfast-menu-1.html");

        try (var outputStream = Files.newOutputStream(outputFile)) {
            transformer.transform(xmlSource, new StreamResult(outputStream));
        }
    }

    private void transformXML2() throws Exception {
        var documentBuilder = ElementAdapter.newDocumentBuilder();

        Document document;
        try (var inputStream = getClass().getResourceAsStream("breakfast-menu.xml")) {
            document = documentBuilder.parse(inputStream);
        }

        var templateEncoder = new TemplateEncoder(getClass(), "breakfast-menu.html");

        var outputFile = Paths.get(System.getProperty("user.home"), "breakfast-menu-2.html");

        try (var outputStream = Files.newOutputStream(outputFile)) {
            templateEncoder.write(new ElementAdapter(document.getDocumentElement()), outputStream);
        }
    }
}
