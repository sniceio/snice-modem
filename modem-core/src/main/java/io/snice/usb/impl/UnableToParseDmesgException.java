package io.snice.usb.impl;

import io.snice.usb.UsbException;

import java.util.regex.Pattern;

public class UnableToParseDmesgException extends UsbException {

    private final String dmesg;
    private final Pattern pattern;

    public UnableToParseDmesgException(final String dmesg, final Pattern pattern) {
        super("Unable to parse " + dmesg);
        this.dmesg = dmesg;
        this.pattern = pattern;
    }

    public String getDmesgLine() {
        return dmesg;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
