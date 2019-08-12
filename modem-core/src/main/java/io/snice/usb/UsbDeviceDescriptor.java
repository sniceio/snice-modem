package io.snice.usb;

import io.snice.usb.impl.LinuxUsbDeviceDescriptor;

import java.util.List;
import java.util.Optional;

public interface UsbDeviceDescriptor {

    static ProductIdStep ofVendorId(final String vendorId) {
        return LinuxUsbDeviceDescriptor.ofVendorId(vendorId);
    }

    String getVendorId();
    String getProductId();

    /**
     * A human friendly description of the vendor.
     *
     * This description is only meant for human consumption and has
     * not bearing, or use, to the running logic.
     */
    Optional<String> getVendorDescription();

    List<UsbInterfaceDescriptor> getInterfaces();

    interface ProductIdStep {
        Builder withProductId(String productId);
    }

    interface Builder {
        Builder withVendorDescription(String description);

        UsbDeviceDescriptor build();
    }

}
