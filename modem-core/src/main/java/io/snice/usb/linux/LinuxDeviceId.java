package io.snice.usb.linux;

import io.snice.usb.DeviceId;

import java.util.Objects;

import static io.snice.preconditions.PreConditions.assertArgument;
import static io.snice.preconditions.PreConditions.assertNotEmpty;

public class LinuxDeviceId implements DeviceId {

    private final int busNo;
    private final int deviceAddress;
    private final String sysfs;
    private final Boolean isRootHub;

    public static final BusNoStep withUsbSysfs(final String sysfs) {
        assertNotEmpty(sysfs, "The Linux sysfs folder of the USB device cannot be null or the empty String");
        return (busNo) -> {
            assertArgument(busNo >= 0, "The USB Bus number must be equal or greater than zero");
            return (deviceAddress) -> {
                assertArgument(deviceAddress >= 0, "The Linux USB Device Address must be equal or greater than zero");
                return (isRootHub) -> new LinuxDeviceId(busNo, deviceAddress, sysfs, isRootHub);
            };
        };
    }

    public interface BusNoStep {
        DeviceAddressStep withBusNo(int busNo);

    }

    public interface DeviceAddressStep {
        RootHubStep withDeviceAddress(int deviceAddress);
    }

    public interface RootHubStep {
        LinuxDeviceId isRootHub(boolean isRootHub);
    }

    @Override
    public String toString() {
        return String.format("Bus %03d Device %03d Sysfs %s", getBusNo(), deviceAddress, sysfs);
    }

    private LinuxDeviceId(final int busNo, final int deviceAddress, final String sysfs, final boolean isRootHub) {
        this.busNo = busNo;
        this.deviceAddress = deviceAddress;
        this.sysfs = sysfs;
        this.isRootHub = isRootHub;
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

    public boolean isRootHub() {
        return isRootHub;
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
