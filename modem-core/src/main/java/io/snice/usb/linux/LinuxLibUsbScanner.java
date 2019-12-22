package io.snice.usb.linux;

import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbException;
import io.snice.usb.UsbInterfaceDescriptor;
import io.snice.usb.UsbScanner;
import io.snice.usb.VendorDescriptor;
import io.snice.usb.VendorDeviceDescriptor;
import io.snice.usb.impl.LinuxUsbInterfaceDescriptor;
import org.usb4java.ConfigDescriptor;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
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
    public List<UsbDeviceDescriptor> scan() throws UsbException {
        return scan((vendorId, productId) -> true);
    }

    @Override
    public List<UsbDeviceDescriptor> scan(final Predicate<String> vendorFilter) throws UsbException {
        assertNotNull(vendorFilter, "The Vendor ID filter cannot be null");
        return scan((vendorId, productId) -> vendorFilter.test(vendorId));
    }

    @Override
    public List<UsbDeviceDescriptor> scan(final BiPredicate<String, String> vendorProductFilter) throws UsbException {
        assertNotNull(vendorProductFilter, "The Vendor and Product ID filter cannot be null");
        final DeviceList list = new DeviceList();
        final int result = LibUsb.getDeviceList(libUsbContext, list);
        if (result < 0) {
            throw new UsbException("Unable to get USB device list. Error code " + result);
        }

        final var deviceList = new ArrayList<UsbDeviceDescriptor>();

        try {
            for (final Device device: list) {
                final DeviceDescriptor desc = new DeviceDescriptor();
                final var descResult = LibUsb.getDeviceDescriptor(device, desc);
                if (descResult != LibUsb.SUCCESS) {
                    throw new UsbException("Unable to read device descriptor. Error code: " + result);
                }

                final var vendorId = String.format("%04x", desc.idVendor() & '\uffff');
                final var productId = String.format("%04x", desc.idProduct() & '\uffff');

                if (vendorProductFilter.test(vendorId, productId)) {
                    deviceList.add(createDescriptor(device, vendorId, productId));
                }
            }
        } finally {
            // Ensure the allocated device list is freed
            LibUsb.freeDeviceList(list, true);
        }
        return Collections.unmodifiableList(deviceList);
    }

    private UsbDeviceDescriptor createDescriptor(final Device device, final String vendorId, final String productId) {
        final var portNo = LibUsb.getPortNumber(device);
        final var busNo = LibUsb.getBusNumber(device);
        final var devAddress = LibUsb.getDeviceAddress(device);
        final var isRootHub = isRootHub(vendorId, productId);


        final var usbsysfs = constructUsbfs(device, busNo);
        final var id = LinuxDeviceId.withUsbSysfs(usbsysfs).withBusNo(busNo).withDeviceAddress(devAddress).isRootHub(isRootHub);
        final var interfaceDescriptors = createInterfaceDescriptors(device, id);

        final var vendor = Optional.ofNullable(knownUsbVendors.get(vendorId));
        final var knownDevice = vendor.map(v -> v.getDevices().get(productId));
        final var description = vendor.map(VendorDescriptor::getName).orElse("")
                + " "
                + knownDevice.map(VendorDeviceDescriptor::getDescription).orElse("");

        final var dev = LinuxUsbDeviceDescriptor.of(id)
                .withVendorId(vendorId)
                .withProductId(productId)
                .withDescription(description)
                .withUsbInterfaces(interfaceDescriptors)
                .build();

        return dev;
    }

    /**
     * Construct the Linux USB sysfs device name for this device. There is a logic to this, which is:
     *
     * <pre>
     * root_hub-hub_port:config.interface
     * </pre>
     *
     * Source: Linux Device Drivers, 3rd Edition. Chapter 13. USB Drivers.
     * You may see an excerpt here: https://www.oreilly.com/library/view/linux-device-drivers/0596005903/ch13.html
     * (was available on August 17th, 2019)
     *
     * @param device
     * @param busNo
     * @return
     */
    private String constructUsbfs(final Device device, final int busNo) {
        final var b = ByteBuffer.allocateDirect(8);
        final var count = LibUsb.getPortNumbers(device, b);
        if (count > 0) {
            final List<Integer> ports = new ArrayList<>();
            for (int i = 0; i < count; ++i) {
                ports.add((int) b.get(i));
            }

            return busNo + "-" + ports.stream().map(String::valueOf).collect(Collectors.joining("."));
        }
        return "usb" + busNo;
    }

    private List<UsbInterfaceDescriptor> createInterfaceDescriptors(final Device device, final LinuxDeviceId id) {
        final List<UsbInterfaceDescriptor> interfaces = new ArrayList<>();

        final var usbfsRoot = config.getUsbSysfsRoot();
        final var deviceUsbfsRoot = usbfsRoot.resolve(id.getSysfs());

        final var configDescriptor = new ConfigDescriptor();
        LibUsb.getActiveConfigDescriptor(device, configDescriptor);
        final int configNumber = getInt(configDescriptor.bConfigurationValue());

        final var ifaces = configDescriptor.iface();
        for (int i = 0; i < getInt(configDescriptor.bNumInterfaces()); ++i) {

            final var ifBuilder = LinuxUsbInterfaceDescriptor.of(deviceUsbfsRoot);

            final var iface = ifaces[i];
            final var altSettings = iface.altsetting();
            for (int j = 0; j < iface.numAltsetting(); ++j) {
                final var ifaceDesc = altSettings[j];
                final int number = getInt(ifaceDesc.bInterfaceNumber());
                final var usbfsInterface = id.getSysfs() + ":" + configNumber + "." + number;
                final var sysfsInterfacePath = deviceUsbfsRoot.resolve(usbfsInterface);
                final Optional<Path> tty = id.isRootHub() ? Optional.empty() : findUsbSerialInterface(sysfsInterfacePath);

                ifBuilder.withAlternateSetting(number, usbfsInterface, tty);

                final var endpoints = ifaceDesc.endpoint();
                for (int k = 0; k < getInt(ifaceDesc.bNumEndpoints()); ++k) {
                    ifBuilder.withEndpoint(LinuxUsbEndpoint.from(endpoints[k]));
                }

            }
            interfaces.add(ifBuilder.build());
        }

        return List.copyOf(interfaces);
    }

    private static int getInt(final byte b) {
        return b & 0xff;
    }

    @Override
    public Optional<UsbDevice> find(final UsbDeviceDescriptor descriptor) throws UsbException {
        try {
            final var linuxDescriptor = (LinuxUsbDeviceDescriptor) descriptor;
            final var deviceSysfs = config.getUsbSysfsRoot().resolve(linuxDescriptor.getSysfs());
            // TODO: check so path is good and readable...

            return Optional.empty();
        } catch (final ClassCastException e) {
            throw new IllegalArgumentException("This is the Linux scanner, you must supply a "
                    + UsbDeviceDescriptor.class.getSimpleName() + " of subtype "
                    + LinuxUsbDeviceDescriptor.class.getSimpleName());
        }
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
