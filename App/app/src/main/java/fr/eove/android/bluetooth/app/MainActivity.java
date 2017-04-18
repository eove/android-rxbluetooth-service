package fr.eove.android.bluetooth.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import fr.eove.android.bluetooth.service.BluetoothService;
import fr.eove.android.bluetooth.service.events.DiscoveredDeviceEvent;

public class MainActivity extends Activity {

    private static final String TAG = "Bluetooth App";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBluetoothService();
    }

    private void startBluetoothService() {
        Intent i = new Intent(this, BluetoothService.class);
        startService(i);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(DiscoveredDeviceEvent event) {
        Log.d(TAG, "Found device with address " + event.address);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        Log.d(TAG, "started application");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        Log.d(TAG, "stopped application");
    }
}
