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

import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.httprpc.WebServiceProxy;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;
import static org.httprpc.WebServiceProxy.valueAt;

public class WebServiceProxyTest {
    public static void main(String[] args) throws Exception {
        // Allow self-signed certificates for testing purposes
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // No-op
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // No-op
            }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");

        sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> {
            return true;
        });

        // Create service proxy
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy serviceProxy = new WebServiceProxy(new URL("https://localhost:8443"), threadPool, 3000, 3000);

        // Set credentials
        serviceProxy.setAuthorization(new PasswordAuthentication("tomcat", "tomcat".toCharArray()));

        // GET
        serviceProxy.invoke("GET", "/httprpc-server/test/get.json", mapOf(
            entry("string", "héllo"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true)),
            (Map<String, ?> result, Exception exception) -> {
            validate("GET", exception == null
                && valueAt(result, "string").equals("héllo")
                && valueAt(result, "strings").equals(listOf("a", "b", "c"))
                && valueAt(result, "number").equals(123)
                && valueAt(result, "flag").equals(true)
                && valueAt(result, "xyz") == null);
        });

        // POST
        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        serviceProxy.invoke("POST", "/httprpc-server/test/post.json", mapOf(
            entry("string", "héllo"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("attachments", listOf(textTestURL, imageTestURL))),
            (Map<String, ?> result, Exception exception) -> {
            validate("POST", exception == null && result.equals(mapOf(
                entry("string", "héllo"),
                entry("strings", listOf("a", "b", "c")),
                entry("number", 123),
                entry("flag", true),
                entry("attachmentInfo", listOf(
                    mapOf(
                        entry("bytes", 26),
                        entry("checksum", 2412)
                    ),
                    mapOf(
                        entry("bytes", 10392),
                        entry("checksum", 1038036)
                    )
                ))
            )));
        });

        // PUT
        serviceProxy.invoke("PUT", "/httprpc-server/test/put.json", mapOf(
            entry("text", "héllo")),
            (String result, Exception exception) -> {
            validate("PUT", exception == null && result.equals("göodbye"));
        });

        // DELETE
        serviceProxy.invoke("DELETE", "/httprpc-server/test/delete.json", mapOf(
            entry("id", 101)),
            (Boolean result, Exception exception) -> {
            validate("DELETE", exception == null && result.equals(true));
        });

        // Long list
        Future<?> future = serviceProxy.invoke("GET", "/httprpc-server/test/longList.json", (result, exception) -> {
            // No-op
        });

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                validate("Long list", future.cancel(true));
            }
        }, 1000);

        // Delayed result
        serviceProxy.invoke("GET", "/httprpc-server/test/delayedResult.json", mapOf(entry("result", "abcdefg"), entry("delay", 6000)), (result, exception) -> {
            validate("Delayed result", exception instanceof SocketTimeoutException);
        });

        // Parallel operations
        Future<Number> sum1 = serviceProxy.invoke("GET", "/httprpc-server/test/sum.json", mapOf(entry("a", 1), entry("b", 2)), null);
        Future<Number> sum2 = serviceProxy.invoke("GET", "/httprpc-server/test/sum.json", mapOf(entry("a", 2), entry("b", 4)), null);
        Future<Number> sum3 = serviceProxy.invoke("GET", "/httprpc-server/test/sum.json", mapOf(entry("a", 3), entry("b", 6)), null);

        validate("Parallel operations", sum1.get().equals(3) && sum2.get().equals(6) && sum3.get().equals(9));

        // Shut down thread pool
        threadPool.shutdown();
    }

    private static void validate(String test, boolean condition) {
        System.out.println(test + ": " + (condition ? "OK" : "FAIL"));
    }
}
