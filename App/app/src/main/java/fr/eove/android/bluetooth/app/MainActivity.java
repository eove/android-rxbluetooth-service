package fr.eove.android.bluetooth.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import fr.eove.android.bluetooth.service.BluetoothService;
import fr.eove.android.bluetooth.service.events.DiscoveredDeviceEvent;
import fr.eove.android.bluetooth.service.events.DiscoveryStartRequest;
import fr.eove.android.bluetooth.service.events.DiscoveryStatus;
import fr.eove.android.bluetooth.service.events.DiscoveryStatusEvent;
import fr.eove.android.bluetooth.service.events.DiscoveryCancelRequest;

public class MainActivity extends Activity {

    private static final String TAG = "App";
    private Button startDiscoveryButton;
    private Button stopDiscoveryButton;
    private List<String> deviceAddresses = new ArrayList<>();
    ArrayAdapter<String> deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBluetoothService();

        startDiscoveryButton = (Button) findViewById(R.id.startDiscoveryButton);
        startDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceAddresses.clear();
                startDiscovery();
            }
        });
        startDiscoveryButton.setEnabled(true);

        stopDiscoveryButton = (Button) findViewById(R.id.stopDiscoveryButton);
        stopDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDiscovery();
            }
        });
        stopDiscoveryButton.setEnabled(false);

        ListView list =  (ListView) findViewById(R.id.deviceListView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String address = deviceAddresses.get(position);
                if (address != null) {
                    startActivityForDevice(address);
                } else {
                    Toast.makeText(MainActivity.this, "Could not open device window", Toast.LENGTH_LONG);
                }
            }
        });

        deviceListAdapter = new DeviceListAdapter();
        list.setAdapter(deviceListAdapter);
    }

    private class DeviceListAdapter extends ArrayAdapter<String> {

        public DeviceListAdapter() {
            super(MainActivity.this, R.layout.item_view, deviceAddresses);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.item_view, parent, false);
            }

            String address = deviceAddresses.get(position);

            TextView textView = (TextView) itemView.findViewById(R.id.item_name);
            textView.setText(address);

            return itemView;
        }
    }

    private void startActivityForDevice(String address) {
        Intent i = new Intent(this, DeviceActivity.class);
        i.putExtra("address", address);
        startActivity(i);
    }

    private void startBluetoothService() {
        Intent i = new Intent(this, BluetoothService.class);
        startService(i);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveredDeviceEvent(DiscoveredDeviceEvent event) {
        deviceAddresses.add(event.address);
        deviceListAdapter.notifyDataSetChanged();
        Log.d(TAG, "Found device with address " + event.address);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveryStatusEvent(DiscoveryStatusEvent event) {

        if (event.status.equals(DiscoveryStatus.STARTED)) {
            Log.d(TAG, "discovery started!");
            startDiscoveryButton.setEnabled(false);
            stopDiscoveryButton.setEnabled(true);
            return;
        }

        if (event.status.equals(DiscoveryStatus.CANCELLED)) {
            Log.d(TAG, "discovery cancelled!");
            stopDiscoveryButton.setEnabled(false);
            startDiscoveryButton.setEnabled(true);
            return;
        }

        if (event.status.equals(DiscoveryStatus.FINISHED)) {
            Log.d(TAG, "discovery finished!");
            stopDiscoveryButton.setEnabled(false);
            startDiscoveryButton.setEnabled(true);
            return;
        }
    }

    public void startDiscovery() {
        Log.d(TAG, "Starting discovery...");
        EventBus.getDefault().post(new DiscoveryStartRequest());
    }

    public void cancelDiscovery() {
        Log.d(TAG, "Stopping discovery...");
        EventBus.getDefault().post(new DiscoveryCancelRequest());
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
