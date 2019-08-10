package io.snice.usb;

import io.hektor.fsm.Scheduler;
import io.snice.usb.fsm.UsbManagerContext;

import javax.usb.UsbServices;

public class ActorUsbManagerContext implements UsbManagerContext {

    private final UsbServices usbServices;
    private final UsbConfiguration config;
    private final UsbScanner scanner;

    public ActorUsbManagerContext(final UsbServices usbServices, final UsbScanner scanner, final UsbConfiguration config) {
        this.usbServices = usbServices;
        this.scanner = scanner;
        this.config = config;
    }

    @Override
    public UsbServices getUsbServices() {
        return usbServices;
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
