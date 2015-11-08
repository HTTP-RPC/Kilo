package org.httprpc.test;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
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

import org.httprpc.ResultHandler;
import org.httprpc.WebServiceProxy;

import static org.httprpc.Arguments.*;

public class MainActivity extends AppCompatActivity {
    private CheckBox addCheckBox;
    private CheckBox addValuesCheckBox;
    private CheckBox invertValueCheckBox;
    private CheckBox getCharactersCheckBox;
    private CheckBox getSelectionCheckBox;
    private CheckBox getStatisticsCheckBox;
    private CheckBox getTestDataCheckBox;
    private CheckBox getVoidCheckBox;
    private CheckBox getNullCheckBox;
    private CheckBox getLocaleCodeCheckBox;
    private CheckBox getUserNameCheckBox;

    private static final String TAG = MainActivity.class.getSimpleName();

    static {
        // Set global credentials
        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
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
        getStatisticsCheckBox = (CheckBox)findViewById(R.id.get_statistics_checkbox);
        getTestDataCheckBox = (CheckBox)findViewById(R.id.get_test_data_checkbox);
        getVoidCheckBox = (CheckBox)findViewById(R.id.get_void_checkbox);
        getNullCheckBox = (CheckBox)findViewById(R.id.get_null_checkbox);
        getLocaleCodeCheckBox = (CheckBox)findViewById(R.id.get_locale_code_checkbox);
        getUserNameCheckBox = (CheckBox)findViewById(R.id.get_user_name_checkbox);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Create service
        URL baseURL;
        try {
            baseURL = new URL("https://10.0.2.2:8443/webrpc-test-server/test/");
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        WebServiceProxy service = new WebServiceProxy(baseURL, threadPool);

        // Add
        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);

        service.invoke("add", addArguments, new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                addCheckBox.setChecked(exception == null && result.intValue() == 6);
            }
        });

        // Add values
        service.invoke("addValues", mapOf(entry("values", Arrays.asList(1, 2, 3, 4))), new ResultHandler<Number>() {
            @Override
            public void execute(Number result, Exception exception) {
                addValuesCheckBox.setChecked(exception == null && result.doubleValue() == 10.0);
            }
        });

        // Invert value
        service.invoke("invertValue", mapOf(entry("value", true)), new ResultHandler<Boolean>() {
            @Override
            public void execute(Boolean result, Exception exception) {
                invertValueCheckBox.setChecked(exception == null && result == false);
            }
        });

        // Get characters
        service.invoke("getCharacters", mapOf(entry("text", "Hello, World!")), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getCharactersCheckBox.setChecked(exception == null && result.equals(Arrays.asList("H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
            }
        });

        // Get selection
        service.invoke("getSelection", mapOf(entry("items", Arrays.asList("a", "b", "c", "d"))), new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getSelectionCheckBox.setChecked(exception == null && result.equals("a, b, c, d"));
            }
        });

        // Get statistics
        service.invoke("getStatistics", mapOf(entry("values", Arrays.asList(1, 3, 5))), new ResultHandler<Map<String, Number>>() {
            @Override
            public void execute(Map<String, Number> result, Exception exception) {
                getStatisticsCheckBox.setChecked(exception == null
                    && result.get("count").intValue() == 3
                    && result.get("average").doubleValue() == 3.0
                    && result.get("sum").doubleValue() == 9.0);
            }
        });

        // Get test data
        service.invoke("getTestData", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                HashMap<String, Object> row1 = new HashMap<>();
                row1.put("a", "hello");
                row1.put("b", 1L);
                row1.put("c", 2.0);

                HashMap<String, Object> row2 = new HashMap<>();
                row2.put("a", "goodbye");
                row2.put("b", 2L);
                row2.put("c", 4.0);

                getTestDataCheckBox.setChecked(exception == null && result.equals(Arrays.asList(row1, row2)));
            }
        });

        // Get void
        service.invoke("getVoid", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getVoidCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Get null
        service.invoke("getNull", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getNullCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Get locale code
        service.invoke("getLocaleCode", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getLocaleCodeCheckBox.setChecked(exception == null && result != null);
                getLocaleCodeCheckBox.setText(getLocaleCodeCheckBox.getText() + ": " + String.valueOf(result));
            }
        });

        // Get user name
        service.invoke("getUserName", new ResultHandler<Object>() {
            @Override
            public void execute(Object result, Exception exception) {
                getUserNameCheckBox.setChecked(exception == null && result.equals("tomcat"));
            }
        });

        // Shut down thread pool
        threadPool.shutdown();
    }
}
