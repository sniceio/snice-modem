package io.snice.usb.impl;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

public class LinuxUsbDeviceAttachEvent implements LinuxUsbDeviceEvent {
    private final String vendorId;
    private final String productId;
    private final String sysfs;

    public static ProductIdStep of(final String vendorId) {
        assertNotEmpty(vendorId, "The vendor ID cannot be null or the empty String");
        return (productId) -> {
            assertNotEmpty(productId, "The product ID cannot be null or the empty String");
            return (sysfs) -> {
                assertNotEmpty(sysfs, "The sysfs cannot be null or the empty String");
                return new LinuxUsbDeviceAttachEvent(vendorId, productId, sysfs);
            };
        };
    }

    public interface ProductIdStep {
        SysfsStep withProductId(String productId);
    }


    public interface SysfsStep {
        LinuxUsbDeviceAttachEvent withSysfs(String sysfs);
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getProductId() {
        return productId;
    }

    @Override
    public String getSysfs() {
        return sysfs;
    }

    @Override
    public String toString() {
        return LinuxUsbDeviceAttachEvent.class.getSimpleName() + " " + vendorId + ":" + productId
                + " (" + sysfs + ")";
    }

    private LinuxUsbDeviceAttachEvent(final String vendorId, final String productId, final String sysfs) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.sysfs = sysfs;
    }
}
