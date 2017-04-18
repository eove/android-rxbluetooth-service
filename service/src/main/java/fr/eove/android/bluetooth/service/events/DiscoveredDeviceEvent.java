package fr.eove.android.bluetooth.service.events;


public class DiscoveredDeviceEvent {
    public final String address;
    public final String name;

    public DiscoveredDeviceEvent(String address, String name) {
        this.address = address;
        this.name = name;
    }
}
