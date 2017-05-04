package fr.eove.android.bluetooth.service.events;


import java.util.Formatter;

public class ReceivedData {
    public final byte[] payload;

    public ReceivedData(byte[] data) {
        this.payload = data;
    }

    public String toString() {
        Formatter formatter = new Formatter();
        for (byte b : payload) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
