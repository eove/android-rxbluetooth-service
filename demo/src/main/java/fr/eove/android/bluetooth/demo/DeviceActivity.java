package fr.eove.android.bluetooth.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;

import fr.eove.android.bluetooth.service.events.DeviceConnectRequest;

public class DeviceActivity extends Activity {

    private static final String TAG = "Device Activity";
    private String address;
    private  EditText logWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Bundle b = getIntent().getExtras();
        address = b.getString("address");
        Log.d(TAG, "started activity for " + address);
        logWindow = (EditText) findViewById(R.id.deviceLog);
    }

    private void connectToDevice() {
        EventBus.getDefault().post(new DeviceConnectRequest(address));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "started activity");
        connectToDevice();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "stopped activity");
    }
}
