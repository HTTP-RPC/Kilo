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

package org.httprpc.test;

import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.httprpc.BasicAuthentication;
import org.httprpc.ResultHandler;
import org.httprpc.WebServiceProxy;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

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
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
                return true;
            }
        });

        // Create service proxy
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy serviceProxy = new WebServiceProxy(new URL("https://localhost:8443"), threadPool, 3000, 3000);

        // Set credentials
        serviceProxy.setAuthentication(new BasicAuthentication("tomcat", "tomcat"));

        // Sum
        HashMap<String, Object> sumArguments = new HashMap<>();
        sumArguments.put("a", 2);
        sumArguments.put("b", 4);

        serviceProxy.invoke("GET", "/httprpc-server-test/test/sum", sumArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                validate(exception == null && result.doubleValue() == 6.0);
            }
        });

        // Sum all
        serviceProxy.invoke("GET", "/httprpc-server-test/test/sumAll", mapOf(entry("values", listOf(1, 2, 3, 4))), (Number result, Exception exception) -> {
            validate(exception == null && result.doubleValue() == 10.0);
        });

        // Inverse
        serviceProxy.invoke("GET", "/httprpc-server-test/test/inverse", mapOf(entry("value", true)), (Boolean result, Exception exception) -> {
            validate(exception == null && result == false);
        });

        // Characters
        serviceProxy.invoke("GET", "/httprpc-server-test/test/characters", mapOf(entry("text", "Héllo, World!")), (result, exception) -> {
            validate(exception == null && result.equals(listOf("H", "é", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
        });

        // Selection
        serviceProxy.invoke("POST", "/httprpc-server-test/test/selection", mapOf(entry("items", listOf("å", "b", "c", "d"))), (result, exception) -> {
            validate(exception == null && result.equals("å, b, c, d"));
        });

        // Put
        serviceProxy.invoke("PUT", "/httprpc-server-test/test", mapOf(entry("value", "héllo")), (result, exception) -> {
            validate(exception == null && result.equals("héllo"));
        });

        // Delete
        serviceProxy.invoke("DELETE", "/httprpc-server-test/test", mapOf(entry("value", 101)), (result, exception) -> {
            validate(exception == null && result.equals(101L));
        });

        // Statistics
        serviceProxy.invoke("POST", "/httprpc-server-test/test/statistics", mapOf(entry("values", listOf(1, 3, 5))), (Map<String, Object> result, Exception exception) -> {
            Statistics statistics = (exception == null) ? new Statistics(result) : null;

            validate(statistics != null
                && statistics.getCount() == 3
                && statistics.getAverage() == 3.0
                && statistics.getSum() == 9.0);
        });

        // Test data
        serviceProxy.invoke("GET", "/httprpc-server-test/test/testData", (result, exception) -> {
            validate(exception == null && result.equals(listOf(
                mapOf(entry("a", "hello"), entry("b", 1L), entry("c", 2.0)),
                mapOf(entry("a", "goodbye"), entry("b", 2L), entry("c", 4.0))))
            );
        });

        // Void
        serviceProxy.invoke("GET", "/httprpc-server-test/test/void", (result, exception) -> {
            validate(exception == null && result == null);
        });

        // Null
        serviceProxy.invoke("GET", "/httprpc-server-test/test/null", (result, exception) -> {
            validate(exception == null && result == null);
        });

        // Locale code
        serviceProxy.invoke("GET", "/httprpc-server-test/test/localeCode", (result, exception) -> {
            validate(exception == null && result != null);
            System.out.println(result);
        });

        // User name
        serviceProxy.invoke("GET", "/httprpc-server-test/test/userName", (result, exception) -> {
            validate(exception == null && result.equals("tomcat"));
        });

        // User role status
        serviceProxy.invoke("GET", "/httprpc-server-test/test/userRoleStatus", mapOf(entry("role", "tomcat")), (result, exception) -> {
            validate(exception == null && result.equals(true));
        });

        // Attachment info
        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        List<?> attachments = listOf(textTestURL, imageTestURL);

        serviceProxy.invoke("POST", "/httprpc-server-test/test/attachmentInfo",
            mapOf(entry("text", "héllo"), entry("attachments", attachments)),
            WebServiceProxyTest::handleAttachmentInfoResult);

        // Long list
        Future<?> future = serviceProxy.invoke("GET", "/httprpc-server-test/test/longList", (result, exception) -> {
            // No-op
        });

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                validate(future.cancel(true));
            }
        }, 1000);

        // Delayed result
        serviceProxy.invoke("GET", "/httprpc-server-test/test/delayedResult", mapOf(entry("result", "abcdefg"), entry("delay", 9000)), (result, exception) -> {
            validate(exception instanceof SocketTimeoutException);
        });

        // Shut down thread pool
        threadPool.shutdown();
    }

    private static void handleAttachmentInfoResult(Map<String, ?> result, Exception exception) {
        validate(exception == null && result.equals(mapOf(
            entry("text", "héllo"),
            entry("attachmentInfo", listOf(
                mapOf(
                    entry("bytes", 26L),
                    entry("checksum", 2412L)
                ),
                mapOf(
                    entry("bytes", 10392L),
                    entry("checksum", 1038036L)
                )
            ))
        )));
    }

    private static void validate(boolean condition) {
        System.out.println(condition ? "OK" : "FAIL");
    }
}
