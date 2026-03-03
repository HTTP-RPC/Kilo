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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class BulkUploadServiceTest {
    public static class Rows implements Iterable<Row> {
        private int count;

        private int i = 0;

        public Rows(int count) {
            this.count = count;
        }

        private Row row = new Row("abcdefghijklmnopqrstuvwxyz", "ABCDEFG", 123456, 101.05, 2002.0125);

        @Override
        public Iterator<Row> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return i < count;
                }

                @Override
                public Row next() {
                    i++;

                    return row;
                }
            };
        }
    }

    private static final int ROW_COUNT = 15000;

    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testBulkUpload() throws IOException {
        var t0 = System.currentTimeMillis();

        uploadRows("bulk-upload");

        var t1 = System.currentTimeMillis();

        uploadRows("bulk-upload/batch");

        var t2 = System.currentTimeMillis();

        assertTrue(t2 - t1 < (t1 - t0) / 5.0);
    }

    private static void uploadRows(String path) throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve(path));

        webServiceProxy.setBody(new Rows(ROW_COUNT));

        webServiceProxy.setChunkSize(4096);

        webServiceProxy.invoke();
    }
}
