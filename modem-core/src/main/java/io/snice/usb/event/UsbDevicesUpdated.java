package io.snice.usb.event;

import io.snice.usb.UsbDeviceDescriptor;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertCollectionNotEmpty;

public class UsbDevicesUpdated {

    private final List<UsbDeviceDescriptor> devices;
    private final boolean isAdded;

    public static UsbDevicesUpdated devicesAdded(final List<UsbDeviceDescriptor> added) {
        assertCollectionNotEmpty(added);
        return new UsbDevicesUpdated(true, added);
    }

    public static UsbDevicesUpdated devicesRemoved(final List<UsbDeviceDescriptor> removed) {
        assertCollectionNotEmpty(removed);
        return new UsbDevicesUpdated(false, removed);
    }

    private UsbDevicesUpdated(final boolean isAdded, final List<UsbDeviceDescriptor> devices) {
        this.isAdded = isAdded;
        this.devices = devices;
    }

    public boolean isAdded() {
        return isAdded;
    }

    public List<UsbDeviceDescriptor> getDevices() {
        return devices;
    }

}
