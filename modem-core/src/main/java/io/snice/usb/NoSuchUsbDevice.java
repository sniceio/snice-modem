package io.snice.usb;

import io.snice.preconditions.PreConditions;

import java.util.Optional;

public class NoSuchUsbDevice extends UsbException {

    private final String vendorId;
    private final String deviceId;
    private final Optional<String> linuxDevice;

    private NoSuchUsbDevice(final String vendorId, final String deviceId) {
        super(String.format("No USB device found for vendor id %s and device id %s", vendorId, deviceId));
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        linuxDevice = Optional.empty();
    }

    private NoSuchUsbDevice(final String vendorId, final String deviceId, final String linuxDevice) {
        super(String.format("No USB device found for vendor id %s and device id %s as linux device no %s", vendorId, deviceId, linuxDevice));
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.linuxDevice = Optional.of(linuxDevice);
    }

    public static NoSuchUsbDevice of(final UsbDeviceDescriptor desc) {
        PreConditions.assertNotNull(desc, "The usb device descriptor cannot be null");
        return new NoSuchUsbDevice(desc.getVendorId(), desc.getDeviceId());
    }

    public static NoSuchUsbDevice of(final UsbDeviceDescriptor desc, final String linuxDevice) {
        PreConditions.assertNotNull(desc, "The usb device descriptor cannot be null");
        PreConditions.assertNotEmpty(linuxDevice, "The linux device no cannot be the empty string or null");
        return new NoSuchUsbDevice(desc.getVendorId(), desc.getDeviceId(), linuxDevice);
    }

    public static NoSuchUsbDevice of(final UsbDeviceDescriptor desc, final int linuxDevice) {
        PreConditions.assertNotNull(desc, "The usb device descriptor cannot be null");
        PreConditions.assertArgument(linuxDevice >= 0 , "The linux device no cannot be less than zero");
        return new NoSuchUsbDevice(desc.getVendorId(), desc.getDeviceId(), Integer.toString(linuxDevice));
    }
}
