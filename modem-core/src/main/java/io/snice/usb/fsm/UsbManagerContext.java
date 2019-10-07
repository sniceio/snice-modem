package io.snice.usb.fsm;

import io.hektor.fsm.Context;
import io.snice.usb.UsbScanner;
import io.snice.usb.linux.LibUsbConfiguration;

public interface UsbManagerContext extends Context {

    LibUsbConfiguration getConfig();

    UsbScanner getScanner();

}
