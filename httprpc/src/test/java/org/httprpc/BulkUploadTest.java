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

package org.httprpc;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.httprpc.io.CSVEncoder;

public class BulkUploadTest extends AbstractTest {
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
            return new Iterator<Map<String,Object>>() {
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

    public static void main(String[] args) throws Exception {
        testUpload();
    }

    private static void testUpload() throws IOException {
        testUpload("http://localhost:8080/httprpc-test/bulk-upload/upload", "Upload", 5000);
        testUpload("http://localhost:8080/httprpc-test/bulk-upload/upload-batch", "Upload Batch", 500000);
    }

    private static void testUpload(String url, String description, int count) throws IOException {
        long t0 = System.currentTimeMillis();

        WebServiceProxy webServiceProxy = new WebServiceProxy("POST", new URL(url));

        webServiceProxy.setRequestHandler((outputStream) -> {
            CSVEncoder csvEncoder = new CSVEncoder(listOf("text1", "text2", "number1", "number2", "number3"));

            csvEncoder.write(new Rows(count), outputStream);
        });

        webServiceProxy.invoke();

        long t1 = System.currentTimeMillis();

        System.out.printf("%s (%d) %.1f\n", description, count, (t1 - t0) / 1000.0);
    }
}