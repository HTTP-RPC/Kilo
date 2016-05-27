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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

public class MainActivity extends AppCompatActivity {
    private CheckBox addCheckBox;
    private CheckBox addValuesCheckBox;
    private CheckBox invertValueCheckBox;
    private CheckBox getCharactersCheckBox;
    private CheckBox getSelectionCheckBox;
    private CheckBox getMapCheckBox;
    private CheckBox getStatisticsCheckBox;
    private CheckBox getTestDataCheckBox;
    private CheckBox getVoidCheckBox;
    private CheckBox getNullCheckBox;
    private CheckBox getLocaleCodeCheckBox;
    private CheckBox getUserNameCheckBox;
    private CheckBox isUserInRoleCheckBox;
    private CheckBox getAttachmentInfoCheckBox;

    private static final String TAG = MainActivity.class.getSimpleName();

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

        addCheckBox = (CheckBox)findViewById(R.id.add_checkbox);
        addValuesCheckBox = (CheckBox)findViewById(R.id.add_values_checkbox);
        invertValueCheckBox = (CheckBox)findViewById(R.id.invert_value_checkbox);
        getCharactersCheckBox = (CheckBox)findViewById(R.id.get_characters_checkbox);
        getSelectionCheckBox = (CheckBox)findViewById(R.id.get_selection_checkbox);
        getMapCheckBox = (CheckBox)findViewById(R.id.get_map_checkbox);
        getStatisticsCheckBox = (CheckBox)findViewById(R.id.get_statistics_checkbox);
        getTestDataCheckBox = (CheckBox)findViewById(R.id.get_test_data_checkbox);
        getVoidCheckBox = (CheckBox)findViewById(R.id.get_void_checkbox);
        getNullCheckBox = (CheckBox)findViewById(R.id.get_null_checkbox);
        getLocaleCodeCheckBox = (CheckBox)findViewById(R.id.get_locale_code_checkbox);
        getUserNameCheckBox = (CheckBox)findViewById(R.id.get_user_name_checkbox);
        isUserInRoleCheckBox = (CheckBox)findViewById(R.id.is_user_in_role_checkbox);
        getAttachmentInfoCheckBox = (CheckBox)findViewById(R.id.get_attachment_info_checkbox);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Create service
        URL baseURL;
        try {
            baseURL = new URL("https://10.0.2.2:8443/httprpc-server-test/test/");
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy serviceProxy = new WebServiceProxy(baseURL, threadPool);

        // Set credentials
        serviceProxy.setAuthentication(new BasicAuthentication("tomcat", "tomcat"));

        // Add
        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);

        serviceProxy.invoke("add", addArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                addCheckBox.setChecked(exception == null && result.intValue() == 6);
            }
        });

        // Add values
        serviceProxy.invoke("addValues", mapOf(entry("values", Arrays.asList(1, 2, 3, 4))), new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                addValuesCheckBox.setChecked(exception == null && result.doubleValue() == 10.0);
            }
        });

        // Invert value
        serviceProxy.invoke("invertValue", mapOf(entry("value", true)), new ResultHandler<Boolean>() {
            @Override
            public void execute(Boolean result, Exception exception) {
                invertValueCheckBox.setChecked(exception == null && result == false);
            }
        });

        // Get characters
        serviceProxy.invoke("getCharacters", mapOf(entry("text", "Hello, World!")), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getCharactersCheckBox.setChecked(exception == null && result.equals(Arrays.asList("H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
            }
        });

        // Get selection
        serviceProxy.invoke("getSelection", mapOf(entry("items", Arrays.asList("a", "b", "c", "d"))), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getSelectionCheckBox.setChecked(exception == null && result.equals("a, b, c, d"));
            }
        });

        // Get map
        final Map<String, ?> map = mapOf(entry("a", 123L), entry("b", 456L), entry("c", 789L));

        serviceProxy.invoke("getMap", mapOf(entry("map", map)), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getMapCheckBox.setChecked(exception == null && result.equals(map));
            }
        });

        // Get statistics
        serviceProxy.invoke("getStatistics", mapOf(entry("values", Arrays.asList(1, 3, 5))), new ResultHandler<Map<String, Object>>() {
            @Override
            public void execute(Map<String, Object> result, Exception exception) {
                Statistics statistics = (exception == null) ? new Statistics(result) : null;

                getStatisticsCheckBox.setChecked(statistics != null && statistics.getCount() == 3 && statistics.getAverage() == 3.0 && statistics.getSum() == 9.0);
            }
        });

        // Get test data
        serviceProxy.invoke("getTestData", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getTestDataCheckBox.setChecked(exception == null && result.equals(Arrays.asList(mapOf(entry("a", "hello"), entry("b", 1L), entry("c", 2.0)), mapOf(entry("a", "goodbye"), entry("b", 2L), entry("c", 4.0)))));
            }
        });

        // Get void
        serviceProxy.invoke("getVoid", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getVoidCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Get null
        serviceProxy.invoke("getNull", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getNullCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Get locale code
        serviceProxy.invoke("getLocaleCode", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getLocaleCodeCheckBox.setChecked(exception == null && result != null);
                getLocaleCodeCheckBox.setText(getLocaleCodeCheckBox.getText() + ": " + String.valueOf(result));
            }
        });

        // Get user name
        serviceProxy.invoke("getUserName", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getUserNameCheckBox.setChecked(exception == null && result.equals("tomcat"));
            }
        });

        // Is user in role
        serviceProxy.invoke("isUserInRole", mapOf(entry("role", "tomcat")), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                isUserInRoleCheckBox.setChecked(exception == null && result.equals(true));
            }
        });

        // Get attachment info
        Map<String, ?> arguments = Collections.emptyMap();

        URL textTestURL = getClass().getResource("/assets/test.txt");
        URL imageTestURL = getClass().getResource("/assets/test.jpg");

        Map<String, ?> attachments = mapOf(entry("test", listOf(textTestURL, imageTestURL)));

        serviceProxy.invoke("getAttachmentInfo", arguments, (Map<String, List<URL>>)attachments, new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                List<Map<String, Object>> attachmentInfo = (List<Map<String, Object>>)result;

                Map<String, Object> textInfo = attachmentInfo.get(0);
                Map<String, Object> imageInfo = attachmentInfo.get(1);

                getAttachmentInfoCheckBox.setChecked(exception == null
                    && textInfo.get("contentType").equals("text/plain")
                    && textInfo.get("size").equals(26L) && textInfo.get("checksum").equals(2412L)
                    && imageInfo.get("contentType").equals("image/jpeg")
                    && imageInfo.get("size").equals(10392L) && imageInfo.get("checksum").equals(1038036L));
            }
        });

        // Shut down thread pool
        threadPool.shutdown();
    }
}
