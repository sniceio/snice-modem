package io.snice.usb.linux;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.snice.preconditions.PreConditions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

@JsonDeserialize(builder = LibUsbConfiguration.Builder.class)
public class LibUsbConfiguration {

    private final Path devicesFolder;

    /**
     * TODO: these should be only configured in the linux usb scanner or something.
     *
     * By default, we will use lsusb to find the device no
     */
    private final String findDeviceNo;

    private final String findSysfsRoot;

    private static final String USB_DEVICE_CONNECTED = "\\[.*\\] usb (\\d-\\d[\\.\\d]*): *New USB device found, idVendor=([a-f,A-F,0-9]+), idProduct=([a-f,A-F,0-9]+).*";
    private static final String USB_DEVICE_DISCONNECTED = "\\[.*\\] usb (\\d-\\d[\\.\\d]*):.*device number.*(\\d+).*";

    private final Pattern dmesgUsbDeviceAttached;
    private final Pattern dmesgUsbDeviceDetached;

    /**
     * A list of <vendor_id>[:<device_id>] that will be the only ones
     * we'll actually process.
     */
    private final Map<String, List<String>> whiteList;

    public static Builder of() {
        return new Builder();
    }

    private LibUsbConfiguration(final Path devicesFolder, final String findSysfsRootCmd, final String findDeviceNoCmd, final Map<String, List<String>> whiteList) {
        this.devicesFolder = devicesFolder;
        this.findSysfsRoot = findSysfsRootCmd;
        this.whiteList = whiteList;
        this.findDeviceNo = findDeviceNoCmd;

        dmesgUsbDeviceAttached = Pattern.compile(USB_DEVICE_CONNECTED);
        dmesgUsbDeviceDetached = Pattern.compile(USB_DEVICE_DISCONNECTED);
    }

    public Path getDevicesFolder() {
        return devicesFolder;
    }

    public static class Builder {

        private final static Pattern hexPattern = Pattern.compile("[a-f,A-F,0-9]{4}");

        /**
         * We need to find the device number that the new usb device get assigned. Once we
         * have that one, we can check (also in dmesg) for where the device ended up in
         * sysfs, which is where we'll find the rest of the info we are a looking for.
         */
        // private final String DEFAULT_SYSFS_CMD = "dmesg | egrep \"new*.USB device number %s\"";
        private final String DEFAULT_SYSFS_CMD = "dmesg | egrep \"New USB device found.*idVendor=%s, idProduct=%s\"";

        private final static String DEFAULT_LSUSB_CMD = "lsusb -d %s:%s";


        private String devicesFolder = "/sys/bus/usb/devices";
        private List<String> whiteList;

        private String findDeviceCmd = DEFAULT_LSUSB_CMD;
        private String findSysfsCmd = DEFAULT_SYSFS_CMD;

        public Builder() {
            // left empty so that jackson can create an
            // instance success this builder.
        }

        @JsonProperty("whiteList")
        public Builder withWhiteList(final List<String> whiteList) {
            this.whiteList = whiteList;
            return this;
        }

        public Builder withDevicesPath(final String path) {
            assertNotEmpty(path, "The path to the devices folder cannot be null or the empty string");
            // TODO: check so that the path exists and that the process can read it.
            this.devicesFolder = path;
            return this;
        }

        public Builder withDevicesPath(final Path path) {
            PreConditions.assertNotNull(path, "The path to the devices folder cannot be null");
            // TODO: check so that the path exists and that the process can read it.
            this.devicesFolder = path.normalize().toString();
            return this;
        }

        @JsonProperty("findDeviceNoCmd")
        public Builder withFindDeviceNoCmd(final String cmd) {
            assertNotEmpty(cmd, "The command to use to find the device no of the USB device cannot be mepty");
            this.findDeviceCmd = cmd;
            return this;
        }

        @JsonProperty("findSysfsCmd")
        public Builder withFindSysfsCmd(final String cmd) {
            assertNotEmpty(cmd, "The command to use to find the device no of the USB device cannot be empty");
            this.findSysfsCmd = cmd;
            return this;
        }

        public LibUsbConfiguration build() {
            final Path path = Paths.get(devicesFolder);
            return new LibUsbConfiguration(path, findSysfsCmd, findDeviceCmd, ensureWhiteList());
        }

        private Map<String, List<String>> ensureWhiteList() {
            if (whiteList == null) {
                return Map.of();
            }

            final var whiteListMap = new HashMap<String, List<String>>();

            whiteList.forEach(def -> {
                final var parts = def.split(":");
                if (parts.length > 2) {
                    throw new IllegalArgumentException("The vendor id and/or product id were " +
                            "misconfigured. You cannot have more than one ':' in it. " +
                            "Format is <vendor_id>[:<product_id>]");
                }

                final var vendorId = ensureId(parts[0]);
                final Optional<String> productId = parts.length == 2 ? Optional.of(ensureId(parts[1])) : Optional.empty();

                final var list = whiteListMap.computeIfAbsent(vendorId, key ->  new ArrayList<>());
                productId.ifPresent(id -> list.add(id));

            });

            return whiteListMap;
        }

        private static String ensureId(final String id) {
            if (hexPattern.matcher(id).matches()) {
                return id;
            }

            throw new IllegalArgumentException("The vendor_id[:product_id] must be a hex string of exactly 4 characters");
        }

    }
}
