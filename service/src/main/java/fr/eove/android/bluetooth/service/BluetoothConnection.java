package fr.eove.android.bluetooth.service;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class BluetoothConnection {

    private static final String TAG = "Connection";
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Observable<Byte> mObserveInputStream;
    private boolean connected = false;
    private String address = "";

    public BluetoothConnection(String address, BluetoothSocket socket) throws Exception {
        if (socket == null) {
            throw new InvalidParameterException("socket can't be null");
        }
        this.address = address;
        this.socket = socket;

        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            connected = true;
        } catch (IOException e) {
            throw new Exception("Can't get stream from socket");
        } finally {
            if (!connected) {
                closeConnection();
            }
        }
    }

    public String getAddress() {
        return address;
    }

    public boolean isConnected() {
        return connected;
    }

    public Observable<Byte> observeByteStream() {
        if (mObserveInputStream == null) {

            mObserveInputStream = Observable.create(new ObservableOnSubscribe<Byte>() {
                @Override
                public void subscribe(ObservableEmitter<Byte> e) throws Exception {
                    try {
                        e.onNext( (byte) inputStream.read());
                    } catch (IOException error) {
                        e.onError(error);
                    } finally {
                        if (! connected) {
                            closeConnection();
                        }
                    }
                }
            });
        }

        return mObserveInputStream;
    }

    public boolean send(byte[] bytes) {
        if (!connected) return false;

        try {
            outputStream.write(bytes);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            // Error occurred. Better to close terminate the connection
            connected = false;
            Log.e(TAG, "Fail to send data");
            return false;
        } finally {
            if (!connected) {
                closeConnection();
            }
        }
    }


    public boolean send(String text) {
        byte[] sBytes = text.getBytes();
        return send(sBytes);
    }

    public void closeConnection() {
        try {
            connected = false;

            if (inputStream != null) {
                inputStream.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }

            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
