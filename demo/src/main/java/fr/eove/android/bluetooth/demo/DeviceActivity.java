package fr.eove.android.bluetooth.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Formatter;

import fr.eove.android.bluetooth.service.events.ConnectRequest;
import fr.eove.android.bluetooth.service.events.ReceivedData;
import fr.eove.android.bluetooth.service.events.DisconnectRequest;

public class DeviceActivity extends Activity {

    private static final String TAG = "Device Activity";
    private static final Integer LOG_WINDOW_MAX_SIZE = 5000;

    private String address;
    private String name;
    private boolean isLogging;

    private  TextView logWindow;
    private TextView idView;
    private Button logStartStopButton;
    private Button logClearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Bundle b = getIntent().getExtras();
        address = b.getString("address");
        name = b.getString("name");

        logWindow = (TextView) findViewById(R.id.log_window);
        logWindow.setMovementMethod(new ScrollingMovementMethod());

        idView = (TextView) findViewById(R.id.device_id);
        idView.setText(name + " - " + address);

        logStartStopButton = (Button) findViewById(R.id.log_startstop_button);
        logStartStopButton.setText(getResources().getString(R.string.start_log));
        logStartStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoggingStarted()) {
                    stopLoggingAndUpdateUI();
                } else {
                    startLoggingAndUpdateUI();
                }
            }
        });

        logClearButton = (Button) findViewById(R.id.log_clear_button);
        logClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logWindow.setText("");
            }
        });

        Log.d(TAG, "created activity for " + name + "(" + address + ")");
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        connectToDevice();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        disconnectFromDevice();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceData(ReceivedData receivedData) {
        byte[] data = receivedData.payload;
        if (isLoggingStarted()) {
            logWindow.append(deviceDataToString(data));
        }

        if (isLoggingSizeTooHigh()) {
            Toast.makeText(this, getResources().getString(R.string.log_too_long_warning), Toast.LENGTH_LONG).show();
            logWindow.setText("");
        }
    }

    private String deviceDataToString(byte[] data) {
        Formatter formatter = new Formatter();
        for (byte b : data) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private void startLoggingAndUpdateUI() {
        logStartStopButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        logStartStopButton.setText(getResources().getString(R.string.stop_log));
        isLogging = true;
    }

    private void stopLoggingAndUpdateUI() {
        logStartStopButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        logStartStopButton.setText(getResources().getString(R.string.start_log));
        isLogging = false;
    }

    private boolean isLoggingStarted() {
        return isLogging;
    }

    private boolean isLoggingSizeTooHigh() {
        return logWindow.getText().length() >= LOG_WINDOW_MAX_SIZE;
    }

    private void connectToDevice() {
        EventBus.getDefault().post(new ConnectRequest(address));
    }

    private void disconnectFromDevice() {
        EventBus.getDefault().post(new DisconnectRequest());
    }
}
