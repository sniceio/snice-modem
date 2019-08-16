package io.snice.usb.linux;

import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbInterfaceDescriptor;

import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class LinuxUsbDeviceDescriptor implements UsbDeviceDescriptor {

    private final LinuxDeviceId id;
    private final String vendorId;
    private final String productId;
    private final Optional<String> vendorDescription;

    private LinuxUsbDeviceDescriptor(final LinuxDeviceId id,
                                     final String vendorId,
                                     final String productId,
                                     final Optional<String> vendorDescription) {
        this.id = id;
        this.vendorId = vendorId;
        this.productId = productId;
        this.vendorDescription = vendorDescription;
    }

    public static VendorIdStep of(final LinuxDeviceId id) {
        assertNotNull(id, "The " + LinuxDeviceId.class.getSimpleName() + " cannot be null");
        return (vendorId) -> {
            assertNotEmpty(vendorId, "The VendorId cannot be null or the empty String");
            return productId -> {
                assertNotEmpty(productId, "The ProductId cannot be null or the empty String");
                return new Builder(id, vendorId, productId);
            };

        };
    }

    public int getBusNo() {
        return id.getBusNo();
    }

    public int getDeviceNo() {
        return id.getDeviceAddress();
    }

    public String getSysfs() {
        return id.getSysfs();
    }

    @Override
    public String getVendorId() {
        return vendorId;
    }

    @Override
    public String getProductId() {
        return productId;
    }

    @Override
    public Optional<String> getVendorDescription() {
        return vendorDescription;
    }

    @Override
    public List<UsbInterfaceDescriptor> getInterfaces() {
        return List.of();
    }

    @Override
    public String toString() {
        return String.format("Bus %03d Device %03d Sysfs %8s: ID %s:%s %s ", getBusNo(), getDeviceNo(), id.getSysfs(),
                vendorId, productId, vendorDescription.orElse("Unknown Device"));
    }

    public static class Builder implements UsbDeviceDescriptor.Builder {
        private final LinuxDeviceId id;
        private final String vendorId;
        private final String productId;
        private String description;

        private Builder(final LinuxDeviceId id, final String vendorId, final String productId) {
            this.id = id;
            this.vendorId = vendorId;
            this.productId = productId;
        }

        @Override
        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        @Override
        public LinuxUsbDeviceDescriptor build() {
            final Optional<String> desc = description == null || description.strip().isEmpty() ? Optional.empty() : Optional.of(description);
            return new LinuxUsbDeviceDescriptor(id, vendorId, productId, desc);
        }
    }

}
