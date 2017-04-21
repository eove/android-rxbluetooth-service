package fr.eove.android.bluetooth.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Formatter;

import fr.eove.android.bluetooth.service.events.DeviceConnectRequest;
import fr.eove.android.bluetooth.service.events.DeviceData;

public class DeviceActivity extends Activity {

    private static final String TAG = "Device Activity";
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

        connectToDevice();
        Log.d(TAG, "created activity for " + name + "(" + address + ")");
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

    private void connectToDevice() {
        EventBus.getDefault().post(new DeviceConnectRequest(address));
    }

    private String deviceDataToString(byte[] data) {
        Formatter formatter = new Formatter();
        for (byte b : data) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceData(DeviceData event) {
        byte[] data = event.data;
        if (isLoggingStarted()) {
            logWindow.append(deviceDataToString(data));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
