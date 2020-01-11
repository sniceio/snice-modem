package io.snice.usb.hektor;

import io.hektor.core.ActorRef;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbScanner;
import io.snice.usb.VendorDescriptor;
import io.snice.usb.event.Subscribe;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.fsm.support.Subscription;
import io.snice.usb.impl.DefaultUsbEvent;
import io.snice.usb.linux.LibUsbConfiguration;

import java.util.List;
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
    public Subscription createSubscription(final Subscribe subscribeRequest) {
        return ActorSubscription.of(subscribeRequest);
    }

    @Override
    public void deviceAttached(final UsbDeviceDescriptor device, final List<Subscription> subscriptions) {
        System.err.println("New device attached: " + device);
        subscriptions.forEach(subscription -> deviceAttached(device, subscription));
    }

    @Override
    public void deviceAttached(final UsbDeviceDescriptor device, final Subscription subscription) {
        final var sender = (ActorRef)subscription.getSubscribeRequest().getSender();
        sender.tell(DefaultUsbEvent.attachEvent(device));
    }

    @Override
    public void deviceDetached(final UsbDeviceDescriptor device) {

    }

}
