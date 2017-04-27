package fr.eove.android.bluetooth.service.events;


public class DiscoveryStatus {
    public final DiscoveryStatusValue status;

    public DiscoveryStatus(DiscoveryStatusValue status) {
        this.status = status;
    }

    public String toString() {
        return status.toString();
    }
}
