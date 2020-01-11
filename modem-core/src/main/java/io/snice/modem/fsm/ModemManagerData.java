package io.snice.modem.fsm;

import io.hektor.fsm.Data;
import io.snice.usb.DeviceId;
import io.snice.usb.UsbDeviceDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModemManagerData implements Data {

    private final Map<DeviceId, UsbDeviceDescriptor> allAvailableModems = new HashMap<>();

    public void storeModem(final UsbDeviceDescriptor descriptor) {
        allAvailableModems.put(descriptor.getId(), descriptor);
    }

    public Optional<UsbDeviceDescriptor> getModem(final DeviceId id) {
        return Optional.ofNullable(allAvailableModems.get(id));
    }

    public List<UsbDeviceDescriptor> getAllAvailableModems() {
        return allAvailableModems.values().stream().collect(Collectors.toList());
    }

}
