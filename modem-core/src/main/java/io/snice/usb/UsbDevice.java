package io.snice.usb;

/**
 * Represents a USB device. There is of course the javax.usb but it is very old style
 * interface and I wanted to hide it's ugliness.
 */
public interface UsbDevice {

    UsbDeviceDescriptor getDeviceDescriptor();
}
