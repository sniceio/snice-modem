package io.snice.usb.impl;

import io.snice.usb.UsbEndpointDescriptor;
import io.snice.usb.UsbException;
import io.snice.usb.UsbInterfaceDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotNull;

/**
 * Loads information about an USB interface, reading all information off of a
 * linux sysfs folder.
 */
public class LinuxUsbInterfaceDescriptorOld implements UsbInterfaceDescriptor {


    private final static Pattern IF_NAME_PATTERN = Pattern.compile("(\\d-\\d[\\.\\d]*):(\\d[\\.\\d]*)");

    private final Path path;
    private final String usbDevice;
    private final String usbInterface;
    private final Optional<Path> tty;

    private LinuxUsbInterfaceDescriptorOld(final Path path, final String usbDevice, final String usbInterface, final Optional<Path> tty) {
        this.path = path;
        this.usbDevice = usbDevice;
        this.usbInterface = usbInterface;
        this.tty = tty;
    }

    public static LinuxUsbInterfaceDescriptorOld of(final Path path) throws UsbException {
        assertNotNull(path, "The path to the Linux sysfs folder containing the description of a USB interface cannot be null");
        final var matcher = IF_NAME_PATTERN.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            throw new UsbException(path.getFileName() + " is not a Linux sysfs USB interface folder");
        }

        final var usbDevice = matcher.group(1);
        final var usbInterface = matcher.group(2);
        final var tty = findUsbSerialInterface(path);

        return new LinuxUsbInterfaceDescriptorOld(path, usbDevice, usbInterface, tty);
    }

    private static Optional<Path> findUsbSerialInterface(final Path path) {
        try {
            final var ttys = Files.find(path, 1, (p, attr) -> attr.isDirectory() && p.getFileName().toString().startsWith("ttyUSB")).collect(Collectors.toList());
            if (ttys.isEmpty()) {
                return Optional.empty();
            }
            if (ttys.size() > 1) {
                throw new UsbException("Can a Linux USB interface have many ttyUSB* serial ports defined?");
            }

            return Optional.of(ttys.get(0));
        } catch (final IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public String getUsbDevice() {
        return usbDevice;
    }

    public String getUsbInterface() {
        return usbInterface;
    }

    public Path getInterfacePath() {
        return path;
    }

    public Optional<Path> getTty() {
        return tty;
    }

    @Override
    public String toString() {
        // TODO: clean up, dont build a manual json blob, if that's even what you want for toString...
        return "UsbInterface { device : " + usbDevice + ", interface : " + usbInterface + ", tty : " + tty.map(Path::toString).orElse("") + " }";
    }

    @Override
    public List<UsbEndpointDescriptor> getEndpoints() {
        return null;
    }

    @Override
    public Optional<Path> getSerialDevicePath() {
        return Optional.empty();
    }
}
