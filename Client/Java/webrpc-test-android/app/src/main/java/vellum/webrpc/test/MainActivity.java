package vellum.webrpc.test;

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
import java.util.Locale;
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

import vellum.webrpc.R;
import vellum.webrpc.ResultHandler;
import vellum.webrpc.WebRPCService;

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
    private CheckBox isUserInRoleCheckBox;

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
        WebRPCService.setResultDispatcher(new Executor() {
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
        isUserInRoleCheckBox = (CheckBox)findViewById(R.id.is_user_in_role_checkbox);
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

        WebRPCService service = new WebRPCService(baseURL, threadPool);

        // Add
        HashMap<String, Object> addArguments = new HashMap<>();
        addArguments.put("a", 2);
        addArguments.put("b", 4);

        service.invoke("add", addArguments, new ResultHandler<Number>() {
            @Override
            public void execute(final Number result, final Exception exception) {
                addCheckBox.setChecked(exception == null && result.intValue() == 6);
            }
        });

        // Add values
        HashMap<String, Object> addValuesArguments = new HashMap<>();
        addValuesArguments.put("values", Arrays.asList(1, 2, 3, 4));

        service.invoke("addValues", addValuesArguments, new ResultHandler<Number>() {
            @Override
            public void execute(final Number result, final Exception exception) {
                addValuesCheckBox.setChecked(exception == null && result.doubleValue() == 10.0);
            }
        });

        // Invert value
        HashMap<String, Object> invertValueArguments = new HashMap<>();
        invertValueArguments.put("value", true);

        service.invoke("invertValue", invertValueArguments, new ResultHandler<Boolean>() {
            @Override
            public void execute(final Boolean result, final Exception exception) {
                invertValueCheckBox.setChecked(exception == null && result == false);
            }
        });

        // Get characters
        HashMap<String, Object> getCharactersArguments = new HashMap<>();
        getCharactersArguments.put("text", "Hello, World!");

        service.invoke("getCharacters", getCharactersArguments, new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
                getCharactersCheckBox.setChecked(exception == null && result.equals(Arrays.asList("H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!")));
            }
        });

        // Get selection
        HashMap<String, Object> getSelectionArguments = new HashMap<>();
        getSelectionArguments.put("items", Arrays.asList("a", "b", "c", "d"));

        service.invoke("getSelection", getSelectionArguments, new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
                getSelectionCheckBox.setChecked(exception == null && result.equals("a, b, c, d"));
            }
        });

        // Get statistics
        HashMap<String, Object> getStatisticsArguments = new HashMap<>();
        getStatisticsArguments.put("values", Arrays.asList(1, 3, 5));

        service.invoke("getStatistics", getStatisticsArguments, new ResultHandler<Map<String, Object>>() {
            @Override
            public void execute(final Map<String, Object> result, final Exception exception) {
                Statistics statistics = (exception == null) ? new Statistics(result) : null;

                getStatisticsCheckBox.setChecked(statistics != null
                    && statistics.getCount() == 3
                    && statistics.getAverage() == 3.0
                    && statistics.getSum() == 9.0);
            }
        });

        // Get test data
        service.invoke("getTestData", getSelectionArguments, new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
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
        service.invoke("getVoid", getSelectionArguments, new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
                getVoidCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Get null
        service.invoke("getNull", getSelectionArguments, new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
                getNullCheckBox.setChecked(exception == null && result == null);
            }
        });

        // Get locale code
        service.invoke("getLocaleCode", new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
                getLocaleCodeCheckBox.setChecked(exception == null && result.equals(Locale.getDefault().toString()));
            }
        });

        // Get user name
        service.invoke("getUserName", new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
                getUserNameCheckBox.setChecked(exception == null && result.equals("tomcat"));
            }
        });

        // Is user in role
        HashMap<String, Object> isUserInRoleArguments = new HashMap<>();
        isUserInRoleArguments.put("role", "tomcat");

        service.invoke("isUserInRole", isUserInRoleArguments, new ResultHandler<Object>() {
            @Override
            public void execute(final Object result, final Exception exception) {
                isUserInRoleCheckBox.setChecked(exception == null && result.equals(true));
            }
        });

        // Shut down thread pool
        threadPool.shutdown();
    }
}
