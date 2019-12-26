package io.snice.usb.fsm.support;

import io.snice.usb.event.Subscribe;
import io.snice.usb.fsm.UsbManagerFsm;

/**
 * The {@link UsbManagerFsm}, and FSMs in general, are not aware of the environment
 * they are executing within. So, when the {@link UsbManagerFsm} receives a request
 * to {@link Subscribe} to changes from an external entity, it doesn't know who
 * the remote party is, nor how to "tell" it when there are changes. This is dependent
 * on whether we e.g. are executing an an Actor environment, or perhaps in an HTTP container
 * and as such, the environment must provide the implementing {@link Subscription}.
 */
public interface Subscription {

    /**
     * Get the original {@link Subscribe} request.
     */
    Subscribe getSubscribeRequest();
}
