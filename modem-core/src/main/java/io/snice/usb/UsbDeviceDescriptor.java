package io.snice.usb;

import java.util.List;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

public interface UsbDeviceDescriptor {

    static Builder ofVendorId(final String vendorId) {
        assertNotEmpty(vendorId, "The VendorId cannot be null or the empty String");
    }

    String getVendorId();
    String getDeviceId();

    List<UsbInterfaceDescriptor> getInterfaces();

    class Builder {

    }
}
