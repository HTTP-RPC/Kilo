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

import org.httprpc.kilo.RequestHandler;
import org.httprpc.kilo.WebServiceProxy;
import org.httprpc.kilo.io.CSVEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.*;

public class BulkUploadTest {
    public static class Rows implements Iterable<Map<String, Object>> {
        private int count;

        private int i = 0;

        public Rows(int count) {
            this.count = count;
        }

        private Map<String, Object> row = mapOf(
            entry("text1", "abcdefghijklmnopqrstuvwxyz"),
            entry("text2", "ABCDEFG"),
            entry("number1", 123456),
            entry("number2", 101.05),
            entry("number3", 2002.0125)
        );

        @Override
        public Iterator<Map<String, Object>> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return i < count;
                }

                @Override
                public Map<String, Object> next() {
                    i++;

                    return row;
                }
            };
        }
    }

    public static void main(String[] args) throws IOException {
        var baseURI = URI.create("http://localhost:8080/kilo-test/bulk-upload/");

        var t0 = System.currentTimeMillis();

        logTiming(baseURI, "upload", 25000, t0);

        var t1 = System.currentTimeMillis();

        logTiming(baseURI, "upload-batch", 500000, t1);
    }

    private static void logTiming(URI baseURI, String path, int count, long start) throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURI, path);

        webServiceProxy.setBody(new Rows(count));

        webServiceProxy.setRequestHandler(new RequestHandler() {
            @Override
            public String getContentType() {
                return "text/csv";
            }

            @Override
            public void encodeRequest(Object body, OutputStream outputStream) throws IOException {
                var csvEncoder = new CSVEncoder(listOf("text1", "text2", "number1", "number2", "number3"));

                csvEncoder.write((Rows)body, outputStream);
            }
        });

        webServiceProxy.setReadTimeout(120000);
        webServiceProxy.setChunkSize(65536);

        webServiceProxy.invoke();

        var current = System.currentTimeMillis();

        System.out.println(String.format("Uploaded %d rows in %.1fs", count, (current - start) / 1000.0));
    }
}
