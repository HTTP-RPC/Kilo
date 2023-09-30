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
import org.httprpc.kilo.io.CSVEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import static org.httprpc.kilo.util.Collections.entry;
import static org.httprpc.kilo.util.Collections.listOf;
import static org.httprpc.kilo.util.Collections.mapOf;

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
        // TODO Move invocations to dedicated method

        var baseURL = new URL("http://localhost:8080/kilo-test/bulk-upload/");

        var t0 = System.currentTimeMillis();

        var count = 25000;

        WebServiceProxy.post(baseURL, "upload").setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return "text/csv";
            }

            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                var csvEncoder = new CSVEncoder(listOf("text1", "text2", "number1", "number2", "number3"));

                csvEncoder.write(new Rows(count), outputStream);
            }
        }).invoke();

        var t1 = System.currentTimeMillis();

        System.out.println(String.format("Uploaded %d rows in %.1fs", count, (t1 - t0) / 1000.0));

        var batchCount = 500000;

        WebServiceProxy.post(baseURL, "upload-batch").setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return "text/csv";
            }

            @Override
            public void encodeRequest(OutputStream outputStream) throws IOException {
                var csvEncoder = new CSVEncoder(listOf("text1", "text2", "number1", "number2", "number3"));

                csvEncoder.write(new Rows(batchCount), outputStream);
            }
        }).invoke();

        var t2 = System.currentTimeMillis();

        System.out.println(String.format("Uploaded %d rows in %.1fs", batchCount, (t2 - t1) / 1000.0));
    }
}