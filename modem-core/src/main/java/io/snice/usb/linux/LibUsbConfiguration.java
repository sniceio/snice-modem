package io.snice.usb.linux;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.snice.preconditions.PreConditions.assertNotEmpty;
import static io.snice.preconditions.PreConditions.assertNotNull;

@JsonDeserialize(builder = LibUsbConfiguration.Builder.class)
public class LibUsbConfiguration {

    private final Path usbfs;


    // private static final String USB_DEVICE_CONNECTED = "\\[.*\\] usb (\\d-\\d[\\.\\d]*): *New USB device found, idVendor=([a-f,A-F,0-9]+), idProduct=([a-f,A-F,0-9]+).*";
    // private static final String USB_DEVICE_DISCONNECTED = "\\[.*\\] usb (\\d-\\d[\\.\\d]*):.*device number.*(\\d+).*";


    /**
     * A list of <vendor_id>[:<device_id>] that will be the only ones
     * we'll actually process.
     */
    private final Map<String, List<String>> whiteList;

    public static Builder of() {
        return new Builder();
    }

    private LibUsbConfiguration(final Path usbfs, final Map<String, List<String>> whiteList) {
        this.usbfs = usbfs;
        this.whiteList = whiteList;
    }

    public Path getUsbSysfsRoot() {
        return usbfs;
    }

    public static class Builder {

        private final static Pattern hexPattern = Pattern.compile("[a-f,A-F,0-9]{4}");

        private String usbfsRootPath = "/sys/bus/usb/devices";
        private List<String> whiteList;

        public Builder() {
            // left empty so that jackson can create an
            // instance success this builder.
        }

        @JsonProperty("whiteList")
        public Builder withWhiteList(final List<String> whiteList) {
            this.whiteList = whiteList;
            return this;
        }

        public Builder withUsbfsRoot(final String path) {
            assertNotEmpty(path, "The path to the Linux usbfs root cannot be null or the empty string");
            this.usbfsRootPath = path;
            return this;
        }

        public Builder withUsbfsRoot(final Path path) {
            assertNotNull(path, "The path to the Linux usbfs root cannot be null or the empty string");
            // TODO: check so that the path exists and that the process can read it.
            this.usbfsRootPath = path.normalize().toString();
            return this;
        }

        public LibUsbConfiguration build() {
            final Path path = Paths.get(usbfsRootPath);
            return new LibUsbConfiguration(path, ensureWhiteList());
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
