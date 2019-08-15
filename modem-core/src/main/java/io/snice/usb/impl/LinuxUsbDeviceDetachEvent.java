package io.snice.usb.impl;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotEmpty;

public class LinuxUsbDeviceDetachEvent implements LinuxUsbDeviceEvent {
    private final String sysfs;
    private final int deviceNo;

    public static SysfsStep of(final int deviceNo) {
        assertArgument(deviceNo >= 0, "The Linux Device No must be greater or equal to zero");
        return (sysfs) -> {
            assertNotEmpty(sysfs, "The Linux sysfs folder for the device cannot be null or the empty String");
            return new LinuxUsbDeviceDetachEvent(sysfs,deviceNo);
        };
    }

    interface SysfsStep {
        LinuxUsbDeviceDetachEvent withSysfs(String sysfs);
    }

    @Override
    public String getSysfs() {
        return sysfs;
    }

    @Override
    public String toString() {
        return LinuxUsbDeviceDetachEvent.class.getSimpleName() + " for device no " + deviceNo
                + " mounted at " + sysfs;
    }

    private LinuxUsbDeviceDetachEvent(final String sysfs, final int deviceNo) {
        this.sysfs = sysfs;
        this.deviceNo = deviceNo;
    }
}
