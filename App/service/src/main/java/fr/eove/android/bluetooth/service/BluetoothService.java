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

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class BluetoothService extends Service {

    private static final String TAG = "BluetoothService";
    private BluetoothAdapter bluetoothAdapter;
    Disposable deviceDisposables;

    public BluetoothService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "started service");
        deviceDisposables = observeDevices().subscribe(new Consumer<BluetoothDevice>() {
            @Override
            public void accept(BluetoothDevice bluetoothDevice) throws Exception {
                Log.d(TAG, "Found device: " + bluetoothDevice.getAddress());
            }
        });
        startDiscovery();
    }
    public boolean startDiscovery() {
        return bluetoothAdapter.startDiscovery();
    }

    public Observable<BluetoothDevice> observeDevices() {
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
}
