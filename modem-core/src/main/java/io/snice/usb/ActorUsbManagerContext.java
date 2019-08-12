package io.snice.usb;

import io.hektor.fsm.Scheduler;
import io.snice.usb.fsm.UsbManagerContext;

import java.util.Map;

public class ActorUsbManagerContext implements UsbManagerContext {

    private final UsbConfiguration config;
    private final UsbScanner scanner;
    private final Map<String, VendorDescriptor> knownUsbVendors;

    public ActorUsbManagerContext(final UsbScanner scanner, final UsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors) {
        this.scanner = scanner;
        this.config = config;
        this.knownUsbVendors = knownUsbVendors;
    }

    @Override
    public UsbConfiguration getConfig() {
        return config;
    }

    @Override
    public UsbScanner getUsbScanner() {
        return scanner;
    }

    @Override
    public void deviceAttached(final UsbDevice device) {

    }

    @Override
    public void deviceDetached(final UsbDevice device) {

    }

    @Override
    public Scheduler getScheduler() {
        throw new RuntimeException("haven't figured out this one just yet, another support class perhaps");
    }
}
