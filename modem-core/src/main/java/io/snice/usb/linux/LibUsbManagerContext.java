package io.snice.usb.linux;

import io.hektor.fsm.Context;
import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbDevice;
import io.snice.usb.impl.LinuxUsbDevice;
import io.snice.usb.impl.LinuxUsbDeviceEvent;
import io.snice.usb.impl.LinuxUsbScanner;

public interface LibUsbManagerContext extends Context {

    UsbConfiguration getConfig();

    LinuxUsbScanner getScanner();

    void processUsbEvent(LinuxUsbDeviceEvent evt);

    /**
     * Will be called when a new {@link UsbDevice} attached to the system
     *
     * @param device
     */
    void deviceAttached(final LinuxUsbDevice device);

    /**
     * Will be called when a new {@link UsbDevice} detached from the system.
     * @param device
     */
    void deviceDetached(final LinuxUsbDevice device);
}
