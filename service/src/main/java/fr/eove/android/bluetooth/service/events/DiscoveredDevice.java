package fr.eove.android.bluetooth.service.events;


public class DiscoveredDevice {
    public final String address;
    public final String name;

    public DiscoveredDevice(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String toString() {
        return String.format("%s (%s)", name, address);
    }
}
