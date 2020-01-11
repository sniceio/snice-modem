package io.snice.usb.event;

import io.snice.usb.DeviceId;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class ClaimUsbDevice {

    private final DeviceId deviceId;

    private ClaimUsbDevice(final DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public static ClaimUsbDevice of(final DeviceId deviceId) {
        assertNotNull(deviceId);
        return new ClaimUsbDevice(deviceId);
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }
}
