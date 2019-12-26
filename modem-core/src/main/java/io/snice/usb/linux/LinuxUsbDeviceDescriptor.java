package io.snice.usb.linux;

import io.snice.usb.DeviceId;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbInterfaceDescriptor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class LinuxUsbDeviceDescriptor implements UsbDeviceDescriptor {

    private final LinuxDeviceId id;
    private final String vendorId;
    private final String productId;
    private final Optional<String> vendorDescription;
    private final List<UsbInterfaceDescriptor> ifs;

    private LinuxUsbDeviceDescriptor(final LinuxDeviceId id,
                                     final String vendorId,
                                     final String productId,
                                     final Optional<String> vendorDescription,
                                     final List<UsbInterfaceDescriptor> ifs) {
        this.id = id;
        this.vendorId = vendorId;
        this.productId = productId;
        this.vendorDescription = vendorDescription;
        this.ifs = ifs;
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
    public DeviceId getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final LinuxUsbDeviceDescriptor that = (LinuxUsbDeviceDescriptor) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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
        private List<UsbInterfaceDescriptor> ifs;

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
        public UsbDeviceDescriptor.Builder withUsbInterfaces(final List<UsbInterfaceDescriptor> ifs) {
            this.ifs = ifs;
            return this;
        }

        @Override
        public LinuxUsbDeviceDescriptor build() {
            final Optional<String> desc = description == null || description.strip().isEmpty() ? Optional.empty() : Optional.of(description);
            return new LinuxUsbDeviceDescriptor(id, vendorId, productId, desc, List.copyOf(ifs));
        }
    }

}
