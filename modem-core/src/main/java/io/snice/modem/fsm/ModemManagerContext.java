package io.snice.modem.fsm;

import io.hektor.fsm.Context;
import io.snice.usb.DeviceId;
import io.snice.usb.UsbDevice;

public interface ModemManagerContext extends Context {

    /**
     * Claim a particular {@link UsbDevice}.
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

}
