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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.httprpc.ResultHandler;
import org.httprpc.WebServiceProxy;

import static org.httprpc.Arguments.*;

public class WebServiceProxyTest {
    public static void main(String[] args) throws Exception {
        // Set global credentials
        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication () {
                return new PasswordAuthentication("tomcat", "tomcat".toCharArray());
            }
        });

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
        URL baseURL = new URL("https://localhost:8443/httprpc-test-server/test/");
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy service = new WebServiceProxy(baseURL, threadPool);

        // Add
        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);

        service.invoke("add", addArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                validate(exception == null && result.doubleValue() == 6.0);
            }
        });

        // Add values
        service.invoke("addValues", mapOf(entry("values", Arrays.asList(1, 2, 3, 4))), (Number result, Exception exception) -> {
            validate(exception == null && result.doubleValue() == 10.0);
        });

        // Invert value
        service.invoke("invertValue", mapOf(entry("value", true)), (Boolean result, Exception exception) -> {
            validate(exception == null && result == false);
        });

        // Get characters
        service.invoke("getCharacters", mapOf(entry("text", "Hello, World!")), (result, exception) -> {
            validate(exception == null && result.equals(Arrays.asList("H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
        });

        // Get selection
        service.invoke("getSelection", mapOf(entry("items", Arrays.asList("a", "b", "c", "d"))), (result, exception) -> {
            validate(exception == null && result.equals("a, b, c, d"));
        });

        // Get statistics
        service.invoke("getStatistics", mapOf(entry("values", Arrays.asList(1, 3, 5))), (Map<String, Number> result, Exception exception) -> {
            validate(exception == null
                && result.get("count").intValue() == 3
                && result.get("average").doubleValue() == 3.0
                && result.get("sum").doubleValue() == 9.0);
        });

        // Get test data
        service.invoke("getTestData", (result, exception) -> {
            HashMap<String, Object> row1 = new HashMap<>();
            row1.put("a", "hello");
            row1.put("b", 1L);
            row1.put("c", 2.0);

            HashMap<String, Object> row2 = new HashMap<>();
            row2.put("a", "goodbye");
            row2.put("b", 2L);
            row2.put("c", 4.0);

            validate(exception == null && result.equals(Arrays.asList(row1, row2)));
        });

        // Get void
        service.invoke("getVoid", (result, exception) -> {
            validate(exception == null && result == null);
        });

        // Get null
        service.invoke("getNull", (result, exception) -> {
            validate(exception == null && result == null);
        });

        // Get locale code
        service.invoke("getLocaleCode", (result, exception) -> {
            validate(exception == null && result != null);
            System.out.println(result);
        });

        // Get user name
        service.invoke("getUserName", (result, exception) -> {
            validate(exception == null && result.equals("tomcat"));
        });

        // Shut down thread pool
        threadPool.shutdown();
    }

    private static void validate(boolean condition) {
        System.out.println(condition ? "OK" : "FAIL");
    }
}
