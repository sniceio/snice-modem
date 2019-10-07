package io.snice.usb.impl;

import io.snice.usb.DeviceId;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbInterfaceDescriptor;

import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

public class LinuxUsbDeviceDescriptorOld implements UsbDeviceDescriptor {
    private final String busNo;
    private final String deviceNo;
    private final String vendorId;
    private final String productId;
    private final Optional<String> vendorDescription;
    private final List<LinuxUsbInterfaceDescriptorOld> ifs;

    private LinuxUsbDeviceDescriptorOld(final String busNo,
                                        final String deviceNo,
                                        final String vendorId,
                                        final String productId,
                                        final Optional<String> vendorDescription,
                                        final List<LinuxUsbInterfaceDescriptorOld> ifs) {
        this.busNo = busNo;
        this.deviceNo = deviceNo;
        this.vendorId = vendorId;
        this.productId = productId;
        this.vendorDescription = vendorDescription;
        this.ifs = ifs;
    }

    public static UsbDeviceDescriptor.ProductIdStep ofVendorId(final String vendorId) {
        assertNotEmpty(vendorId, "The VendorId cannot be null or the empty String");
        return productId -> {
            assertNotEmpty(productId, "The ProductId cannot be null or the empty String");
            return new Builder(vendorId, productId);
        };

    }

    public int getBusNo() {
        return Integer.parseInt(busNo);
    }

    public int getDeviceNo() {
        return Integer.parseInt(deviceNo);
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
        return null;
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
        return String.format("USB Device %s:%s %s. Interface count %d", vendorId, productId, vendorDescription.orElse("N/A"), ifs.size());
    }

    public LinuxBuilder copy() {
        final var builder = new Builder(vendorId, productId);
        builder.withDescription(vendorDescription.orElse(null));
        builder.withBusNo(busNo);
        builder.withDeviceNo(deviceNo);
        // builder.withUsbInterfaces(ifs);
        return builder;
    }

    interface LinuxBuilder extends UsbDeviceDescriptor.Builder {
        LinuxBuilder withBusNo(final String busNo);
        LinuxBuilder withDeviceNo(final String deviceNo);
    }

    public static class Builder implements LinuxBuilder {
        private final String vendorId;
        private final String productId;
        private String description;
        private String busNo;
        private String deviceNo;
        private List<LinuxUsbInterfaceDescriptorOld> ifs;

        private Builder(final String vendorId, final String productId) {
            this.vendorId = vendorId;
            this.productId = productId;
        }

        @Override
        public LinuxBuilder withDescription(final String description) {
            this.description = description;
            return this;
        }

        @Override
        public LinuxBuilder withBusNo(final String busNo) {
            assertNotEmpty(busNo, "The Linux Bus No cannot be null or the empty string");
            this.busNo = busNo;
            return this;
        }

        @Override
        public LinuxBuilder withDeviceNo(final String deviceNo) {
            assertNotEmpty(busNo, "The Linux Device No cannot be null or the empty string");
            this.deviceNo = deviceNo;
            return this;
        }

        @Override
        public Builder withUsbInterfaces(final List<UsbInterfaceDescriptor> ifs) {
            // this.ifs = ifs == null ? List.of() : List.copyOf(ifs);
            return this;
        }

        @Override
        public LinuxUsbDeviceDescriptorOld build() {
            final Optional<String> desc = description == null || description.trim().isEmpty() ? Optional.empty() : Optional.of(description);
            return new LinuxUsbDeviceDescriptorOld(busNo, deviceNo, vendorId, productId, desc, ifs);
        }
    }

}
