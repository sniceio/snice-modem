package io.snice.usb.event;

import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;

import java.util.Optional;

public interface UsbEvent {

    /**
     * Not all events have a fully configured {@link UsbDevice} created/configured just yet.
     * This is true for the {@link UsbAttachEvent}, which just contains some meta data
     * (so a {@link UsbDeviceDescriptor )} about the device that just was attached.
     *
     * On the other hand, when a device detaches, we do have the full device configured
     * so in that case, you will get the actual {@link UsbDevice} as part of the event.
     *
     * @return
     */
    Optional<UsbDevice> getDevice();

    UsbDeviceDescriptor getUsbDeviceDescriptor();

    default boolean isDeviceAttach() {
        return false;
    }

    default boolean isDeviceDetach() {
        return false;
    }

    interface UsbAttachEvent extends UsbEvent {
        default boolean isDeviceAttach() {
            return true;
        }
    }

    interface UsbDetachEvent extends UsbEvent {

        default boolean isDeviceDetach() {
            return true;
        }
    }

}
