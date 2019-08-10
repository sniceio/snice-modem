package io.snice.usb.fsm;

import io.hektor.fsm.Data;
import io.snice.usb.UsbDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UsbManagerData implements Data {

    private final List<UsbDevice> devices = new ArrayList<>();

    public void store(UsbDevice device) {
        devices.add(device);
    }

    public List<UsbDevice> getDevices() {
        return Collections.unmodifiableList(devices);
    }
}
