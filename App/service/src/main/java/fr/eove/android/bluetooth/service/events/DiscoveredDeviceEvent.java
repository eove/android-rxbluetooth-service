package fr.eove.android.bluetooth.service.events;


public class DiscoveredDeviceEvent {
    public final String address;

    public DiscoveredDeviceEvent(String address) {
        this.address = address;
    }
}
