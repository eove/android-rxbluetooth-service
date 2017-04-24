package fr.eove.android.bluetooth.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.github.ivbaranov.rxbluetooth.Action;
import com.github.ivbaranov.rxbluetooth.BluetoothConnection;
import com.github.ivbaranov.rxbluetooth.RxBluetooth;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fr.eove.android.bluetooth.service.events.ConnectRequest;
import fr.eove.android.bluetooth.service.events.ReceivedData;
import fr.eove.android.bluetooth.service.events.DisconnectRequest;
import fr.eove.android.bluetooth.service.events.DiscoveredDevice;
import fr.eove.android.bluetooth.service.events.DiscoveryStartRequest;
import fr.eove.android.bluetooth.service.events.DiscoveryStatusValue;
import fr.eove.android.bluetooth.service.events.DiscoveryStatus;
import fr.eove.android.bluetooth.service.events.DiscoveryCancelRequest;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;

public class BluetoothService extends Service {

    private static final String TAG = "Service";
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    RxBluetooth rxBluetooth;
    private Subscription deviceSubscription;
    private Subscription deviceConnectSubscription;
    private Subscription discoveryStartSubscription;
    private Subscription discoveryFinishSubscription;
    private Subscription currentConnectionSubscription;
    private Map<String, BluetoothDevice> devices = new HashMap<>();
    private BluetoothConnection currentConnection;

    public BluetoothService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // Communication with activities is done via EventBus. Good practice is to return null here.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        rxBluetooth = new RxBluetooth(this);
        if (!rxBluetooth.isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled, stopping service");
            stopSelf();
            return;
        }

        rxBluetooth = new RxBluetooth(BluetoothService.this);

        deviceSubscription = rxBluetooth.observeDevices()
                .subscribe(new Action1<BluetoothDevice>() {
                    @Override
                    public void call(BluetoothDevice bluetoothDevice) {
                        String address = bluetoothDevice.getAddress();
                        String name = bluetoothDevice.getName();

                        if (! isDeviceKnown(address)) {
                            addDevice(bluetoothDevice);
                        }
                        if (name.equals("")) name = "NO NAME";
                        Log.d(TAG, "found device: " + name + " (" + address + ")");
                        EventBus.getDefault().post(new DiscoveredDevice(address, name));
                    }
                });
        discoveryStartSubscription = rxBluetooth.observeDiscovery()
                .filter(Action.isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
                .subscribe(new Action1<String>() {
                    @Override public void call(String action) {
                        Log.d(TAG, "starting discovery");
                        EventBus.getDefault().post(new DiscoveryStatus(DiscoveryStatusValue.STARTED));
                    }
                });

        discoveryFinishSubscription = rxBluetooth.observeDiscovery()
                .filter(Action.isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
                .subscribe(new Action1<String>() {
                    @Override public void call(String action) {
                        Log.d(TAG, "finished discovery");
                        EventBus.getDefault().post(new DiscoveryStatus(DiscoveryStatusValue.FINISHED));
                    }
                });

        EventBus.getDefault().register(this);
        Log.d(TAG, "started service");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);

        if (rxBluetooth != null) {
            rxBluetooth.cancelDiscovery();
        }

        unsubscribe(deviceSubscription);
        unsubscribe(deviceConnectSubscription);
        unsubscribe(discoveryStartSubscription);
        unsubscribe(discoveryFinishSubscription);
        unsubscribe(currentConnectionSubscription);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onConnectRequest(ConnectRequest event) {
        final String address = event.address;

        Log.d(TAG, "requesting connection to " + event.address);

        if ( ! isDeviceKnown(address)) {
            Log.e(TAG, "unknown device with address: " + address);
            return;
        }
        
        installConnectionHandlerFor(address);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDisconnectRequest(DisconnectRequest event) {
        Log.d(TAG, "requesting disconnection...");

        if (currentConnection != null) {
            currentConnection.closeConnection();
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDiscoveryStartRequest(DiscoveryStartRequest request){
        Log.d(TAG, "requesting discovery start...");
        rxBluetooth.startDiscovery();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDiscoveryStopRequest(DiscoveryCancelRequest request){
        Log.d(TAG, "requesting discovery stop...");
        rxBluetooth.cancelDiscovery();
        EventBus.getDefault().post(new DiscoveryStatus(DiscoveryStatusValue.CANCELLED));
    }

    private void addDevice(BluetoothDevice device) {
        devices.put(device.getAddress(), device);
    }

    private boolean isDeviceKnown(String address) {
        return devices.containsKey(address);
    }

    private void installConnectionHandlerFor(final String address) {
        deviceConnectSubscription = rxBluetooth.observeConnectDevice(devices.get(address), MY_UUID)
                .subscribe(new Subscriber<BluetoothSocket>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable connError) {
                        Log.e(TAG, "error when connecting to " + address, connError);
                    }

                    @Override
                    public void onNext(BluetoothSocket bluetoothSocket) {
                        try {
                            currentConnection = new BluetoothConnection(bluetoothSocket);
                            currentConnectionSubscription = currentConnection
                                    .observeBytesStream(40)
                                    .subscribe(new Action1<byte[]>() {
                                        @Override
                                        public void call(byte[] bytes) {
                                            Log.d(TAG, "rec: " + toString(bytes));
                                            EventBus.getDefault().post(new ReceivedData(bytes));
                                        }

                                        private String toString(byte[] bytes) {
                                            Formatter formatter = new Formatter();
                                            for (byte b : bytes) {
                                                formatter.format("%02x", b);
                                            }
                                            return formatter.toString();
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable socketError) {
                                            Log.e(TAG, "error when receiving bytes", socketError);
                                        }
                                    });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private static void unsubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
    }
}
