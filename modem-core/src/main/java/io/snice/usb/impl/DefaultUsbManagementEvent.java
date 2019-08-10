package io.snice.usb.impl;

import io.snice.usb.UsbManagementEvent;

public class DefaultUsbManagementEvent implements UsbManagementEvent {

    public static final UsbManagementEvent.ScanEvent SCAN_EVENT = new ScanEvent() {};
    public static final UsbManagementEvent.TerminateEvent TERMINATE_EVENT = new TerminateEvent() {};

    private DefaultUsbManagementEvent() {}

}
