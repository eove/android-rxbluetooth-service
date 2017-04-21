package fr.eove.android.bluetooth.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class DeviceListActivity extends Activity {

    private static final String TAG = "DeviceList Activity";
    private Button startDiscoveryButton;
    private Button stopDiscoveryButton;
    private List<Device> devices = new ArrayList<>();
    ArrayAdapter<Device> deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startBluetoothService();

        startDiscoveryButton = (Button) findViewById(R.id.startDiscoveryButton);
        startDiscoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devices.clear();
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
                Device dev = devices.get(position);
                if (dev != null) {
                    startActivityForDevice(dev);
                } else {
                    Toast.makeText(DeviceListActivity.this, "Could not open device activity", Toast.LENGTH_LONG).show();
                }
            }
        });

        deviceListAdapter = new DeviceListAdapter();
        list.setAdapter(deviceListAdapter);
    }

    private class DeviceListAdapter extends ArrayAdapter<Device> {

        public DeviceListAdapter() {
            super(DeviceListActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = super.getView(position, convertView, parent);

            Device dev = devices.get(position);

            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);

            text1.setText(dev.name);
            text2.setText(dev.address);

            return view;
        }
    }

    private void startActivityForDevice(Device dev) {
        Intent i = new Intent(this, DeviceActivity.class);
        i.putExtra("address", dev.address);
        i.putExtra("name", dev.name);
        startActivity(i);
    }

    private void startBluetoothService() {
        Intent i = new Intent(this, BluetoothService.class);
        startService(i);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveredDeviceEvent(DiscoveredDeviceEvent event) {
        devices.add(new Device(event.address, event.name));
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
        Log.d(TAG, "started activity");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        Log.d(TAG, "stopped activity");
    }
}
