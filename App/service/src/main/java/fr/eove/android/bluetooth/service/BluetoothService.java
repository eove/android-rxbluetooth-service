package fr.eove.android.bluetooth.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import fr.eove.android.bluetooth.service.events.DiscoveredDeviceEvent;
import fr.eove.android.bluetooth.service.events.DiscoveryStartRequest;
import fr.eove.android.bluetooth.service.events.DiscoveryStatus;
import fr.eove.android.bluetooth.service.events.DiscoveryStatusEvent;
import fr.eove.android.bluetooth.service.events.DeviceConnectRequest;
import fr.eove.android.bluetooth.service.events.DiscoveryCancelRequest;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class BluetoothService extends Service {

    private static final String TAG = "Service";
    private BluetoothAdapter bluetoothAdapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private List<BluetoothDevice> devices = new ArrayList<>();

    public BluetoothService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Communication with activities is done via EventBus. Good practice is to return null here.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        Log.d(TAG, "started service");
        disposables.add(observeDevices().subscribe(new Consumer<BluetoothDevice>() {
            @Override
            public void accept(BluetoothDevice bluetoothDevice) throws Exception {
                String address = bluetoothDevice.getAddress();
                devices.add(bluetoothDevice);
                Log.d(TAG, "Found device: " + address);
                EventBus.getDefault().post(new DiscoveredDeviceEvent(address));
            }
        }));

        disposables.add(observeDiscovery().subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                if (s.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                    Log.d(TAG, "starting discovery");
                    EventBus.getDefault().post(new DiscoveryStatusEvent(DiscoveryStatus.STARTED));
                } else {
                    Log.d(TAG, "discovery finished");
                    EventBus.getDefault().post(new DiscoveryStatusEvent(DiscoveryStatus.FINISHED));
                }
            }
        }));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        disposables.clear();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDeviceConnectRequest(DeviceConnectRequest event){
        Log.d(TAG, "requesting connection to " + event.address);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDiscoveryStartRequest(DiscoveryStartRequest request){
        Log.d(TAG, "requesting discovery start...");
        bluetoothAdapter.startDiscovery();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDiscoveryStopRequest(DiscoveryCancelRequest request){
        Log.d(TAG, "requesting discovery stop...");
        bluetoothAdapter.cancelDiscovery();
        EventBus.getDefault().post(new DiscoveryStatusEvent(DiscoveryStatus.CANCELLED));
    }

    private Observable<BluetoothDevice> observeDevices() {
        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

        return Observable.create(new ObservableOnSubscribe<BluetoothDevice>() {
            @Override
            public void subscribe(final ObservableEmitter<BluetoothDevice> e) throws Exception {
                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            e.onNext(device);
                        }
                    }
                };
                BluetoothService.this.registerReceiver(receiver, filter);
            }
        });
    }

    public Observable<String> observeDiscovery() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> e) throws Exception {
                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        e.onNext(intent.getAction());
                    }
                };
                BluetoothService.this.registerReceiver(receiver, filter);
            }
        });
    }
}
