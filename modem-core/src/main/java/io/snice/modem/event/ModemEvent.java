package io.snice.modem.event;

import io.snice.usb.DeviceId;
import io.snice.usb.UsbDeviceDescriptor;

public interface ModemEvent {

    DeviceId getDeviceId();

    UsbDeviceDescriptor getDescriptor();

    interface Claim {}

    interface Lease {}

    interface Release {}

    interface Attach {}

    interface Detach {}
}
