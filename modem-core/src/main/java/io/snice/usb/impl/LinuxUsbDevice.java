package io.snice.usb.impl;

import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbException;
import io.snice.usb.UsbInterface;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

public class LinuxUsbDevice implements UsbDevice {

    private final LinuxUsbDeviceDescriptorOld descriptor;
    private final String sysfs;

    public static Builder of(final LinuxUsbDeviceDescriptorOld descriptor) {
        assertNotNull(descriptor, "The USB device descriptor cannot be null");
        return new Builder(descriptor);
    }

    private LinuxUsbDevice(final String sysfs, final LinuxUsbDeviceDescriptorOld descriptor) {
        this.sysfs = sysfs;
        this.descriptor = descriptor;
    }

    public int getBusNo() {
        return descriptor.getBusNo();
    }

    public int getDeviceNo() {
        return descriptor.getDeviceNo();
    }

    @Override
    public UsbDeviceDescriptor getDeviceDescriptor() {
        return descriptor;
    }

    @Override
    public String getVendorId() {
        return descriptor.getVendorId();
    }

    @Override
    public String getProductId() {
        return descriptor.getProductId();
    }

    @Override
    public Optional<String> getVendorDescription() {
        return descriptor.getVendorDescription();
    }

    @Override
    public List<UsbInterface> getInterfaces() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("USB Device %s:%s %s (sysfs: %s).", getVendorId(), getProductId(), getVendorDescription().orElse(""), sysfs);
    }

    public static class Builder {

        private final LinuxUsbDeviceDescriptorOld descriptor;
        private String devicePath;
        private Path sysfsDevicesPath;

        private Builder(final LinuxUsbDeviceDescriptorOld descriptor) {
            this.descriptor = descriptor;
        }

        public Builder withDevicePath(final String path) {
            this.devicePath = path;
            return this;
        }

        public Builder withLinuxSysfsDevicesPath(final Path path) {
            this.sysfsDevicesPath = path;
            return this;
        }

        public LinuxUsbDevice build() {
            final var sysfs = ensureDevicePath(sysfsDevicesPath, devicePath);
            // final var ifs = buildInterfaces(descriptor, sysfs, devicePath);
            // final var enhancedDescriptor = (LinuxUsbDeviceDescriptorOld)descriptor.copy().withUsbInterfaces(ifs).build();
            final var enhancedDescriptor = (LinuxUsbDeviceDescriptorOld)descriptor.copy().build();
            return new LinuxUsbDevice(sysfs.getFileName().toString(), enhancedDescriptor);
        }

        private static Path ensureDevicePath(final Path sysfsPath, final String devicePath) {
            final var sysfs = assertNotNull(sysfsPath, "The Linux sysfs devices path must be specified");
            final var device = assertNotEmpty(devicePath, "The device sysfs path cannot be null or the empty string");
            final var path = sysfs.resolve(device);

            // TODO: check so that we have read permissions etc...
            return path;
        }

        public static List<LinuxUsbInterfaceDescriptorOld> buildInterfaces(final LinuxUsbDeviceDescriptorOld descriptor, final Path sysfs, final String device) throws UsbException {
            try {
                return Files.list(sysfs)
                        .map(Path::toFile)
                        .filter(File::isDirectory)
                        .map(File::toPath)
                        .filter(f -> f.getFileName().toString().startsWith(device + ":"))
                        .map(LinuxUsbInterfaceDescriptorOld::of)
                        .collect(Collectors.toList());
            } catch (final IOException e) {
                throw new UsbException("Unable to scan the file system", e);
            }
        }



    }
}