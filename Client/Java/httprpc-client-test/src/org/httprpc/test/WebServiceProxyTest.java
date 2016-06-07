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

import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    @SuppressWarnings("unchecked")
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

        // Create service
        URL baseURL = new URL("https://localhost:8443/httprpc-server-test/test/");
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy serviceProxy = new WebServiceProxy(baseURL, threadPool);

        // Set credentials
        serviceProxy.setAuthentication(new BasicAuthentication("tomcat", "tomcat"));

        // Add
        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);

        serviceProxy.invoke("GET", "add", addArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                validate(exception == null && result.doubleValue() == 6.0);
            }
        });

        // Add values
        serviceProxy.invoke("GET", "addValues", mapOf(entry("values", listOf(1, 2, 3, 4))), (Number result, Exception exception) -> {
            validate(exception == null && result.doubleValue() == 10.0);
        });

        // Invert value
        serviceProxy.invoke("GET", "invertValue", mapOf(entry("value", true)), (Boolean result, Exception exception) -> {
            validate(exception == null && result == false);
        });

        // Get characters
        serviceProxy.invoke("GET", "getCharacters", mapOf(entry("text", "Hello, World!")), (result, exception) -> {
            validate(exception == null && result.equals(listOf("H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
        });

        // Get selection
        serviceProxy.invoke("POST", "getSelection", mapOf(entry("items", listOf("a", "b", "c", "d"))), (result, exception) -> {
            validate(exception == null && result.equals("a, b, c, d"));
        });

        // Get map
        Map<String, ?> map = mapOf(entry("a", 123L), entry("b", 456L), entry("c", 789L));

        serviceProxy.invoke("GET", "getMap", mapOf(entry("map", map)), (result, exception) -> {
            validate(exception == null && result.equals(map));
        });

        // Get statistics
        serviceProxy.invoke("POST", "getStatistics", mapOf(entry("values", listOf(1, 3, 5))), (Map<String, Object> result, Exception exception) -> {
            Statistics statistics = (exception == null) ? new Statistics(result) : null;

            validate(statistics != null
                && statistics.getCount() == 3
                && statistics.getAverage() == 3.0
                && statistics.getSum() == 9.0);
        });

        // Get test data
        serviceProxy.invoke("GET", "getTestData", (result, exception) -> {
            validate(exception == null && result.equals(listOf(
                mapOf(entry("a", "hello"), entry("b", 1L), entry("c", 2.0)),
                mapOf(entry("a", "goodbye"), entry("b", 2L), entry("c", 4.0))))
            );
        });

        // Get void
        serviceProxy.invoke("GET", "getVoid", (result, exception) -> {
            validate(exception == null && result == null);
        });

        // Get null
        serviceProxy.invoke("GET", "getNull", (result, exception) -> {
            validate(exception == null && result == null);
        });

        // Get locale code
        serviceProxy.invoke("GET", "getLocaleCode", (result, exception) -> {
            validate(exception == null && result != null);
            System.out.println(result);
        });

        // Get user name
        serviceProxy.invoke("GET", "getUserName", (result, exception) -> {
            validate(exception == null && result.equals("tomcat"));
        });

        // Is user in role
        serviceProxy.invoke("GET", "isUserInRole", mapOf(entry("role", "tomcat")), (result, exception) -> {
            validate(exception == null && result.equals(true));
        });

        // Get attachment info
        Map<String, ?> arguments = Collections.emptyMap();

        URL textTestURL = WebServiceProxyTest.class.getResource("test.txt");
        URL imageTestURL = WebServiceProxyTest.class.getResource("test.jpg");

        Map<String, ?> attachments = mapOf(entry("test", listOf(textTestURL, imageTestURL)));

        serviceProxy.invoke("POST", "getAttachmentInfo", arguments, (Map<String, List<URL>>)attachments, new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                List<Map<String, Object>> attachmentInfo = (List<Map<String, Object>>)result;

                Map<String, Object> textInfo = attachmentInfo.get(0);
                Map<String, Object> imageInfo = attachmentInfo.get(1);

                validate(exception == null
                    && textInfo.get("contentType").equals("text/plain")
                    && textInfo.get("size").equals(26L) && textInfo.get("checksum").equals(2412L)
                    && imageInfo.get("contentType").equals("image/jpeg")
                    && imageInfo.get("size").equals(10392L) && imageInfo.get("checksum").equals(1038036L));
            }
        });

        // Shut down thread pool
        threadPool.shutdown();
    }

    private static void validate(boolean condition) {
        System.out.println(condition ? "OK" : "FAIL");
    }
}
