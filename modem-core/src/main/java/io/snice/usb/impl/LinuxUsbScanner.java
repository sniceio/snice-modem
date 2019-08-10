package io.snice.usb.impl;

import io.snice.usb.NoSuchUsbDevice;
import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbScanner;
import org.usb4java.DeviceDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotNull;
import static io.snice.usb.impl.FileHelper.getValue;

public class LinuxUsbScanner implements UsbScanner {

    private final static String DEFAULT_USB_SERIAL_FS = "/sys/bus/usb-serial/devices";

    private final static String DEFAULT_DMESG = "/sys/bus/usb-serial/devices";

    private final static Pattern DMESG_SYSFS_PATTERN = Pattern.compile("\\[.*\\] usb (\\d-\\d[\\.\\d]*): .*");

    private final UsbConfiguration config;
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    public static LinuxUsbScanner of(final UsbConfiguration config) {
        assertNotNull(config, "The configuration cannot be null");
        return new LinuxUsbScanner(config);
    }

    @Override
    public Optional<UsbDevice> find(final UsbDeviceDescriptor descriptor) throws IOException {

        final int usbDevice = findUsbDeviceNumber(descriptor).orElseThrow(() -> NoSuchUsbDevice.of(descriptor));
        final var dmesg = executeShellCommandBlah("/bin/sh", "-c", String.format(config.getFindSysfsRoot(), usbDevice));
        final var s = dmesg.orElseThrow(() -> NoSuchUsbDevice.of(descriptor, usbDevice));
        final var matcher = DMESG_SYSFS_PATTERN.matcher(s);
        if (!matcher.matches() || matcher.groupCount() != 1) {
            throw new RuntimeException("Unable to match the USB pattern... ");
        }

        final var sysfs = matcher.group(1);
        return LinuxUsbScanner.createDevice(descriptor, sysfs, config.getDevicesFolder());
    }


    private Optional<Integer> findUsbDeviceNumber(final UsbDeviceDescriptor descriptor) {
        final var pattern = Pattern.compile("Bus.*(\\d{3}): ID.*");
        final var vendor = descriptor.getVendorId();
        final var device = descriptor.getDeviceId();

        final var lsUsbOutput = executeShellCommandBlah("/bin/sh", "-c", String.format(config.getFindDeviceNoCmd(), vendor, device));

        if (lsUsbOutput.isEmpty()) {
            return Optional.empty();
        }

        final var matcher = pattern.matcher(lsUsbOutput.get());
        if (!matcher.matches()) {
            System.err.println("God damn it, regexp doesn't match");
            return Optional.empty();
        }

        return Optional.ofNullable(Integer.parseInt(matcher.group(1)));
    }

    private static List<String> executeShellWithGrep(final String regexp, final String... cmd) {
        final var pattern = Pattern.compile(regexp);
        System.err.println(cmd[0]);
        System.err.println(executeShellCommand(cmd));
        // return executeShellCommand(cmd).stream().filter(l -> pattern.matcher(l).matches()).collect(Collectors.toList());
        return List.of();
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

    private LinuxUsbScanner(final UsbConfiguration config) {
        this.config = config;
    }

    @Override
    public List<UsbDevice> scan() throws IOException, ExecutionException, InterruptedException {
        final var builder = new ProcessBuilder();
        // final var path = config.getDevicesFolder().toAbsolutePath();
        // builder.command("find", "-L", path.toString(), "-maxdepth", "2", "-name", "tty*");
        builder.command("lsusb");
        final var process = builder.start();
        process.onExit().get();

        final List<String> output = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().collect(Collectors.toList());
        output.stream().map(str -> {

        });
        // final var length = path.toString().length() + 1;
        // final var serials = output.stream().map(s -> s.substring(length)).collect(Collectors.toList());
        // final Map<String, List<String>> map = serials.stream().collect(groupingBy(LinuxUsbScanner::gropuByKey));

        System.err.println(output);

        /*
        try (final Stream<Path> walk = Files.walk(path)) {

            final List<Path> devices = walk.filter(Files::isDirectory)
                    .filter(folder -> folder.getFileName().toString().matches("\\d-\\d"))
                    .collect(Collectors.toList());

            devices.forEach(System.out::println);

        } catch (final IOException e) {
            e.printStackTrace();
        }
        */

        return null;
    }

    private static DeviceDescriptor mapToDeviceDescriptor(final String str) {
        final var pattern = Pattern.compile("Bus.*ID\\s+(\\d+):(\\d+)\\s(.*)");
        final var matcher = pattern.matcher(str);
        if (!matcher.matches() || matcher.groupCount() != 3) {
            throw new IllegalArgumentException("Unable to parse the output from lsusb. Did the format change?");
        }

        return
    }

    public static Optional<UsbDevice> createDevice(final UsbDeviceDescriptor descriptor, final String device, final Path devicesPath) throws IOException {
        final var devicePath = devicesPath.resolve(device);
        final var ifs = Files.list(devicePath)
                .map(Path::toFile)
                .filter(File::isDirectory)
                .map(File::toPath)
                .filter(f -> f.getFileName().toString().startsWith(device + ":"))
                .map(LinuxUsbInterfaceDescriptor::of)
                .collect(Collectors.toList());
        System.out.println(ifs);
        System.err.println("it should be here " + devicePath);
        return Optional.empty();
    }


    public static Optional<UsbDevice> createDevice(final Path folder, final String device, final List<String> serialPorts) throws IOException {
        final Path deviceFolder = folder.resolve(device);

        final String manufacturer = getValue(deviceFolder, "manufacturer");
        final String product = getValue(deviceFolder, "idProduct");
        final String vendor = getValue(deviceFolder, "idVendor");

        System.err.println(vendor);
        System.err.println(product);
        System.err.println(manufacturer);
        return null;
    }

    private static String gropuByKey(final String s) {
        return s.substring(0, s.indexOf(":"));
    }

}
