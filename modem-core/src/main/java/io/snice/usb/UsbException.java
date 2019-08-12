package io.snice.usb;

public class UsbException extends RuntimeException {

    public UsbException(final String msg) {
        super(msg);
    }

    public UsbException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
