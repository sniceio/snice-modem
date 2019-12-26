package io.snice.usb.impl;

import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.event.UsbEvent;

import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class DefaultUsbEvent implements UsbEvent {

    private final Optional<UsbDevice> device;
    private final UsbDeviceDescriptor descriptor;

    private DefaultUsbEvent(final Optional<UsbDevice> device, final UsbDeviceDescriptor descriptor) {
        this.device = device;
        this.descriptor = descriptor;
    }

    public static UsbAttachEvent attachEvent(final UsbDeviceDescriptor descriptor) {
        assertNotNull(descriptor, "The USB descriptor cannot be null");
        return new DefaultUsbAttachEvent(descriptor);
    }

    public static UsbDetachEvent detachEvent(final UsbDevice device) {
        assertNotNull(device, "The UsbDevice cannot be null");
        return new DefaultUsbDetachEvent(device);
    }

    @Override
    public Optional<UsbDevice> getDevice() {
        return device;
    }

    @Override
    public UsbDeviceDescriptor getUsbDeviceDescriptor() {
        return descriptor;
    }

    private static class DefaultUsbAttachEvent extends DefaultUsbEvent implements UsbAttachEvent {
        private DefaultUsbAttachEvent(final UsbDeviceDescriptor descriptor) {
            super(Optional.empty(), descriptor);
        }
    }

    private static class DefaultUsbDetachEvent extends DefaultUsbEvent implements UsbDetachEvent {
        private DefaultUsbDetachEvent(final UsbDevice device) {
            super(Optional.of(device), device.getDeviceDescriptor());
        }
    }
}
