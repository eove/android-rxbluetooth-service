package fr.eove.android.bluetooth.service.events;


public class DeviceConnectRequest {
    public final String address;

    public DeviceConnectRequest(String address) {
        this.address = address;
    }
}
