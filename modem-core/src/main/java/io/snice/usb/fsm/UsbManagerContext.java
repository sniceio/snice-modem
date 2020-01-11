package io.snice.usb.fsm;

import io.hektor.fsm.Context;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbScanner;
import io.snice.usb.event.Subscribe;
import io.snice.usb.fsm.support.Subscription;
import io.snice.usb.linux.LibUsbConfiguration;

import java.util.List;

public interface UsbManagerContext extends Context {

    LibUsbConfiguration getConfig();

    UsbScanner getScanner();

    /**
     * Ask the context to create a corresponding, environment specific, {@link Subscription}
     * object.
     *
     * In general, an FSM is unaware of the execution environment within it is operating. Hence, the FSM
     * doesn't know e.g. how to "contact" the subscriber since that is dependent on that very execution environment.
     * If we are exeucting the FSM in an actor container of some sort, the {@link Subscription} must contain
     * the actor address, if we are executing in an HTTP container, perhaps the {@link Subscription} will contain
     * a HTTP callback URL etc etc.
     *
     * @param subscribeRequest
     * @return an object representing the {@link Subscription} and is execution environment specific.
     */
    Subscription createSubscription(Subscribe subscribeRequest);

    void deviceAttached(UsbDeviceDescriptor device, List<Subscription> subscriptions);
    void deviceAttached(UsbDeviceDescriptor device, Subscription subscription);

    void deviceDetached(UsbDeviceDescriptor device);


}
