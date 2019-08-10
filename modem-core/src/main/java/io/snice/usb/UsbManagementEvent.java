package io.snice.usb;

import io.snice.usb.impl.DefaultUsbManagementEvent;

public interface UsbManagementEvent {

    interface ScanEvent extends UsbManagementEvent {
        static ScanEvent of() {
            return DefaultUsbManagementEvent.SCAN_EVENT;
        }
    }

    interface TerminateEvent extends UsbManagementEvent {
        static TerminateEvent of() {
            return DefaultUsbManagementEvent.TERMINATE_EVENT;
        }
    }

}
