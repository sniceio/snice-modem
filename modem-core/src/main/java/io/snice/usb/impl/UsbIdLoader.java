package io.snice.usb.impl;

import io.snice.usb.VendorDescriptor;
import io.snice.usb.VendorDeviceDescriptor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;

public class UsbIdLoader {
    private static final String ID_FILE = "usb.ids";

    public static Map<String, VendorDescriptor> load() {
        final var list = new BufferedReader(new InputStreamReader(UsbIdLoader.class.getClassLoader().getResourceAsStream(ID_FILE)))
                .lines().filter(l -> !l.isEmpty() || !l.startsWith("#")).collect(VendorCollector.toVendorList());

        final var vendors = new HashMap<String, VendorDescriptor>();
        list.forEach(vendor -> vendors.put(vendor.getId(), vendor));
        return vendors;
    }

    private static class Builder {

        private static final String hex = "[a-f,A-F,0-9]{4}";
        private static final Pattern vendor = Pattern.compile("^(" + hex + ")\\s+(.*)");
        private static final Pattern device = Pattern.compile("^\\t(" + hex + ")\\s+(.*)");
        private static final Pattern ifs = Pattern.compile("^\\t\\t(" + hex + ")\\s+(.*)");

        private final List<VendorDescriptor> vendors = new ArrayList<>();
        private final List<VendorDeviceDescriptor> devices = new ArrayList<>();

        private String currentVendorId;
        private String currentVendorText;

        public void process(final String s) {
            final var vendorMatcher = vendor.matcher(s);
            final var deviceMatcher = device.matcher(s);
            if (vendorMatcher.matches()) {
                final String vendorId = vendorMatcher.group(1);
                final String text = vendorMatcher.group(2);

                if (currentVendorId != null) {
                    final var vendor = VendorDescriptor.of(currentVendorId, currentVendorText, List.copyOf(devices));
                    vendors.add(vendor);
                    currentVendorId = null;
                    currentVendorText = null;
                    devices.clear();
                }

                currentVendorId = vendorId;
                currentVendorText = text;
            } else if (deviceMatcher.matches()) {
                final String deviceId = deviceMatcher.group(1);
                final String text = deviceMatcher.group(2);
                devices.add(VendorDeviceDescriptor.of(deviceId, text));
            }
        }

        public List<VendorDescriptor> build() {
            if (currentVendorId != null) {
                vendors.add(VendorDescriptor.of(currentVendorId, currentVendorText, List.copyOf(devices)));
            }
            return vendors;
        }

    }

    private static class VendorCollector implements Collector<String, Builder, List<VendorDescriptor>> {

        private final Builder builder;

        private VendorCollector(final Builder builder) {
            this.builder = builder;
        }

        @Override
        public Supplier<Builder> supplier() {
            return () -> builder;
        }

        @Override
        public BiConsumer<Builder, String> accumulator() {
            return Builder::process;
        }

        @Override
        public BinaryOperator<Builder> combiner() {
            // not needed right now...
            return null;
        }

        @Override
        public Function<Builder, List<VendorDescriptor>> finisher() {
            return Builder::build;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Collections.unmodifiableSet(Set.of(Characteristics.UNORDERED));
        }

        public static VendorCollector toVendorList() {
            return new VendorCollector(new Builder());
        }
    }
}
