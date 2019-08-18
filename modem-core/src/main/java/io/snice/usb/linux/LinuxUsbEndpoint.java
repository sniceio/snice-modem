package io.snice.usb.linux;

import io.snice.usb.UsbEndpointDescriptor;
import io.snice.usb.UsbException;
import org.usb4java.EndpointDescriptor;

import javax.usb.UsbConst;

public class LinuxUsbEndpoint implements UsbEndpointDescriptor {

    private final DIRECTION direction;
    private final TYPE type;
    private final int maxPacketSize;

    private LinuxUsbEndpoint(final DIRECTION direction, final TYPE type, final int maxPacketSize) {
        this.direction = direction;
        this.type = type;
        this.maxPacketSize = maxPacketSize;
    }

    public static LinuxUsbEndpoint from(final EndpointDescriptor descriptor) throws UsbException {
        final var direction = getDirection(descriptor);
        final var type = getType(descriptor);
        final var maxPacket = descriptor.wMaxPacketSize();
        return new LinuxUsbEndpoint(direction, type, maxPacket);
    }

    @Override
    public DIRECTION getDirection() {
        return direction;
    }

    @Override
    public TYPE getType() {
        return type;
    }

    @Override
    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    private static UsbEndpointDescriptor.DIRECTION getDirection(final EndpointDescriptor descriptor) {
        final byte address = descriptor.bEndpointAddress();
        final boolean in = (address & UsbConst.ENDPOINT_DIRECTION_MASK) == UsbConst.ENDPOINT_DIRECTION_IN;
        final boolean out = (address & UsbConst.ENDPOINT_DIRECTION_MASK) == UsbConst.ENDPOINT_DIRECTION_OUT;

        if (in && out) {
            return DIRECTION.INOUT;
        } else if (in) {
            return DIRECTION.IN;
        }

        return DIRECTION.OUT;
    }

    private static UsbEndpointDescriptor.TYPE getType(final EndpointDescriptor descriptor) {
        final byte attribs = descriptor.bmAttributes();
        if ((attribs & UsbConst.ENDPOINT_TYPE_MASK) == UsbConst.ENDPOINT_TYPE_BULK) {
            return TYPE.BULK;
        }

        if ((attribs & UsbConst.ENDPOINT_TYPE_MASK) == UsbConst.ENDPOINT_TYPE_INTERRUPT) {
            return TYPE.INTERRUPT;
        }

        if ((attribs & UsbConst.ENDPOINT_TYPE_MASK) == UsbConst.ENDPOINT_TYPE_CONTROL) {
            return TYPE.CONTROL;
        }

        if ((attribs & UsbConst.ENDPOINT_TYPE_MASK) == UsbConst.ENDPOINT_TYPE_ISOCHRONOUS) {
            return TYPE.ISOCHRONOUS;
        }

        throw new UsbException("Unknown endpoint type");
    }

}
