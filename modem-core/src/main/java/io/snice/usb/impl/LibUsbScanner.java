package io.snice.usb.impl;

import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbScanner;

import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbPort;
import javax.usb.UsbServices;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class LibUsbScanner implements UsbScanner {

    private final UsbConfiguration config;
    private final UsbServices services;

    public static LibUsbScanner of(final UsbServices services, final UsbConfiguration config) {
        assertNotNull(config, "The configuration cannot be null");
        assertNotNull(services, "The USB services cannot be null");
        return new LibUsbScanner(services, config);
    }

    private LibUsbScanner(final UsbServices services, final UsbConfiguration config) {
        this.services = services;
        this.config = config;
    }

    @Override
    public List<UsbDevice> scan() throws IOException, ExecutionException, InterruptedException {
        try {
            final var root = services.getRootUsbHub();
            dumpDevice(root);
            return List.of();
        } catch (final UsbException e) {
            e.printStackTrace();
        }

        return List.of();
    }

    @Override
    public Optional<UsbDevice> find(final UsbDeviceDescriptor descriptor) {
        throw new RuntimeException("Not yet implemented");
    }

    /**
     * Dumps the specified USB device to stdout.
     *
     * @param device
     *            The USB device to dump.
     */
    public static void dumpDevice(final javax.usb.UsbDevice device) {

        final short sierra = 1199;
        // System.err.println((device.getUsbDeviceDescriptor().idVendor()));

        final short vendorId = device.getUsbDeviceDescriptor().idVendor();
        final short productId = device.getUsbDeviceDescriptor().idProduct();
        final String vendorIdStr = String.format("%04x", vendorId & '\uffff');
        final String productIdStr = String.format("%04x", productId & '\uffff');

        // if ("1199".equals(vendorIdStr) || "05c6".equals(vendorIdStr) || "2c7c".equals(vendorIdStr)) {
        if (true || "1199".equals(vendorIdStr)) {
            System.out.println("The device: " + device);
            final UsbPort port = device.getParentUsbPort();
            if (port != null) {
                System.out.println("Connected to port: " + Byte.toUnsignedInt(port.getPortNumber()));
            }

            // Dump device descriptor
            System.out.println(device.getUsbDeviceDescriptor());

            // Process all configurations
            for (final javax.usb.UsbConfiguration configuration : (List<javax.usb.UsbConfiguration>) device.getUsbConfigurations()) {
                // Dump configuration descriptor
                System.out.println(configuration.getUsbConfigurationDescriptor());

                // Process all interfaces
                for (final UsbInterface iface : (List<UsbInterface>) configuration.getUsbInterfaces()) {
                    // iface.
                    // Dump the interface descriptor
                    // System.out.println(iface.getUsbInterfaceDescriptor());

                    // Process all endpoints
                    for (final UsbEndpoint endpoint : (List<UsbEndpoint>) iface.getUsbEndpoints()) {
                        // Dump the endpoint descriptor
                        // System.out.println(endpoint.getUsbEndpointDescriptor());
                    }
                }
            }
        }

        // Dump child devices if device is a hub
        if (device.isUsbHub()) {
            final javax.usb.UsbHub hub = (javax.usb.UsbHub) device;
            for (final javax.usb.UsbDevice child: (List<javax.usb.UsbDevice>) hub.getAttachedUsbDevices()) {
                dumpDevice(child);
            }
        }
    }


}
