package io.snice.usb.event;

public class Scan {

    public static final Scan SCAN = new Scan();

    private Scan() {
        // left empty intentionally
    }

    @Override
    public String toString() {
        return "SCAN";
    }
}
