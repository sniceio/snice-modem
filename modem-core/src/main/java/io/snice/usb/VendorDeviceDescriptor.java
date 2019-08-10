package io.snice.usb;

import io.snice.preconditions.PreConditions;

import static io.snice.preconditions.PreConditions.assertNotEmpty;

public class VendorDeviceDescriptor {
    private final String id;
    private final String description;

    private VendorDeviceDescriptor(final String id, final String description) {
        this.id = id;
        this.description = description;
    }

    public static VendorDeviceDescriptor of(String id, String description) {
        assertNotEmpty(id, "The device id cannot be null or the empty string");
        assertNotEmpty(description, "The device id cannot be null or the empty string");
        return new VendorDeviceDescriptor(id, description);
    }

    public String toString() {
        return String.format("%s\t%s", id, description);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
