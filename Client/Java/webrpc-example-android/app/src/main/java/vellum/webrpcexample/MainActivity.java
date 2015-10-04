package vellum.webrpcexample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
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

import vellum.webrpc.ResultHandler;
import vellum.webrpc.WebRPCService;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private int temperature = 0;

    private Switch onSwitch;
    private TextView temperatureLabel;
    private RadioGroup fanSpeedRadioGroup;

    private WebRPCService service = null;

    private static final int MIN_TEMPERATURE = 32;
    private static final int MAX_TEMPERATURE = 96;

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

        onSwitch = (Switch)findViewById(R.id.on_switch);

        onSwitch.setOnCheckedChangeListener(this);

        temperatureLabel = (TextView)findViewById(R.id.temperature_label);
        fanSpeedRadioGroup = (RadioGroup)findViewById(R.id.fan_speed_radio_group);

        // Create service
        URL baseURL;
        try {
            baseURL = new URL("https://10.0.2.2:8443/webrpc-test-server/ac/");
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        service = new WebRPCService(baseURL, threadPool);
    }

    @Override
    protected void onResume() {
        super.onResume();

        service.invoke("getStatus", new ResultHandler<Map<String, Object>>() {
            @Override
            public void execute(Map<String, Object> status, Exception exception) {
                if (exception == null) {
                    // Update power
                    onSwitch.setChecked((boolean)status.get("on"));

                    // Update temperature
                    temperature = ((Number)status.get("temperature")).intValue();

                    updateTemperatureLabel();

                    // Update fan speed
                    int fanSpeed = ((Number)status.get("fanSpeed")).intValue();

                    ((RadioButton)fanSpeedRadioGroup.getChildAt(fanSpeed - 1)).setChecked(true);

                } else {
                    handleServiceError(exception);
                }
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == onSwitch) {
            HashMap<String, Object> setOnArguments = new HashMap<>();
            setOnArguments.put("on", onSwitch.isChecked());

            service.invoke("setOn", setOnArguments, new ResultHandler<Map<String, Object>>() {
                @Override
                public void execute(Map<String, Object> result, Exception exception) {
                    if (exception != null) {
                        handleServiceError(exception);
                    }
                }
            });
        }
    }

    public void onDecrementTemperatureButtonClicked(View view) {
        temperature = Math.max(temperature - 1, MIN_TEMPERATURE);

        updateUnitTemperature();
    }

    public void onIncrementTemperatureButtonClicked(View view) {
        temperature = Math.min(temperature + 1, MAX_TEMPERATURE);

        updateUnitTemperature();
    }

    private void updateUnitTemperature() {
        HashMap<String, Object> setTemperatureArguments = new HashMap<>();
        setTemperatureArguments.put("temperature", temperature);

        service.invoke("setTemperature", setTemperatureArguments, new ResultHandler<Void>() {
            @Override
            public void execute(Void result, Exception exception) {
                if (exception != null) {
                    handleServiceError(exception);
                }
            }
        });

        updateTemperatureLabel();
    }

    private void updateTemperatureLabel() {
        temperatureLabel.setText(String.format("%d° F", temperature));
    }

    public void onFanSpeedButtonClicked(View view) {
        int index = fanSpeedRadioGroup.indexOfChild(view);

        int fanSpeed = index + 1;

        HashMap<String, Object> setFanSpeedArguments = new HashMap<>();
        setFanSpeedArguments.put("fanSpeed", fanSpeed);

        service.invoke("setFanSpeed", setFanSpeedArguments, new ResultHandler<Void>() {
            @Override
            public void execute(Void result, Exception exception) {
                if (exception != null) {
                    handleServiceError(exception);
                }
            }
        });
    }

    private void handleServiceError(Exception exception) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        alertDialog.setTitle(getResources().getString(R.string.service_error_title));
        alertDialog.setMessage(getResources().getString(R.string.service_error_message));

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

        alertDialog.show();
    }
}
