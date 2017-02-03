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
import android.widget.TextView;

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
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.httprpc.WebServiceException;
import org.httprpc.WebServiceProxy;

import static org.httprpc.WebServiceProxy.listOf;
import static org.httprpc.WebServiceProxy.mapOf;
import static org.httprpc.WebServiceProxy.entry;
import static org.httprpc.WebServiceProxy.valueAt;

public class MainActivity extends AppCompatActivity {
    private CheckBox getCheckBox;
    private CheckBox postMultipartCheckBox;
    private CheckBox postURLEncodedCheckBox;
    private CheckBox postJSONCheckBox;
    private CheckBox putCheckBox;
    private CheckBox putJSONCheckBox;
    private CheckBox patchCheckBox;
    private CheckBox patchJSONCheckBox;
    private CheckBox deleteCheckBox;
    private CheckBox errorCheckBox;
    private CheckBox timeoutCheckBox;
    private CheckBox cancelCheckBox;
    private CheckBox imageCheckBox;
    private ImageView imageView;
    private CheckBox textCheckBox;
    private TextView textView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getCheckBox = (CheckBox)findViewById(R.id.get_checkbox);
        postMultipartCheckBox = (CheckBox)findViewById(R.id.post_multipart_checkbox);
        postURLEncodedCheckBox = (CheckBox)findViewById(R.id.post_url_encoded_checkbox);
        postJSONCheckBox = (CheckBox)findViewById(R.id.post_json_checkbox);
        putCheckBox = (CheckBox)findViewById(R.id.put_checkbox);
        putJSONCheckBox = (CheckBox)findViewById(R.id.put_json_checkbox);
        patchCheckBox = (CheckBox)findViewById(R.id.patch_checkbox);
        patchJSONCheckBox = (CheckBox)findViewById(R.id.patch_json_checkbox);
        deleteCheckBox = (CheckBox)findViewById(R.id.delete_checkbox);
        errorCheckBox = (CheckBox)findViewById(R.id.error_checkbox);
        timeoutCheckBox = (CheckBox)findViewById(R.id.timeout_checkbox);
        cancelCheckBox = (CheckBox)findViewById(R.id.cancel_checkbox);
        imageCheckBox = (CheckBox)findViewById(R.id.image_checkbox);
        imageView = (ImageView)findViewById(R.id.image_view);
        textCheckBox = (CheckBox)findViewById(R.id.text_checkbox);
        textView = (TextView)findViewById(R.id.text_view);
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

        WebServiceProxy serviceProxy = new WebServiceProxy(serverURL, threadPool) {
            private Handler handler = new Handler(Looper.getMainLooper());

            @Override
            protected Object decodeImageResponse(InputStream inputStream, String imageType) {
                return BitmapFactory.decodeStream(inputStream);
            }

            @Override
            protected void dispatchResult(Runnable command) {
                handler.post(command);
            }
        };

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

        serviceProxy.setEncoding(WebServiceProxy.MULTIPART_FORM_DATA);
        serviceProxy.invoke("POST", "/httprpc-server/test", mapOf(
            entry("string", "héllo"),
            entry("strings", listOf("a", "b", "c")),
            entry("number", 123),
            entry("flag", true),
            entry("attachments", listOf(textTestURL, imageTestURL))),
            (Map<String, ?> result, Exception exception) -> {
            postMultipartCheckBox.setChecked(exception == null && result.equals(mapOf(
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

        serviceProxy.setEncoding(WebServiceProxy.APPLICATION_X_WWW_FORM_URLENCODED);
        serviceProxy.invoke("POST", "/httprpc-server/test", mapOf(
        entry("string", "héllo"),
        entry("strings", listOf("a", "b", "c")),
        entry("number", 123),
        entry("flag", true)),
        (Map<String, ?> result, Exception exception) -> {
            postURLEncodedCheckBox.setChecked(exception == null && result.equals(mapOf(
                entry("string", "héllo"),
                entry("strings", listOf("a", "b", "c")),
                entry("number", 123),
                entry("flag", true),
                entry("attachmentInfo", listOf())
            )));
        });

        serviceProxy.setEncoding(WebServiceProxy.APPLICATION_JSON);
        serviceProxy.invoke("POST", "/httprpc-server/test", mapOf(
        entry("string", "héllo"),
        entry("strings", listOf("a", "b", "c")),
        entry("number", 123),
        entry("flag", true)),
        (Map<String, ?> result, Exception exception) -> {
            postJSONCheckBox.setChecked(exception == null && result.equals(mapOf(
                entry("string", "héllo"),
                entry("strings", listOf("a", "b", "c")),
                entry("number", 123),
                entry("flag", true)
            )));
        });

        // PUT
        serviceProxy.setEncoding(WebServiceProxy.MULTIPART_FORM_DATA);
        serviceProxy.invoke("PUT", "/httprpc-server/test", mapOf(entry("text", "héllo")), (result, exception) -> {
            putCheckBox.setChecked(exception == null && result.equals("héllo"));
        });

        serviceProxy.setEncoding(WebServiceProxy.APPLICATION_JSON);
        serviceProxy.invoke("PUT", "/httprpc-server/test", mapOf(entry("text", "héllo")), (result, exception) -> {
            putJSONCheckBox.setChecked(exception == null && result.equals(mapOf(entry("text", "héllo"))));
        });

        // PATCH
        serviceProxy.setEncoding(WebServiceProxy.MULTIPART_FORM_DATA);
        serviceProxy.invoke("PATCH", "/httprpc-server/test", mapOf(entry("text", "héllo")), (result, exception) -> {
            patchCheckBox.setChecked(exception == null && result.equals("héllo"));
        });

        serviceProxy.setEncoding(WebServiceProxy.APPLICATION_JSON);
        serviceProxy.invoke("PATCH", "/httprpc-server/test", mapOf(entry("text", "héllo")), (result, exception) -> {
            patchJSONCheckBox.setChecked(exception == null && result.equals(mapOf(entry("text", "héllo"))));
        });

        // Delete
        serviceProxy.invoke("DELETE", "/httprpc-server/test", mapOf(entry("id", 101)), (result, exception) -> {
            deleteCheckBox.setChecked(exception == null && result.equals(true));
        });

        // Error
        serviceProxy.invoke("GET", "/httprpc-server/xyz", (result, exception) -> {
            errorCheckBox.setChecked(exception instanceof WebServiceException
                && ((WebServiceException)exception).getCode() == 404);
        });

        // Timeout
        serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(
            entry("value", 123),
            entry("delay", 6000)), (result, exception) -> {
            timeoutCheckBox.setChecked(exception instanceof SocketTimeoutException);
        });

        // Cancel
        Future<?> future = serviceProxy.invoke("GET", "/httprpc-server/test", mapOf(
            entry("value", 123),
            entry("delay", 6000)), (result, exception) -> {
            // No-op
        });

        Handler handler = new Handler();

        handler.postDelayed(() -> {
            cancelCheckBox.setChecked(future.cancel(true));
        }, 1000);

        // Image
        serviceProxy.invoke("GET", "/httprpc-server/test.jpg", (Bitmap result, Exception exception) -> {
            imageCheckBox.setChecked(exception == null && result != null);
            imageView.setImageBitmap(result);
        });

        // Text
        serviceProxy.invoke("GET", "/httprpc-server/test.txt", (String result, Exception exception) -> {
            textCheckBox.setChecked(exception == null && result != null);
            textView.setText(result);
        });

        // Shut down thread pool
        threadPool.shutdown();
    }
}
