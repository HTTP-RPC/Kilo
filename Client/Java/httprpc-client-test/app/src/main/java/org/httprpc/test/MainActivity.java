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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.httprpc.WebServiceProxy;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;
import static org.httprpc.WebServiceProxy.valueAt;

public class MainActivity extends AppCompatActivity {
    private CheckBox getCheckBox;
    private CheckBox postCheckBox;
    private CheckBox putCheckBox;
    private CheckBox deleteCheckBox;
    private CheckBox longListCheckBox;
    private CheckBox delayedResultCheckBox;
    private CheckBox imageCheckBox;
    private ImageView imageView;

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

        getCheckBox = (CheckBox)findViewById(R.id.get_checkbox);
        postCheckBox = (CheckBox)findViewById(R.id.post_checkbox);
        putCheckBox = (CheckBox)findViewById(R.id.put_checkbox);
        deleteCheckBox = (CheckBox)findViewById(R.id.delete_checkbox);
        longListCheckBox = (CheckBox)findViewById(R.id.long_list_checkbox);
        delayedResultCheckBox = (CheckBox)findViewById(R.id.delayed_result_checkbox);
        imageCheckBox = (CheckBox)findViewById(R.id.image_checkbox);
        imageView = (ImageView)findViewById(R.id.image_view);
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

        // GET
        serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(
            entry("string", "héllo"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true)),
            (Map<String, ?> result, Exception exception) -> {
            getCheckBox.setChecked(exception == null
                && valueAt(result, "string").equals("héllo")
                && valueAt(result, "strings").equals(listOf("a", "b", "c"))
                && valueAt(result, "number").equals(123)
                && valueAt(result, "flag").equals(true)
                && valueAt(result, "xyz") == null);
        });

        // POST
        URL textTestURL = getClass().getResource("/assets/test.txt");
        URL imageTestURL = getClass().getResource("/assets/test.jpg");

        serviceProxy.invoke("POST", "/httprpc-server/test", mapOf(
            entry("string", "héllo"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("attachments", listOf(textTestURL, imageTestURL))),
            (Map<String, ?> result, Exception exception) -> {
            postCheckBox.setChecked(exception == null && result.equals(mapOf(
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
            putCheckBox.setChecked(exception == null && result.equals("göodbye"));
        });

        // Delete
        serviceProxy.invoke("DELETE", "/httprpc-server/test", mapOf(entry("id", 101)), (result, exception) -> {
            deleteCheckBox.setChecked(exception == null && result.equals(true));
        });

        // Long list
        // TODO Closing the input stream does not appear to abort the connection in Android
        /*
        Future<?> future = serviceProxy.invoke("GET", "/httprpc-server/test", (result, exception) -> {
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
        serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(entry("result", "abcdefg"), entry("delay", 9000)), (result, exception) -> {
            delayedResultCheckBox.setChecked(exception instanceof SocketTimeoutException);
        });

        // Image
        serviceProxy.invoke("GET", "/httprpc-server/test.jpg", (Bitmap result, Exception exception) -> {
            imageCheckBox.setChecked(exception == null && result != null);
            imageView.setImageBitmap(result);
        });

        // Shut down thread pool
        threadPool.shutdown();
    }
}
