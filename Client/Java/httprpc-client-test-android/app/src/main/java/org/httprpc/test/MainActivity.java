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

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
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

import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

public class MainActivity extends AppCompatActivity {
    private CheckBox sumCheckBox;
    private CheckBox sumAllCheckBox;
    private CheckBox inverseCheckBox;
    private CheckBox charactersCheckBox;
    private CheckBox selectionCheckBox;
    private CheckBox statisticsCheckBox;
    private CheckBox testDataCheckBox;
    private CheckBox voidCheckBox;
    private CheckBox nullCheckBox;
    private CheckBox localeCodeCheckBox;
    private CheckBox userNameCheckBox;
    private CheckBox userRoleStatusCheckBox;

    static {
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

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
                return true;
            }
        });

        // Set result dispatcher
        WebServiceProxy.setResultDispatcher(new Executor() {
            private Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        sumCheckBox = (CheckBox)findViewById(R.id.sum_checkbox);
        sumAllCheckBox = (CheckBox)findViewById(R.id.sum_all_checkbox);
        inverseCheckBox = (CheckBox)findViewById(R.id.inverse_checkbox);
        charactersCheckBox = (CheckBox)findViewById(R.id.characters_checkbox);
        selectionCheckBox = (CheckBox)findViewById(R.id.selection_checkbox);
        statisticsCheckBox = (CheckBox)findViewById(R.id.statistics_checkbox);
        testDataCheckBox = (CheckBox)findViewById(R.id.test_data_checkbox);
        voidCheckBox = (CheckBox)findViewById(R.id.void_checkbox);
        nullCheckBox = (CheckBox)findViewById(R.id.null_checkbox);
        localeCodeCheckBox = (CheckBox)findViewById(R.id.locale_code_checkbox);
        userNameCheckBox = (CheckBox)findViewById(R.id.user_name_checkbox);
        userRoleStatusCheckBox = (CheckBox)findViewById(R.id.user_role_status_checkbox);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Create service proxy
        URL serverURL;
        try {
            serverURL = new URL("https://10.0.2.2:8443");
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy serviceProxy = new WebServiceProxy(serverURL, threadPool);

        // Set credentials
        serviceProxy.setAuthentication(new BasicAuthentication("tomcat", "tomcat"));

        // Sum
        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);

        serviceProxy.invoke("GET", "/httprpc-server-test/test/sum", addArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                sumCheckBox.setChecked(exception == null && result.intValue() == 6);
            }
        });

        // Sum all
        serviceProxy.invoke("GET", "/httprpc-server-test/test/sumAll", mapOf(entry("values", Arrays.asList(1, 2, 3, 4))), new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                sumAllCheckBox.setChecked(exception == null && result.doubleValue() == 10.0);
            }
        });

        // Inverse
        serviceProxy.invoke("GET", "/httprpc-server-test/test/inverse", mapOf(entry("value", true)), new ResultHandler<Boolean>() {
            @Override
            public void execute(Boolean result, Exception exception) {
                inverseCheckBox.setChecked(exception == null && result == false);
            }
        });

        // Characters
        serviceProxy.invoke("GET", "/httprpc-server-test/test/characters", mapOf(entry("text", "Hello, World!")), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                charactersCheckBox.setChecked(exception == null && result.equals(Arrays.asList("H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
            }
        });

        // Selection
        serviceProxy.invoke("POST", "/httprpc-server-test/test/selection", mapOf(entry("items", Arrays.asList("a", "b", "c", "d"))), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                selectionCheckBox.setChecked(exception == null && result.equals("a, b, c, d"));
            }
        });

        // Statistics
        serviceProxy.invoke("POST", "/httprpc-server-test/test/statistics", mapOf(entry("values", Arrays.asList(1, 3, 5))), new ResultHandler<Map<String, Object>>() {
            @Override
            public void execute(Map<String, Object> result, Exception exception) {
                Statistics statistics = (exception == null) ? new Statistics(result) : null;

                statisticsCheckBox.setChecked(statistics != null && statistics.getCount() == 3 && statistics.getAverage() == 3.0 && statistics.getSum() == 9.0);
            }
        });

        // Test data
        serviceProxy.invoke("GET", "/httprpc-server-test/test/testData", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                testDataCheckBox.setChecked(exception == null && result.equals(Arrays.asList(mapOf(entry("a", "hello"), entry("b", 1L), entry("c", 2.0)), mapOf(entry("a", "goodbye"), entry("b", 2L), entry("c", 4.0)))));
            }
        });

        // Void
        serviceProxy.invoke("GET", "/httprpc-server-test/test/void", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                voidCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Null
        serviceProxy.invoke("GET", "/httprpc-server-test/test/null", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                nullCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Locale code
        serviceProxy.invoke("GET", "/httprpc-server-test/test/localeCode", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                localeCodeCheckBox.setChecked(exception == null && result != null);
                localeCodeCheckBox.setText(localeCodeCheckBox.getText() + ": " + String.valueOf(result));
            }
        });

        // User name
        serviceProxy.invoke("GET", "/httprpc-server-test/test/userName", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                userNameCheckBox.setChecked(exception == null && result.equals("tomcat"));
            }
        });

        // User role status
        serviceProxy.invoke("GET", "/httprpc-server-test/test/userRoleStatus", mapOf(entry("role", "tomcat")), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                userRoleStatusCheckBox.setChecked(exception == null && result.equals(true));
            }
        });

        // Shut down thread pool
        threadPool.shutdown();
    }
}
