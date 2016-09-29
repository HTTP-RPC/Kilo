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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.httprpc.ResultHandler;
import org.httprpc.WebServiceProxy;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;

public class MainActivity extends AppCompatActivity {
    private CheckBox sumCheckBox;
    private CheckBox sumAllCheckBox;
    private CheckBox inverseCheckBox;
    private CheckBox charactersCheckBox;
    private CheckBox selectionCheckBox;
    private CheckBox putCheckBox;
    private CheckBox deleteCheckBox;
    private CheckBox statisticsCheckBox;
    private CheckBox testDataCheckBox;
    private CheckBox voidCheckBox;
    private CheckBox nullCheckBox;
    private CheckBox localeCodeCheckBox;
    private CheckBox userNameCheckBox;
    private CheckBox userRoleStatusCheckBox;
    private CheckBox attachmentInfoCheckBox;
    private CheckBox dateCheckBox;
    private CheckBox datesCheckBox;
    private CheckBox echoCheckBox;
    private ImageView echoImageView;
    private CheckBox longListCheckBox;
    private CheckBox delayedResultCheckBox;

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
        putCheckBox = (CheckBox)findViewById(R.id.put_checkbox);
        deleteCheckBox = (CheckBox)findViewById(R.id.delete_checkbox);
        statisticsCheckBox = (CheckBox)findViewById(R.id.statistics_checkbox);
        testDataCheckBox = (CheckBox)findViewById(R.id.test_data_checkbox);
        voidCheckBox = (CheckBox)findViewById(R.id.void_checkbox);
        nullCheckBox = (CheckBox)findViewById(R.id.null_checkbox);
        localeCodeCheckBox = (CheckBox)findViewById(R.id.locale_code_checkbox);
        userNameCheckBox = (CheckBox)findViewById(R.id.user_name_checkbox);
        userRoleStatusCheckBox = (CheckBox)findViewById(R.id.user_role_status_checkbox);
        attachmentInfoCheckBox = (CheckBox)findViewById(R.id.attachment_info_checkbox);
        dateCheckBox = (CheckBox)findViewById(R.id.date_checkbox);
        datesCheckBox = (CheckBox)findViewById(R.id.dates_checkbox);
        echoImageView = (ImageView)findViewById(R.id.echo_image_view);
        echoCheckBox = (CheckBox)findViewById(R.id.echo_checkbox);
        longListCheckBox = (CheckBox)findViewById(R.id.long_list_checkbox);
        delayedResultCheckBox = (CheckBox)findViewById(R.id.delayed_result_checkbox);
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

        WebServiceProxy serviceProxy = new WebServiceProxy(serverURL, threadPool, 3000, 3000) {
            private Handler handler = new Handler(Looper.getMainLooper());

            @Override
            protected Object decodeResponse(InputStream inputStream, String contentType) throws IOException {
                Object value;
                if (contentType != null && contentType.startsWith("image/")) {
                    value = BitmapFactory.decodeStream(inputStream);
                } else {
                    value = super.decodeResponse(inputStream, contentType);
                }

                return value;
            }

            @Override
            protected void dispatchResult(Runnable command) {
                handler.post(command);
            }
        };

        // Set credentials
        serviceProxy.setAuthorization(new PasswordAuthentication("tomcat", "tomcat".toCharArray()));

        // Sum
        HashMap<String, Object> sumArguments = new HashMap<>();
        sumArguments.put("a", 2);
        sumArguments.put("b", 4);

        serviceProxy.invoke("GET", "/httprpc-server-test/test/sum", sumArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                sumCheckBox.setChecked(exception == null && result.intValue() == 6);
            }
        });

        serviceProxy.invoke("GET", "/httprpc-server-test/test/sum", mapOf(entry("values", listOf(1, 2, 3, 4))), (Number result, Exception exception) -> {
            sumAllCheckBox.setChecked(exception == null && result.doubleValue() == 10.0);
        });

        // Inverse
        serviceProxy.invoke("GET", "/httprpc-server-test/test/inverse", mapOf(entry("value", true)), (Boolean result, Exception exception) -> {
            inverseCheckBox.setChecked(exception == null && result == false);
        });

        // Characters
        serviceProxy.invoke("GET", "/httprpc-server-test/test/characters", mapOf(entry("text", "Héllo, World!")), (result, exception) -> {
            charactersCheckBox.setChecked(exception == null && result.equals(Arrays.asList("H", "é", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
        });

        // Selection
        serviceProxy.invoke("POST", "/httprpc-server-test/test/selection", mapOf(entry("items", listOf("å", "b", "c", "d"))), (result, exception) -> {
            selectionCheckBox.setChecked(exception == null && result.equals("å, b, c, d"));
        });

        // Put
        serviceProxy.invoke("PUT", "/httprpc-server-test/test", mapOf(entry("value", "héllo")), (result, exception) -> {
            putCheckBox.setChecked(exception == null && result.equals("héllo"));
        });

        // Delete
        serviceProxy.invoke("DELETE", "/httprpc-server-test/test", mapOf(entry("value", 101)), (result, exception) -> {
            deleteCheckBox.setChecked(exception == null && result.equals(101L));
        });

        // Statistics
        serviceProxy.invoke("POST", "/httprpc-server-test/test/statistics", mapOf(entry("values", listOf(1, 3, 5))), (Map<String, Object> result, Exception exception) -> {
            Statistics statistics = (exception == null) ? new Statistics(result) : null;

            statisticsCheckBox.setChecked(statistics != null && statistics.getCount() == 3 && statistics.getAverage() == 3.0 && statistics.getSum() == 9.0);
        });

        // Test data
        serviceProxy.invoke("GET", "/httprpc-server-test/test/testData", (result, exception) -> {
            testDataCheckBox.setChecked(exception == null && result.equals(Arrays.asList(mapOf(entry("a", "hello"), entry("b", 1L), entry("c", 2.0)), mapOf(entry("a", "goodbye"), entry("b", 2L), entry("c", 4.0)))));
        });

        // Void
        serviceProxy.invoke("GET", "/httprpc-server-test/test/void", (result, exception) -> {
            voidCheckBox.setChecked(exception == null && result == null);
        });

        // Null
        serviceProxy.invoke("GET", "/httprpc-server-test/test/null", (result, exception) -> {
            nullCheckBox.setChecked(exception == null && result == null);
        });

        // Locale code
        serviceProxy.invoke("GET", "/httprpc-server-test/test/localeCode", (result, exception) -> {
            localeCodeCheckBox.setChecked(exception == null && result != null);
            localeCodeCheckBox.setText(getResources().getString(R.string.locale_code) + ": " + String.valueOf(result));
        });

        // User name
        serviceProxy.invoke("GET", "/httprpc-server-test/test/user/name", (result, exception) -> {
            userNameCheckBox.setChecked(exception == null && result.equals("tomcat"));
        });

        // User role status
        serviceProxy.invoke("GET", "/httprpc-server-test/test/user/roleStatus", mapOf(entry("role", "tomcat")), (result, exception) -> {
            userRoleStatusCheckBox.setChecked(exception == null && result.equals(true));
        });

        // Attachment info
        URL textTestURL = getClass().getResource("/assets/test.txt");
        URL imageTestURL = getClass().getResource("/assets/test.jpg");

        List<?> attachments = listOf(textTestURL, imageTestURL);

        serviceProxy.invoke("POST", "/httprpc-server-test/test/attachmentInfo",
            mapOf(entry("text", "héllo"), entry("attachments", attachments)), (Map<String, ?> result, Exception exception) -> {
            attachmentInfoCheckBox.setChecked(exception == null && result.equals(mapOf(
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
        });

        // Dates
        serviceProxy.invoke("GET", "/httprpc-server-test/test/echo", mapOf(entry("date", 0)), (Number result, Exception exception) -> {
            dateCheckBox.setChecked(exception == null && result.equals(0));
        });

        final List <?> dates = listOf("2016-09-15", "2016-09-16");

        serviceProxy.invoke("GET", "/httprpc-server-test/test/echo", mapOf(entry("dates", dates)), (List<Number> result, Exception exception) -> {
            datesCheckBox.setChecked(exception == null && result.equals(dates));
        });

        // Echo
        serviceProxy.invoke("POST", "/httprpc-server-test/test/echo", mapOf(entry("attachment", imageTestURL)), (Bitmap result, Exception exception) -> {
            echoCheckBox.setChecked(exception == null && result != null);
            echoImageView.setImageBitmap(result);
        });

        // Long list
        // TODO Closing the input stream does not appear to abort the connection in Android
        /*
        Future<?> future = serviceProxy.invoke("GET", "/httprpc-server-test/test/longList", (result, exception) -> {
            // No-op
        });

        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                longListCheckBox.setChecked(future.cancel(true));
            }
        }, 1000);
        */

        // Delayed result
        serviceProxy.invoke("GET", "/httprpc-server-test/test/delayedResult", mapOf(entry("result", "abcdefg"), entry("delay", 9000)), (result, exception) -> {
            delayedResultCheckBox.setChecked(exception instanceof SocketTimeoutException);
        });

        // Shut down thread pool
        threadPool.shutdown();
    }
}
