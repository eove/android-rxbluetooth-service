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


import fr.eove.android.bluetooth.service.events.DeviceConnectRequest;
import fr.eove.android.bluetooth.service.events.DiscoveredDeviceEvent;
import fr.eove.android.bluetooth.service.events.DiscoveryStartRequest;
import fr.eove.android.bluetooth.service.events.DiscoveryStatus;
import fr.eove.android.bluetooth.service.events.DiscoveryStatusEvent;
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
    private Map<String, BluetoothDevice> devices = new HashMap<>();

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
                        if (name.equals("")) name = "NO NAME";
                        addDevice(bluetoothDevice);
                        Log.d(TAG, "Found device: " + name + " (" + address + ")");
                        EventBus.getDefault().post(new DiscoveredDeviceEvent(address, name));
                    }
                });
        discoveryStartSubscription = rxBluetooth.observeDiscovery()
                .filter(Action.isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
                .subscribe(new Action1<String>() {
                    @Override public void call(String action) {
                        Log.d(TAG, "starting discovery");
                        EventBus.getDefault().post(new DiscoveryStatusEvent(DiscoveryStatus.STARTED));
                    }
                });

        discoveryFinishSubscription = rxBluetooth.observeDiscovery()
                .filter(Action.isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
                .subscribe(new Action1<String>() {
                    @Override public void call(String action) {
                        Log.d(TAG, "finished discovery");
                        EventBus.getDefault().post(new DiscoveryStatusEvent(DiscoveryStatus.FINISHED));
                    }
                });

        EventBus.getDefault().register(this);
        Log.d(TAG, "started service");
    }

    private void addDevice(BluetoothDevice device) {
        devices.put(device.getAddress(), device);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDeviceConnectRequest(DeviceConnectRequest event) {
        final String address = event.address;
        Log.d(TAG, "requesting connection to " + address);
        if ( !devices.containsKey(address)) {
            Log.e(TAG, "unknown device with address: " + address);
            return;
        }

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
                            new BluetoothConnection(bluetoothSocket)
                                    .observeBytesStream(40)
                                    .subscribe(new Action1<byte[]>() {
                                        @Override
                                        public void call(byte[] bytes) {
                                            Formatter formatter = new Formatter();
                                            for (byte b : bytes) {
                                                formatter.format("%02x", b);
                                            }
                                            String hex = formatter.toString();
                                            Log.d("device", "rec: " + hex);

                                            Log.d(TAG, "rec: " + hex);
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);

        if (rxBluetooth != null) {
            rxBluetooth.cancelDiscovery();
        }

        unsubscribe(deviceSubscription);
        unsubscribe(discoveryStartSubscription);
        unsubscribe(discoveryFinishSubscription);
    }

//    private boolean isAlreadyConnectedTo(String address) {
//        if (currentConnection != null) {
//            if (currentConnection.isConnected() && currentConnection.getAddress().equals(address)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private void connectionCleanUps() {
//        if (connectedDeviceDisposable != null) {
//            connectedDeviceDisposable.dispose();
//        }
//        if (currentConnection != null) {
//            currentConnection.closeConnection();
//        }
//    }
//
//    @Subscribe(threadMode = ThreadMode.ASYNC)
//    public void onDeviceConnectRequest(DeviceConnectRequest event){
//        final String address = event.address;
//        Log.d(TAG, "requesting connection to " + address);
//
//        if (isAlreadyConnectedTo(address)) {
//            Log.d(TAG, "already connected to " + address);
//            return;
//        }
//
//        connectionCleanUps();
//
//        connectedDeviceDisposable = observeConnectedDevice(bluetoothAdapter.getRemoteDevice(address), MY_UUID)
//                            .map(new Function<BluetoothSocket, BluetoothConnection>() {
//                                @Override
//                                public BluetoothConnection apply(BluetoothSocket bluetoothSocket) throws Exception {
//                                    currentConnection = new BluetoothConnection(address, bluetoothSocket);
//                                    return currentConnection;
//                                }
//                            })
//                            .flatMap(new Function<BluetoothConnection, Observable<Byte>>() {
//                                @Override
//                                public Observable<Byte> apply(BluetoothConnection bluetoothConnection) throws Exception {
//                                    return bluetoothConnection.observeByteStream();
//                                }
//                            })
//                            .subscribe(new Consumer<Byte>() {
//                                @Override
//                                public void accept(Byte recv) throws Exception {
//                                    Log.d(TAG, "recv: " + recv);
//                                }
//                            }, new Consumer<Throwable>() {
//
//                                @Override
//                                public void accept(Throwable throwable) throws Exception {
//                                    Log.e(TAG, "Oooops: " + throwable.toString());
//                                }
//                            });
//        disposables.add(connectedDeviceDisposable);
//    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDiscoveryStartRequest(DiscoveryStartRequest request){
        Log.d(TAG, "requesting discovery start...");
        rxBluetooth.startDiscovery();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onDiscoveryStopRequest(DiscoveryCancelRequest request){
        Log.d(TAG, "requesting discovery stop...");
        rxBluetooth.cancelDiscovery();
        EventBus.getDefault().post(new DiscoveryStatusEvent(DiscoveryStatus.CANCELLED));
    }

//    private Observable<BluetoothDevice> observeDevices() {
//        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//
//        return Observable.create(new ObservableOnSubscribe<BluetoothDevice>() {
//            @Override
//            public void subscribe(final ObservableEmitter<BluetoothDevice> e) throws Exception {
//                final BroadcastReceiver receiver = new BroadcastReceiver() {
//                    @Override
//                    public void onReceive(Context context, Intent intent) {
//                        String action = intent.getAction();
//                        if (action.equals(BluetoothDevice.ACTION_FOUND)) {
//                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                            e.onNext(device);
//                        }
//                    }
//                };
//                BluetoothService.this.registerReceiver(receiver, filter);
//            }
//        });
//    }
//
//    public Observable<String> observeDiscovery() {
//        final IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//
//        return Observable.create(new ObservableOnSubscribe<String>() {
//            @Override
//            public void subscribe(final ObservableEmitter<String> e) throws Exception {
//                final BroadcastReceiver receiver = new BroadcastReceiver() {
//                    @Override
//                    public void onReceive(Context context, Intent intent) {
//                        e.onNext(intent.getAction());
//                    }
//                };
//                BluetoothService.this.registerReceiver(receiver, filter);
//            }
//        });
//    }
//
//    public Observable<BluetoothDevice> observePairing() {
//        final  IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
//
//        return Observable.create(new ObservableOnSubscribe<BluetoothDevice>() {
//            @Override
//            public void subscribe(final ObservableEmitter<BluetoothDevice> e) throws Exception {
//                final BroadcastReceiver receiver = new BroadcastReceiver() {
//                    @Override
//                    public void onReceive(Context context, Intent intent) {
//                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                        byte[] pinBytes = BuildConfig.PAIR_KEY.getBytes();
//                        try {
//                            device.setPin(pinBytes);
//                            device.setPairingConfirmation(false);
//                            device.createBond();
//                            e.onNext(device);
//                        } catch (Exception error) {
//                            e.onError(error);
//                        }
//                    }
//                };
//                BluetoothService.this.registerReceiver(receiver, filter);
//            }
//        });
//    }

//    public Observable<BluetoothSocket> observeConnectedDevice(final BluetoothDevice bluetoothDevice, final UUID uuid) {
//
//        return Observable.create(new ObservableOnSubscribe<BluetoothSocket>() {
//            @Override
//            public void subscribe(ObservableEmitter<BluetoothSocket> e) throws Exception {
//                BluetoothSocket bluetoothSocket;
//                String devName = bluetoothDevice.getName();
//                try {
//                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
//                    Log.d(TAG, "created socket");
//                } catch (Exception createError) {
//                    Log.e(TAG, "error when creating socket: " + createError.toString());
//                    bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
//                    Log.w(TAG, "created insecure socket");
//                }
//
//                try {
//                    Log.d(TAG, "connecting to " + devName + "...");
//                    bluetoothSocket.connect();
//                    Log.d(TAG, "connected to: " + devName);
//                    e.onNext(bluetoothSocket);
//                } catch (IOException error) {
//                    Log.e(TAG, "failed to connect: " + error.toString());
//                    e.onError(error);
//                }
//            }
//        });
//    }

    private static void unsubscribe(Subscription subscription) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
    }
}
