package io.snice.usb.impl;

public interface LinuxUsbDeviceEvent {
    String getSysfs();

    default boolean isAttachEvent() {
        return false;
    }

    default boolean isDetachEvent() {
        return false;
    }
}
