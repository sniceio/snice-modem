package io.snice.usb.linux;

import io.snice.usb.DeviceId;

import java.util.List;

public class ScanResult {

    private final List<DeviceId> added;
    private final List<DeviceId> removed;

    private ScanResult(final List<DeviceId> added, final List<DeviceId> removed) {
        this.added = added;
        this.removed = removed;
    }

    public static ScanResult of(final List<DeviceId> added, final List<DeviceId> removed) {
        final List<DeviceId> a = added == null || added.isEmpty() ? List.of() : List.copyOf(added);
        final List<DeviceId> r = removed == null || removed.isEmpty() ? List.of() : List.copyOf(removed);
        return new ScanResult(a, r);
    }

}
