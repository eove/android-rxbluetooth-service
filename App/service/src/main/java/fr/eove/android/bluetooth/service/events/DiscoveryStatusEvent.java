package fr.eove.android.bluetooth.service.events;


public class DiscoveryStatusEvent {
    public final DiscoveryStatus status;

    public DiscoveryStatusEvent(DiscoveryStatus status) {
        this.status = status;
    }
}
