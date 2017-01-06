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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;
import static org.httprpc.WebServiceProxy.valueAt;

public class WebServiceProxyTest {
    static {
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

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException(exception);
        }

        try {
            sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
        } catch (KeyManagementException exception) {
            throw new RuntimeException(exception);
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> {
            return true;
        });
    }

    public static void main(String[] args) throws Exception {
        // Create service proxy
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy serviceProxy = new WebServiceProxy(new URL("https://localhost:8443"), threadPool);

        serviceProxy.setConnectTimeout(3000);
        serviceProxy.setReadTimeout(3000);

        // Set credentials
        serviceProxy.setAuthorization(new PasswordAuthentication("tomcat", "tomcat".toCharArray()));

        // GET
        serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(
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

        serviceProxy.invoke("POST", "/httprpc-server/test", mapOf(
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
        serviceProxy.invoke("PUT", "/httprpc-server/test", mapOf(entry("text", "héllo")), (result, exception) -> {
            validate("PUT", exception == null && result.equals("göodbye"));
        });

        // DELETE
        serviceProxy.invoke("DELETE", "/httprpc-server/test", mapOf(entry("id", 101)), (result, exception) -> {
            validate("DELETE", exception == null && result.equals(true));
        });

        // Error
        serviceProxy.invoke("GET", "/httprpc-server/xyz", (result, exception) -> {
            validate("Error", exception instanceof WebServiceException
                && ((WebServiceException)exception).getCode() == 404);
        });

        // Timeout
        serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(
            entry("value", 123),
            entry("delay", 6000)), (result, exception) -> {
            validate("Timeout", exception instanceof SocketTimeoutException);
        });

        // Cancel
        Future<?> future = serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(
            entry("value", 123),
            entry("delay", 6000)), (result, exception) -> {
            // No-op
        });

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                validate("Cancel", future.cancel(true));

                timer.cancel();
            }
        }, 1000);

        // Blocking
        Future<Number> value1 = serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(entry("value", 1)), null);
        Future<Number> value2 = serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(entry("value", 2)), null);
        Future<Number> value3 = serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(entry("value", 3)), null);

        validate("Blocking", value1.get().intValue() + value2.get().intValue() + value3.get().intValue() == 6);

        // Error
        serviceProxy.invoke("GET", "/httprpc-server/xyz", (result, exception) -> {
            validate("Error", exception instanceof WebServiceException);
        });

        // Shut down thread pool
        threadPool.shutdown();
    }

    private static void validate(String test, boolean condition) {
        System.out.println(test + ": " + (condition ? "OK" : "FAIL"));
    }
}
