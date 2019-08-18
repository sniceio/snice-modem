package io.snice.usb;

import io.snice.usb.impl.LinuxUsbDeviceDescriptorOld;

import java.util.List;
import java.util.Optional;

public interface UsbDeviceDescriptor {

    static ProductIdStep ofVendorId(final String vendorId) {
        return LinuxUsbDeviceDescriptorOld.ofVendorId(vendorId);
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

    interface VendorIdStep {
        ProductIdStep withVendorId(String vendorId);
    }

    interface ProductIdStep {
        Builder withProductId(String productId);
    }

    interface Builder {
        Builder withDescription(String description);
        Builder withUsbInterfaces(List<UsbInterfaceDescriptor> ifs);
        UsbDeviceDescriptor build();
    }

}
