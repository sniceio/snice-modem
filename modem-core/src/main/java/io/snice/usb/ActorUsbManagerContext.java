package io.snice.usb;

import io.hektor.core.ActorRef;
import io.hektor.fsm.Scheduler;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.impl.LinuxUsbDevice;
import io.snice.usb.impl.LinuxUsbDeviceEvent;
import io.snice.usb.impl.LinuxUsbScanner;

import java.util.Map;

public class ActorUsbManagerContext implements UsbManagerContext {

    private final ActorRef self;
    private final UsbConfiguration config;
    private final LinuxUsbScanner scanner;
    private final Map<String, VendorDescriptor> knownUsbVendors;

    public ActorUsbManagerContext(final ActorRef self, final LinuxUsbScanner scanner, final UsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors) {
        this.self = self;
        this.scanner = scanner;
        this.config = config;
        this.knownUsbVendors = knownUsbVendors;
    }

    @Override
    public UsbConfiguration getConfig() {
        return config;
    }

    @Override
    public LinuxUsbScanner getScanner() {
        return scanner;
    }

    @Override
    public void processUsbEvent(final LinuxUsbDeviceEvent evt) {
        self.tell(evt);
    }

    @Override
    public void deviceAttached(final LinuxUsbDevice device) {

    }

    @Override
    public void deviceDetached(final LinuxUsbDevice device) {

    }

    @Override
    public Scheduler getScheduler() {
        throw new RuntimeException("haven't figured out this one just yet, another support class perhaps");
    }
}
