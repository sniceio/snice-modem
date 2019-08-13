package io.snice.usb.impl;

import io.hektor.actors.LoggingSupport;
import io.snice.processes.Processes;
import io.snice.usb.NoSuchUsbDevice;
import io.snice.usb.UsbAlertCode;
import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbException;
import io.snice.usb.UsbScanner;
import io.snice.usb.VendorDescriptor;
import io.snice.usb.VendorDeviceDescriptor;
import io.snice.usb.impl.LinuxUsbDeviceDescriptor.LinuxBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class LinuxUsbScanner implements UsbScanner, LoggingSupport {

    private static final Logger logger = LoggerFactory.getLogger(LinuxUsbScanner.class);

    private final static String DEFAULT_USB_SERIAL_FS = "/sys/bus/usb-serial/devices";

    private final static String DEFAULT_DMESG = "/sys/bus/usb-serial/devices";

    private final static Pattern DMESG_SYSFS_PATTERN = Pattern.compile("\\[.*\\] usb (\\d-\\d[\\.\\d]*): .*");

    // TODO: needs to be configured.
    private static final String USB_DEVICE_CONNECTED_REGEXP = "\\[.*\\] usb (\\d-\\d[\\.\\d]*): *New USB device found, idVendor=([a-f,A-F,0-9]+), idProduct=([a-f,A-F,0-9]+).*";

    // TODO: needs to be configured
    private static final String LSUSB_CMD = "lsusb -d %s:%s";

    private final Pattern dmesgNewDevicePattern;

    private final UsbConfiguration config;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    private final Map<String, VendorDescriptor> knownUsbVendors;

    public static LinuxUsbScanner of(final UsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors) {
        assertNotNull(config, "The configuration cannot be null");

        // TODO: get from config file
        final var pattern = Pattern.compile(USB_DEVICE_CONNECTED_REGEXP);
        return new LinuxUsbScanner(config, knownUsbVendors == null ? Collections.emptyMap() : knownUsbVendors, pattern);
    }

    public static LinuxUsbScanner of(final UsbConfiguration config) {
        return of(config, null);
    }

    private LinuxUsbScanner(final UsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors, final Pattern dmesgNewDevicePattern) {
        this.config = config;
        this.knownUsbVendors = knownUsbVendors;
        this.dmesgNewDevicePattern = dmesgNewDevicePattern;
    }

    /**
     * Unique to Linux. The {@link LinuxUsbDmesgMonitor} is "tailing" dmesg and whenever it detects a
     * "New USB device connected" message, it'll call this method to setup the new {@link LinuxUsbDevice}
     *
     * @param dmesg
     * @return
     */
    public List<LinuxUsbDevice> processDmesgNewDevice(final String dmesg) {
        final var matcher = dmesgNewDevicePattern.matcher(dmesg);
        if (!matcher.matches() || matcher.groupCount() != 3) {
            // TODO: this should throw exception
            logWarn(UsbAlertCode.UNABLE_TO_PARSE_DMESG_NEW_USB_DEVICE, dmesg, dmesgNewDevicePattern.pattern());
            return List.of();
        }

        final var sysfs = matcher.group(1);
        final var vendorId = matcher.group(2);
        final var productId = matcher.group(3);

        if (isRootHub(vendorId, productId)) {
            System.err.println("Skipping root hub " + dmesg);
            return List.of();
        }

        try {
            final var cmd = String.format(LSUSB_CMD, vendorId, productId);
            final var usbDevices = Processes.execute(cmd).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS).stream()
                    .map(LinuxUsbScanner::mapToDeviceDescriptor)
                    .map(desc -> LinuxUsbDevice.of(desc).withDevicePath(sysfs).withLinuxSysfsDevicesPath(config.getDevicesFolder()).build())
                    .collect(Collectors.toList());
            System.out.println(usbDevices);
            return usbDevices;
        } catch (final TimeoutException e) {
            e.printStackTrace();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return List.of();

    }

    @Override
    public List<UsbDevice> find(final String vendorId, final String productId) throws UsbException {
        final var device = find(LinuxUsbDeviceDescriptor.ofVendorId(vendorId).withProductId(productId).build());
        return device.isPresent() ? List.of(device.get()) : List.of();
    }

    @Override
    public Optional<UsbDevice> find(final UsbDeviceDescriptor descriptor) throws UsbException {

        final var linuxDescriptor = (LinuxUsbDeviceDescriptor)descriptor;

        if (isRootHub(descriptor)) {
            return Optional.empty();
        }

        final var cmd = String.format(config.getFindSysfsRoot(), descriptor.getVendorId(), descriptor.getProductId());
        final var dmesg = executeShellCommandBlah("/bin/sh", "-c", cmd);

        // TODO: no need to throw since we are returning an optional
        final var s = dmesg.orElseThrow(() -> NoSuchUsbDevice.of(descriptor));
        final var matcher = DMESG_SYSFS_PATTERN.matcher(s);
        if (!matcher.matches() || matcher.groupCount() != 1) {
            System.err.println("No match for " + dmesg);
            return Optional.empty();
        }

        final var sysfs = matcher.group(1);
        final var dev =  LinuxUsbDevice.of(linuxDescriptor)
                .withDevicePath(sysfs)
                .withLinuxSysfsDevicesPath(config.getDevicesFolder())
                .build();
        return Optional.of(dev);
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


    public static Optional<String> executeShellCommandBlah(final String... cmd) {
        final var output = executeShellCommand(cmd);
        return output.isEmpty() ? Optional.empty() : Optional.of(output.get(0));
    }

    public static List<String> executeShellCommand(final String... cmd) {
        try {
            final var builder = new ProcessBuilder();
            builder.command(cmd);
            final var process = builder.start();
            final List<String> output = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().collect(Collectors.toList());
            final List<String> error = new BufferedReader(new InputStreamReader(process.getErrorStream())).lines().collect(Collectors.toList());

            process.onExit().get();
            if (process.exitValue() != 0) {
                System.err.println("[ERROR] " + error);
                if (error.isEmpty()) {
                    return List.of();
                }
            }

            return output;

        } catch (final Exception e) {
            e.printStackTrace();;
        }

        return List.of();

    }

    /**
     * Scan on Linux works as follows:
     * <ol>
     *     <li>Use lsusb to get all attached usb devices</li>
     *     <li>For each line from lsusb, parse out the vendor and product ID</li>
     *     <li>Grep in dmesg to get the device path of this device</li>
     *     <li></li>
     *     <li></li>
     * </ol>
     * @return
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public List<UsbDevice> scan() throws IOException, ExecutionException, InterruptedException {
        final var builder = new ProcessBuilder();
        builder.command("lsusb");
        final var process = builder.start();
        process.onExit().get();

        final var descriptors = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                .map(LinuxUsbScanner::mapToDeviceDescriptor)
                .map(this::find)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        return descriptors;
    }

    /**
     * Map the output string from lsusb to a {@link UsbDeviceDescriptor}.
     *
     * @param str
     * @return
     */
    private static LinuxUsbDeviceDescriptor mapToDeviceDescriptor(final String str) {
        final var pattern = Pattern.compile("Bus (\\d{3}) Device (\\d{3}): ID\\s+([a-f,A-F,0-9]+):([a-f,A-F,0-9]+)\\s(.*)");
        final var matcher = pattern.matcher(str);
        if (!matcher.matches() || matcher.groupCount() != 5) {
            throw new IllegalArgumentException("Unable to parse the output from lsusb. Did the format change?");
        }

        final var builder = (LinuxBuilder)UsbDeviceDescriptor.ofVendorId(matcher.group(3)).withProductId(matcher.group(4));
        builder.withVendorDescription(matcher.group(5));
        builder.withBusNo(matcher.group(1));
        builder.withDeviceNo(matcher.group(2));
        return (LinuxUsbDeviceDescriptor)builder.build();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Object getUUID() {
        return "usb-scanner";
    }
}
