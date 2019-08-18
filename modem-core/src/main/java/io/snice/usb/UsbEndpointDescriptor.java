package io.snice.usb;

public interface UsbEndpointDescriptor {

    DIRECTION getDirection();

    TYPE getType();

    int getMaxPacketSize();

    enum DIRECTION {
        IN, OUT, INOUT, UNKNOWN;
    }

    enum TYPE {
        BULK, CONTROL, INTERRUPT, ISOCHRONOUS, UNKNOWN;
    }

}
