package io.snice.usb;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

/**
 * Basic class for basic information about a USB vendor and is loaded from the
 * file usb.ids, which is taken from here: http://www.linux-usb.org/usb.ids
 * and will be updated manually (did not want to have a 3rd party build dependency that
 * hits the internet. If you do not have connection, or if they are down etc then the build
 * would have failed so, manual update it is for now)
 */
public class VendorDescriptor {

    private final String id;
    private final String name;
    private final Map<String, VendorDeviceDescriptor> devices;

    private VendorDescriptor(final String id, final String name, final Map<String, VendorDeviceDescriptor> devices) {
        this.id = id;
        this.name = name;
        this.devices = devices;
    }

    public static VendorDescriptor of(final String id, final String name, final List<VendorDeviceDescriptor> devices) {
        assertNotEmpty(id, "The vendor id cannot be null or the empty string");
        assertNotEmpty(name, "The name of the vendor cannot be null or the empty string");
        final List<VendorDeviceDescriptor> devs = (devices == null ? List.of() : devices);
        final var map = new HashMap<String, VendorDeviceDescriptor>();
        devs.stream().forEach(dev -> map.put(dev.getId(), dev));
        return new VendorDescriptor(id, name, Collections.unmodifiableMap(map));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, VendorDeviceDescriptor> getDevices() {
        return devices;
    }

    @Override
    public String toString() {
        return String.format(String.format("%s\t%s [%d known devices]", id, name, devices.size()));
    }

}
