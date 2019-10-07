package io.snice.usb;

import io.hektor.core.ActorRef;
import io.hektor.fsm.Scheduler;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.impl.LinuxUsbDeviceEvent;
import io.snice.usb.impl.LinuxUsbDeviceOld;
import io.snice.usb.linux.LibUsbConfiguration;

import java.util.Map;

public class ActorUsbManagerContext implements UsbManagerContext {

    private final ActorRef self;
    private final LibUsbConfiguration config;
    private final UsbScanner scanner;
    private final Map<String, VendorDescriptor> knownUsbVendors;

    public ActorUsbManagerContext(final ActorRef self, final UsbScanner scanner, final LibUsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors) {
        this.self = self;
        this.scanner = scanner;
        this.config = config;
        this.knownUsbVendors = knownUsbVendors;
    }

    @Override
    public LibUsbConfiguration getConfig() {
        return config;
    }

    @Override
    public UsbScanner getScanner() {
        return scanner;
    }

    @Override
    public Scheduler getScheduler() {
        throw new RuntimeException("haven't figured out this one just yet, another support class perhaps");
    }
}
