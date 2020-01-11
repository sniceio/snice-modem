package io.snice.modem.fsm;

import io.hektor.fsm.Context;
import io.snice.usb.DeviceId;
import io.snice.usb.UsbDevice;

public interface ModemManagerContext extends Context {

    /**
     * Claim a particular {@link UsbDevice} from the underlying USB sub-system.
     *
     * @param device
     */
    void claim(DeviceId device);

    /**
     * Ask to subscribe to changes of the external, from this FSMs point-of-view, external
     * USB Sub System. How that subscription is accomplished depends on the execution
     * environment.
     */
    void subscribe();

    /**
     * Reply to the sender with the given message. How that is accomplished is depending on the
     * execution environment. If the FSM is executing in an actor environment, the implementing
     * {@link ModemManagerContext} needs to keep track of the sender etc.
     *
     * @param msg the message to send back to the sender.
     */
    void reply(Object msg);



}
