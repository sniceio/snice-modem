package io.snice.usb.impl;

import io.hektor.actors.LoggingSupport;
import io.snice.processes.Processes;
import io.snice.usb.NoSuchUsbDevice;
import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbException;
import io.snice.usb.UsbScanner;
import io.snice.usb.VendorDescriptor;
import io.snice.usb.VendorDeviceDescriptor;
import io.snice.usb.impl.LinuxUsbDeviceDescriptorOld.LinuxBuilder;
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
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
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

    private static final String USB_DEVICE_DISCONNECTED_REGEXP = "\\[.*\\] usb (\\d-\\d[\\.\\d]*):.*device number.*(\\d+).*";

    // TODO: needs to be configured
    private static final String LSUSB_CMD = "lsusb -d %s:%s";

    private final Pattern dmesgNewDevicePattern;
    private final Pattern dmesgDisconnectPattern;

    private final UsbConfiguration config;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    private final Map<String, VendorDescriptor> knownUsbVendors;

    public static LinuxUsbScanner of(final UsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors) {
        assertNotNull(config, "The configuration cannot be null");

        // TODO: get from config file
        final var pattern = Pattern.compile(USB_DEVICE_CONNECTED_REGEXP);
        final var disconnectPattern = Pattern.compile(USB_DEVICE_DISCONNECTED_REGEXP);
        return new LinuxUsbScanner(config, knownUsbVendors == null ? Collections.emptyMap() : knownUsbVendors, pattern, disconnectPattern);
    }

    public static LinuxUsbScanner of(final UsbConfiguration config) {
        return of(config, null);
    }

    private LinuxUsbScanner(final UsbConfiguration config, final Map<String, VendorDescriptor> knownUsbVendors,
                            final Pattern dmesgNewDevicePattern,
                            final Pattern dmesgDisconnectPattern) {
        this.config = config;
        this.knownUsbVendors = knownUsbVendors;
        this.dmesgNewDevicePattern = dmesgNewDevicePattern;
        this.dmesgDisconnectPattern = dmesgDisconnectPattern;
    }

    public Optional<LinuxUsbDeviceEvent> parseDmesg(final String dmesg) {
        final var attachMatcher = config.getDmesgUsbDeviceAttachedPattern().matcher(dmesg);
        if (attachMatcher.matches()) {
            return parseAttachMessage(attachMatcher, dmesg);
        }

        final var detachMatcher = config.getDmesgUsbDeviceDetachedPattern().matcher(dmesg);
        if (detachMatcher.matches()) {
            return parseDetachMessage(detachMatcher, dmesg);
        }

        throw new UnableToParseDmesgException(dmesg, config.getDmesgUsbDeviceAttachedPattern());
    }

    private Optional<LinuxUsbDeviceEvent> parseDetachMessage(final Matcher matcher, final String dmesg) {
        if (matcher.groupCount() != 2) {
            throw new UnableToParseDmesgException(dmesg, matcher.pattern());
        }

        final var sysfs = matcher.group(1);
        final var deviceNo = Integer.parseInt(matcher.group(2));
        return Optional.of(LinuxUsbDeviceDetachEvent.of(deviceNo).withSysfs(sysfs));
    }

    private Optional<LinuxUsbDeviceEvent> parseAttachMessage(final Matcher matcher, final String dmesg) {
        if (matcher.groupCount() != 3) {
            throw new UnableToParseDmesgException(dmesg, matcher.pattern());
        }

        final var sysfs = matcher.group(1);
        final var vendorId = matcher.group(2);
        final var productId = matcher.group(3);
        return Optional.of(LinuxUsbDeviceAttachEvent.of(vendorId).withProductId(productId).withSysfs(sysfs));
    }


    /**
     * Unique to Linux. The {@link LinuxUsbDmesgMonitor} is "tailing" dmesg and whenever it detects a
     * "New USB device connected" message, it'll call this method to setup the new {@link LinuxUsbDevice}
     */
    public List<LinuxUsbDevice> processDmesgNewDevice(final LinuxUsbDeviceAttachEvent evt) {
        final var vendorId = evt.getVendorId();
        final var productId = evt.getProductId();
        final var sysfs = evt.getSysfs();

        if (!config.processDevice(vendorId, productId)) {
            logger.info("Skipping new USB device " + vendorId + ":" + productId + " attached to sysfs " + sysfs);
            return List.of();
        }

        if (isRootHub(vendorId, productId)) {
            return List.of();
        }

        try {
            final var cmd = String.format(LSUSB_CMD, vendorId, productId);
            final var usbDevices = Processes.execute(cmd).toCompletableFuture().get(5000, TimeUnit.MILLISECONDS).stream()
                    .map(LinuxUsbScanner::mapToDeviceDescriptor)
                    .map(desc -> LinuxUsbDevice.of(desc).withDevicePath(sysfs).withLinuxSysfsDevicesPath(config.getDevicesFolder()).build())
                    .collect(Collectors.toList());
            return usbDevices;
        } catch (final TimeoutException e) {
            e.printStackTrace();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return List.of();
    }

    @Override
    public Optional<UsbDevice> find(final UsbDeviceDescriptor descriptor) throws UsbException {

        final var linuxDescriptor = (LinuxUsbDeviceDescriptorOld)descriptor;

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
    public List<UsbDeviceDescriptor> scan() throws UsbException {
        try {
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

            return null;
        } catch (final Exception e) {
            throw new UsbException(e.getMessage(), e);
        }
    }

    @Override
    public List<UsbDeviceDescriptor> scan(final Predicate<String> vendorFilter) throws UsbException {
        return null;
    }

    @Override
    public List<UsbDeviceDescriptor> scan(final BiPredicate<String, String> vendorProductFilter) throws UsbException {
        return null;
    }

    /**
     * Map the output string from lsusb to a {@link UsbDeviceDescriptor}.
     *
     * @param str
     * @return
     */
    private static LinuxUsbDeviceDescriptorOld mapToDeviceDescriptor(final String str) {
        final var pattern = Pattern.compile("Bus (\\d{3}) Device (\\d{3}): ID\\s+([a-f,A-F,0-9]+):([a-f,A-F,0-9]+)\\s(.*)");
        final var matcher = pattern.matcher(str);
        if (!matcher.matches() || matcher.groupCount() != 5) {
            throw new IllegalArgumentException("Unable to parse the output from lsusb. Did the format change?");
        }

        final var builder = (LinuxBuilder)UsbDeviceDescriptor.ofVendorId(matcher.group(3)).withProductId(matcher.group(4));
        builder.withDescription(matcher.group(5));
        builder.withBusNo(matcher.group(1));
        builder.withDeviceNo(matcher.group(2));
        return (LinuxUsbDeviceDescriptorOld)builder.build();
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
