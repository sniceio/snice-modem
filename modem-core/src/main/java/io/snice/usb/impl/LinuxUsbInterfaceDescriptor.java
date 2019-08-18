package io.snice.usb.impl;

import io.snice.preconditions.PreConditions;
import io.snice.usb.UsbEndpointDescriptor;
import io.snice.usb.UsbInterfaceDescriptor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LinuxUsbInterfaceDescriptor implements UsbInterfaceDescriptor {

    private final Path sysfs;
    private final List<UsbEndpointDescriptor> endpoints;
    private final List<AlternativeSettings> altSettings;


    private LinuxUsbInterfaceDescriptor(final Path sysfs, final List<UsbEndpointDescriptor> endpoints, final List<AlternativeSettings> altSettings) {
        this.sysfs = sysfs;
        this.endpoints = endpoints;
        this.altSettings = altSettings;
    }

    public static final Builder of(final Path sysfs) {
        PreConditions.assertNotNull(sysfs, "The Linux sysfs path to the USB interface cannot be null");
        return new Builder(sysfs);
    }

    @Override
    public List<UsbEndpointDescriptor> getEndpoints() {
        return endpoints;
    }

    @Override
    public Optional<Path> getSerialDevicePath() {
        return altSettings.stream().map(s -> s.tty.orElse(null)).filter(p -> p != null).findFirst();
    }

    public static class Builder {

        private final Path sysfs;
        private final List<AlternativeSettings> altSettings = new ArrayList<>();
        private final List<UsbEndpointDescriptor> endpoints = new ArrayList<>();

        public Builder withAlternateSetting(final int altSettings, final String usbfs, final Optional<Path> tty) {
            this.altSettings.add(new AlternativeSettings(altSettings, usbfs, tty));
            return this;
        }

        public Builder withEndpoint(final UsbEndpointDescriptor endpoint) {
            endpoints.add(endpoint);
            return this;
        }

        private Builder(final Path sysfs) {
            this.sysfs = sysfs;
        }

        public LinuxUsbInterfaceDescriptor build() {
            return null;
        }

    }

    private static class AlternativeSettings {
        private final int altSettingsNo;
        private final String usbfs;
        private final Optional<Path> tty;

        private AlternativeSettings(final int altSettingsNo, final String usbfs, final Optional<Path> tty) {
            this.altSettingsNo = altSettingsNo;
            this.usbfs = usbfs;
            this.tty = tty;
        }
    }
}
