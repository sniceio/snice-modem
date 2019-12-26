package io.snice.usb;

import java.util.List;
import java.util.Optional;

/**
 * Represents a USB device. There is of course the javax.usb but it is very old style
 * interface and I wanted to hide it's ugliness.
 */
public interface UsbDevice {

    UsbDeviceDescriptor getDeviceDescriptor();

    String getVendorId();
    String getProductId();

    DeviceId getId();

    /**
     * A human friendly description of the vendor.
     *
     * This description is only meant for human consumption and has
     * no bearing, or use, to the running logic.
     */
    Optional<String> getVendorDescription();

    List<UsbInterface> getInterfaces();

}
