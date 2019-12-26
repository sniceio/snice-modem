package io.snice.usb.fsm;

import io.hektor.fsm.Data;
import io.snice.usb.DeviceId;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.fsm.support.Subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class UsbManagerData implements Data {

    private final Map<DeviceId, UsbDeviceDescriptor> devices = new HashMap<>();
    private final List<Subscription> subscriptions = new ArrayList<>();

    public boolean isKnownDevice(final DeviceId id) {
        return devices.containsKey(id);
    }

    public boolean isKnownDevice(final UsbDeviceDescriptor desc) {
        return isKnownDevice(desc.getId());
    }

    public boolean isUnKnownDevice(final UsbDeviceDescriptor desc) {
        return !isKnownDevice(desc);
    }

    public void addSubscription(final Subscription subscription) {
        assertNotNull(subscription);
        subscriptions.add(subscription);
    }

    public void deviceAttached(final UsbDeviceDescriptor device) {
        devices.put(device.getId(), device);
    }

    public Optional<UsbDeviceDescriptor> deviceDetached(final DeviceId id) {
        return Optional.ofNullable(devices.remove(id));
    }

    public Optional<UsbDeviceDescriptor> deviceDetached(final UsbDeviceDescriptor dev) {
        return deviceDetached(dev.getId());
    }

    public List<UsbDeviceDescriptor> getAvailableDevices() {
        return devices.values().stream().collect(Collectors.toList());
    }

    public List<Subscription> getSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }
}
