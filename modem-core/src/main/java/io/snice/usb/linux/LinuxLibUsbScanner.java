package io.snice.usb.linux;

import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbException;
import io.snice.usb.UsbScanner;
import io.snice.usb.VendorDescriptor;
import io.snice.usb.VendorDeviceDescriptor;
import io.snice.usb.impl.LinuxUsbDeviceAttachEvent;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class LinuxLibUsbScanner implements UsbScanner {

    private final static String DEFAULT_USB_SERIAL_FS = "/sys/bus/usb-serial/devices";

    private final static String DEFAULT_DMESG = "/sys/bus/usb-serial/devices";

    private final static Pattern DMESG_SYSFS_PATTERN = Pattern.compile("\\[.*\\] usb (\\d-\\d[\\.\\d]*): .*");

    // TODO: needs to be configured.
    private static final String USB_DEVICE_CONNECTED_REGEXP = "\\[.*\\] usb (\\d-\\d[\\.\\d]*): *New USB device found, idVendor=([a-f,A-F,0-9]+), idProduct=([a-f,A-F,0-9]+).*";

    private static final String USB_DEVICE_DISCONNECTED_REGEXP = "\\[.*\\] usb (\\d-\\d[\\.\\d]*):.*device number.*(\\d+).*";

    private final LibUsbConfiguration config;

    private final Map<String, VendorDescriptor> knownUsbVendors;

    private final Context libUsbContext;

    public static LinuxLibUsbScanner of(final LibUsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors) {
        assertNotNull(config, "The configuration cannot be null");
        final Context context = new Context();
        final int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS) {
            throw new UsbException("Unable to initialize libusb. Error code " + result);
        }

        return new LinuxLibUsbScanner(config, knownUsbVendors == null ? Collections.emptyMap() : knownUsbVendors, context);
    }

    public static LinuxLibUsbScanner of(final LibUsbConfiguration config) {
        return of(config, null);
    }

    private LinuxLibUsbScanner(final LibUsbConfiguration config,
                               final Map<String, VendorDescriptor> knownUsbVendors,
                               final Context context) {
        this.config = config;
        this.knownUsbVendors = knownUsbVendors;
        this.libUsbContext = context;
    }

    @Override
    public List<UsbDevice> find(final String vendorId, final String productId) throws UsbException {
        return null;
    }

    @Override
    public List<UsbDevice> scan() throws UsbException {
        final DeviceList list = new DeviceList();
        final int result = LibUsb.getDeviceList(libUsbContext, list);
        if (result < 0) {
            throw new UsbException("Unable to get USB device list. Error code " + result);
        }

        try {
            for (final Device device: list) {
                final DeviceDescriptor desc = new DeviceDescriptor();
                final var descResult = LibUsb.getDeviceDescriptor(device, desc);
                if (descResult != LibUsb.SUCCESS) {
                    throw new UsbException("Unable to read device descriptor. Error code: " + result);
                }

                final var vendorId = String.format("%04x", desc.idVendor() & '\uffff');
                final var productId = String.format("%04x", desc.idProduct() & '\uffff');

                final var portNo = LibUsb.getPortNumber(device);
                final var busNo = LibUsb.getBusNumber(device);
                final var devAddress = LibUsb.getDeviceAddress(device);

                final var b = ByteBuffer.allocateDirect(8);
                final var count = LibUsb.getPortNumbers(device, b);
                final List<Integer> ports = new ArrayList<>();
                for (int i = 0; i < count; ++i) {
                    ports.add((int)b.get(i));
                }

                final var sysfs = busNo + "-" + ports.stream().map(String::valueOf).collect(Collectors.joining("."));
                final var attachEvent = LinuxUsbDeviceAttachEvent.of(vendorId).withProductId(productId).withSysfs(sysfs);
                final var vendor = Optional.ofNullable(knownUsbVendors.get(vendorId));
                final var knownDevice = vendor.map(v -> v.getDevices().get(productId));
                System.err.println(attachEvent + " " + vendor.map(VendorDescriptor::getName).orElse("Unknown Vendor")
                        + " " + knownDevice.map(VendorDeviceDescriptor::getDescription).orElse(""));
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true);
        }
        return null;
    }

    @Override
    public Optional<UsbDevice> find(final UsbDeviceDescriptor descriptor) throws UsbException {
        return Optional.empty();
    }

    private boolean isRootHub(final UsbDeviceDescriptor descriptor) {
        return isRootHub(descriptor.getVendorId(), descriptor.getProductId());
    }

    private boolean isRootHub(final String vendorId, final String productId) {
        final var device = lookup(vendorId, productId);
        return device.map(VendorDeviceDescriptor::getDescription).orElse("").toLowerCase().contains("root hub");
    }

    private Optional<VendorDeviceDescriptor> lookup(final String vendorId, final String productId) {
        final var vendor = knownUsbVendors.get(vendorId);
        if (vendor == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(vendor.getDevices().get(productId));
    }
}
