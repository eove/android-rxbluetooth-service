package fr.eove.android.bluetooth.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import fr.eove.android.bluetooth.service.BluetoothService;
import fr.eove.android.bluetooth.service.events.DiscoveredDevice;
import fr.eove.android.bluetooth.service.events.DiscoveryStartRequest;
import fr.eove.android.bluetooth.service.events.DiscoveryStatusValue;
import fr.eove.android.bluetooth.service.events.DiscoveryStatus;
import fr.eove.android.bluetooth.service.events.DiscoveryCancelRequest;

public class DeviceListActivity extends Activity {

    private static final String TAG = "DeviceList Activity";

    private Button discoveryButton;
    private ProgressBar discoveryProgressBar;
    private boolean isDiscoveryStarted = false;

    private List<Device> devices = new ArrayList<>();
    ArrayAdapter<Device> deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        startBluetoothService();

        discoveryButton = (Button) findViewById(R.id.discovery_button);
        discoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! isDiscoveryStarted) {
                    isDiscoveryStarted = true;
                    devices.clear();
                    startDiscovery();
                } else {
                    isDiscoveryStarted = false;
                    cancelDiscovery();
                }
            }
        });

        discoveryProgressBar = (ProgressBar) findViewById(R.id.discovery_progress_bar);

        ListView list =  (ListView) findViewById(R.id.device_list_view);
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
    public void onDiscoveredDeviceEvent(DiscoveredDevice event) {
        devices.add(new Device(event.address, event.name));
        deviceListAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDiscoveryStatusEvent(DiscoveryStatus event) {

        if (event.status.equals(DiscoveryStatusValue.STARTED)) {
            updateUIonDiscoveryStarted();
            return;
        }

        if (event.status.equals(DiscoveryStatusValue.CANCELLED)) {
            updateUIonDiscoveryFinished();
            return;
        }

        if (event.status.equals(DiscoveryStatusValue.FINISHED)) {
            updateUIonDiscoveryFinished();
            return;
        }
    }

    private void updateUIonDiscoveryFinished() {
        discoveryButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        discoveryButton.setText(getResources().getText(R.string.start_dis));
        discoveryProgressBar.setVisibility(View.GONE);
    }

    private void updateUIonDiscoveryStarted() {
        discoveryButton.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        discoveryButton.setText(getResources().getText(R.string.stop_dis));
        discoveryProgressBar.setVisibility(View.VISIBLE);
    }

    public void startDiscovery() {
        EventBus.getDefault().post(new DiscoveryStartRequest());
    }

    public void cancelDiscovery() {
        EventBus.getDefault().post(new DiscoveryCancelRequest());
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
