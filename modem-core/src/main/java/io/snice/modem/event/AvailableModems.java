package io.snice.modem.event;

import io.snice.usb.UsbDeviceDescriptor;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class AvailableModems {

    private final List<UsbDeviceDescriptor> modems;

    private AvailableModems(final List<UsbDeviceDescriptor> modems) {
        this.modems = modems;
    }

    public static AvailableModems of(final List<UsbDeviceDescriptor> modems) {
        assertNotNull(modems);
        return new AvailableModems(modems);
    }

    public List<UsbDeviceDescriptor> getAvailableModems() {
        return modems;
    }
}
