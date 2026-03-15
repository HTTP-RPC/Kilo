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
import org.httprpc.kilo.io.JSONEncoder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Iterator;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Iterables.*;
import static org.junit.jupiter.api.Assertions.*;

public class BulkUploadServiceTest {
    public static class Rows implements Iterable<Row> {
        @Override
        public Iterator<Row> iterator() {
            return new Iterator<>() {
                int i = 0;

                @Override
                public boolean hasNext() {
                    return i < ROW_COUNT;
                }

                @Override
                public Row next() {
                    try {
                        return BeanAdapter.coerce(mapOf(
                            entry("text1", getRandomString(32)),
                            entry("text2", getRandomString(32)),
                            entry("number1", getRandomNumber()),
                            entry("number2", getRandomNumber()),
                            entry("number3", getRandomNumber())
                        ), Row.class);
                    } finally {
                        i++;
                    }
                }
            };
        }
    }

    private static final int ROW_COUNT = 32500;

    private static final int CHUNK_SIZE = 4096;

    private static final URI baseURI = URI.create("http://localhost:8080/kilo-test/");

    @Test
    public void testBulkUploadJSON() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("bulk-upload/json"));

        webServiceProxy.setBody(new Rows());

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return "application/json";
            }

            @Override
            public void encodeRequest(Object body, OutputStream outputStream) throws IOException {
                var jsonEncoder = new JSONEncoder(true);

                var writer = new OutputStreamWriter(outputStream);

                jsonEncoder.write(body, writer);
            }
        });

        webServiceProxy.setChunkSize(CHUNK_SIZE);

        assertEquals(ROW_COUNT, webServiceProxy.invoke());
    }

    @Test
    public void testBulkUploadCSV() throws IOException {
        var webServiceProxy = new WebServiceProxy("POST", baseURI.resolve("bulk-upload/csv"));

        webServiceProxy.setBody(new Rows());

        webServiceProxy.setRequestHandler(new WebServiceProxy.RequestHandler() {
            @Override
            public String getContentType() {
                return "text/csv";
            }

            @Override
            public void encodeRequest(Object body, OutputStream outputStream) throws IOException {
                var csvEncoder = new CSVEncoder();

                var writer = new OutputStreamWriter(outputStream);

                var keys = listOf("text1", "text2", "number1", "number2", "number3");

                csvEncoder.write(keys, writer);
                csvEncoder.writeAll(mapAll(mapAll((Rows)body, BeanAdapter::new), map -> mapAll(keys, map::get)), writer);
            }
        });

        webServiceProxy.setChunkSize(CHUNK_SIZE);

        assertEquals(ROW_COUNT, webServiceProxy.invoke());
    }

    private static String getRandomString(int length) {
        var stringBuilder = new StringBuilder(length);

        for (var i = 0; i < length; i++) {
            stringBuilder.append((char)(97 + (int)(Math.random() * 26)));
        }

        return stringBuilder.toString();
    }

    private static double getRandomNumber() {
        return Math.random() * 999999;
    }
}
