package io.snice.usb.impl;

import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbInterfaceDescriptor;

import javax.usb.UsbPort;
import java.util.List;
import java.util.Optional;

import static io.snice.preconditions.PreConditions.assertNotNull;

public class JavaxUsbDeviceWrapper implements UsbDeviceDescriptor {

    private final javax.usb.UsbDevice javaxUsb;

    public static final UsbDeviceDescriptor of(final javax.usb.UsbDevice javaxUsb) {
        assertNotNull(javaxUsb, "The UsbDevice cannot be null");
        return new JavaxUsbDeviceWrapper(javaxUsb);
    }

    private JavaxUsbDeviceWrapper(final javax.usb.UsbDevice javaxUsb) {
        this.javaxUsb = javaxUsb;
    }

    @Override
    public String getVendorId() {
        return format(javaxUsb.getUsbDeviceDescriptor().idVendor());
    }

    @Override
    public String getProductId() {
        return format(javaxUsb.getUsbDeviceDescriptor().idProduct());
    }

    @Override
    public Optional<String> getVendorDescription() {
        return Optional.empty();
    }

    @Override
    public List<UsbInterfaceDescriptor> getInterfaces() {
        return List.of();
    }

    private String loopIt(final UsbPort port, int count, final String sysfs) {

        final var portNo = Byte.toUnsignedInt(port.getPortNumber());
        final var hub = port.getUsbHub();
        if (hub.isRootUsbHub()) {
            hub.getAttachedUsbDevices();
            return portNo + "-" + sysfs;
        }
        return loopIt(hub.getParentUsbPort(), count++, portNo + "." + sysfs);
    }


    public void figureShitOut() {
        System.err.println("The first one: " + javaxUsb.isUsbHub());
        System.err.println(loopIt(javaxUsb.getParentUsbPort(), 0, ""));
    }

    private static String format(final short id) {
        return String.format("%04x", id & '\uffff');
    }

}
