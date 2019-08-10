package io.snice.usb.fsm;

import io.hektor.fsm.Context;
import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbDevice;
import io.snice.usb.UsbScanner;

import javax.usb.UsbServices;

public interface UsbManagerContext extends Context {

    UsbServices getUsbServices();

    UsbConfiguration getConfig();

    UsbScanner getUsbScanner();

    /**
     * Will be called when a new {@link UsbDevice} attached to the system
     *
     * @param device
     */
    void deviceAttached(final UsbDevice device);

    /**
     * Will be called when a new {@link UsbDevice} detached from the system.
     * @param device
     */
    void deviceDetached(final UsbDevice device);
}
