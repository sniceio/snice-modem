package io.snice.usb.linux;

import io.snice.usb.DeviceId;

import java.util.Objects;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotEmpty;

public class LinuxDeviceId implements DeviceId {

    private final int busNo;
    private final int deviceAddress;
    private final String sysfs;

    public static final BusNoStep fromSysfs(final String sysfs) {
        assertNotEmpty(sysfs, "The Linux sysfs folder of the USB device cannot be null or the empty String");
        return (busNo) -> {
            assertArgument(busNo >= 0, "The USB Bus number must be equal or greater than zero");
            return (deviceAddress) -> {
                assertArgument(deviceAddress >= 0, "The Linux USB Device Address must be equal or greater than zero");
                return new LinuxDeviceId(busNo, deviceAddress, sysfs);
            };
        };
    }

    public interface BusNoStep {
        DeviceAddressStep withBusNo(int busNo);

    }

    public interface DeviceAddressStep {
        LinuxDeviceId withDeviceAddress(int deviceAddress);
    }

    private LinuxDeviceId(final int busNo, final int deviceAddress, final String sysfs) {
        this.busNo = busNo;
        this.deviceAddress = deviceAddress;
        this.sysfs = sysfs;
    }

    public int getBusNo() {
        return busNo;
    }

    public int getDeviceAddress() {
        return deviceAddress;
    }

    public String getSysfs() {
        return sysfs;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final LinuxDeviceId that = (LinuxDeviceId) o;
        return busNo == that.busNo &&
                deviceAddress == that.deviceAddress &&
                sysfs.equals(that.sysfs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(busNo, deviceAddress, sysfs);
    }
}
