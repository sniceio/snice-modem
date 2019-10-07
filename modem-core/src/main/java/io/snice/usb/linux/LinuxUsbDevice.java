package io.snice.usb.linux;

import io.snice.usb.DeviceId;
import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbInterface;

import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class LinuxUsbDevice implements UsbDevice {

    private final LinuxUsbDeviceDescriptor descriptor;

    private LinuxUsbDevice(final LinuxUsbDeviceDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public static Builder of(final LinuxUsbDeviceDescriptor descriptor) {
        assertNotNull(descriptor, "the USB Device Descriptor cannot be null");
        return new Builder(descriptor);
    }


    @Override
    public UsbDeviceDescriptor getDeviceDescriptor() {
        return descriptor;
    }

    @Override
    public String getVendorId() {
        return descriptor.getVendorId();
    }

    @Override
    public String getProductId() {
        return descriptor.getProductId();
    }

    @Override
    public DeviceId getId() {
        return descriptor.getId();
    }

    @Override
    public Optional<String> getVendorDescription() {
        return descriptor.getVendorDescription();
    }

    @Override
    public List<UsbInterface> getInterfaces() {
        return null;
    }

    public static class Builder {
        private final LinuxUsbDeviceDescriptor descriptor;

        private Builder(final LinuxUsbDeviceDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        public LinuxUsbDevice build() {
            return new LinuxUsbDevice(descriptor);
        }
    }
}
