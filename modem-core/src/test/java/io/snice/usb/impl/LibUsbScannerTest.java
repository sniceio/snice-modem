package io.snice.usb.impl;

import io.snice.usb.UsbTestBase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.usb4java.Context;
import org.usb4java.DeviceDescriptor;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import javax.usb.UsbConst;
import javax.usb.UsbEndpoint;
import javax.usb.UsbHostManager;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbServices;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class LibUsbScannerTest extends UsbTestBase {

    private UsbServices usbServices;
    private LibUsbScanner scanner;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        usbServices = UsbHostManager.getUsbServices();
        // scanner = LibUsbScanner.of(usbServices, config);
        scanner = Mockito.mock(LibUsbScanner.class);
    }

    @Test
    public void testScan() throws Exception {
        scanner.scan();
    }

    @Test
    public void testLowLevelApi() {
        final Context context = new Context();
        final int result = LibUsb.init(context);
        if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to initialize libusb.", result);
        // final var device = scanner.findDevice("2c7c", "0125");
        // final var device = scanner.findDevice("1050", "0407");
        // final var device = scanner.findDevice("0451", "8440");
        final var device = scanner.findDevice("0a5c", "5834");
        final var portNo = LibUsb.getPortNumber(device);
        final var busNo = LibUsb.getBusNumber(device);
        final var devAddress = LibUsb.getDeviceAddress(device);
        final var desc = new DeviceDescriptor();
        LibUsb.getDeviceDescriptor(device, desc);
        System.err.println("Bus number is: " + busNo);
        System.err.println("Port number is: " + portNo);
        System.err.println("Device Address: " + devAddress);

        final var vendorId = String.format("%04x", desc.idVendor() & '\uffff');
        final var productId = String.format("%04x", desc.idProduct() & '\uffff');
        System.err.println("VendorId: " + vendorId);
        System.err.println("ProductId: " + productId);

        final var b = ByteBuffer.allocateDirect(8);
        final var count = LibUsb.getPortNumbers(device, b);
        System.err.println("Count: " + count);
        final List<Integer> ports = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            ports.add((int)b.get(i));
        }

        final var sysfs = busNo + "-" + ports.stream().map(String::valueOf).collect(Collectors.joining(","));
        System.err.println("Sysfs: " + sysfs);
        final var attachEvent = LinuxUsbDeviceAttachEvent.of(vendorId).withProductId(productId).withSysfs(sysfs);
        System.err.println(attachEvent);


    }

    @Test
    public void testWaitForEvent() throws Exception {
        final var latch = new CountDownLatch(1);
        final var ref = new AtomicReference<javax.usb.UsbDevice>();

        usbServices.addUsbServicesListener(new UsbServicesListener() {
            @Override
            public void usbDeviceAttached(final UsbServicesEvent event) {
                final var device = event.getUsbDevice();
                final short vendorId = device.getUsbDeviceDescriptor().idVendor();
                final short productId = device.getUsbDeviceDescriptor().idProduct();
                final String vendorIdStr = String.format("%04x", vendorId & '\uffff');
                final String productIdStr = String.format("%04x", productId & '\uffff');
                if ("1199".equals(vendorIdStr) || "05c6".equals(vendorIdStr) || "2c7c".equals(vendorIdStr)) {
                    ref.set(device);
                    latch.countDown();
                }

            }

            @Override
            public void usbDeviceDetached(final UsbServicesEvent event) {
                LibUsbScanner.dumpDevice(event.getUsbDevice());
                // latch.countDown();
            }
        });

        latch.await(3, TimeUnit.SECONDS);
        final var dev = ref.get();
        System.err.println(dev);

        if (true) {
            return;
        }
        final List<UsbInterface> ifs = dev.getActiveUsbConfiguration().getUsbInterfaces();
        final var iface = ifs.get(ifs.size() - 2);
        System.err.println("Interface No: " + Byte.toUnsignedInt(iface.getUsbInterfaceDescriptor().bInterfaceNumber()));
        final List<UsbEndpoint> endpoints = iface.getUsbEndpoints();
        UsbEndpoint readEndpoint = null;
        UsbEndpoint writeEndpoint = null;
        for (final UsbEndpoint ep : endpoints) {
            final var direction = ep.getDirection();
            final var type = ep.getType();

            if (direction == UsbConst.ENDPOINT_DIRECTION_IN && type == UsbConst.ENDPOINT_TYPE_BULK) {
                System.out.println("IN BULK");
                readEndpoint = ep;
            } if (direction == UsbConst.ENDPOINT_DIRECTION_OUT && type == UsbConst.ENDPOINT_TYPE_BULK) {
                System.out.println("OUT BULK");
                writeEndpoint = ep;
            }

            if (type == UsbConst.ENDPOINT_TYPE_CONTROL) {
                System.out.println("CONTROL");
            } else if (type == UsbConst.ENDPOINT_TYPE_INTERRUPT) {
                System.out.println("INTERRUPT");
            } else if (type == UsbConst.ENDPOINT_TYPE_ISOCHRONOUS) {
                System.out.println("ISOCHRONOUS");
            }
        }

        /*
        ifs.forEach(iface -> {
        });
        */
        // final var iface = ifs.get(1);
        // iface.claim();

        iface.claim(new UsbInterfacePolicy()
        {
            @Override
            public boolean forceClaim(final UsbInterface usbInterface) {
                System.err.println("Fucking forcing it");
                return true;
            }
        });
        try {
            final var writePipe = writeEndpoint.getUsbPipe();
            final var readPipe = readEndpoint.getUsbPipe();
            System.out.println("Trying to open pipes");
            writePipe.open();
            readPipe.open();
            System.out.println("Trying to write to pipes");
            int sent = writePipe.syncSubmit("ATI\n\r".getBytes());
            System.out.println(sent + " bytes sent");

            final var latch2 = new CountDownLatch(1);
            readPipe.addUsbPipeListener(new UsbPipeListener() {
                @Override
                public void errorEventOccurred(final UsbPipeErrorEvent event) {
                    System.err.println(event);
                    latch2.countDown();
                }

                @Override
                public void dataEventOccurred(final UsbPipeDataEvent event) {
                    System.out.println("Received data " + new String(event.getData()));
                    latch2.countDown();
                }
            });

            var recv = new byte[100];
            readPipe.asyncSubmit(recv);
            // final var buf = Buffers.wrap(recv).slice(received);
            // System.out.println(buf);
            latch2.await(5000, TimeUnit.MILLISECONDS);
            sent = writePipe.syncSubmit("AT+COPS?\n\r".getBytes());
            System.out.println(sent + " bytes sent");

            recv = new byte[100];
            readPipe.asyncSubmit(recv);
            Thread.sleep(5000);
            writePipe.close();
            readPipe.close();


        } finally {
            // iface.release();
        }
    }


    @Test
    public void testCreateUsbDevice() throws Exception {
        final Map<String, List<String>> map = new HashMap<>();
        map.put("1-2", List.of("1-2:1.2/ttyUSB6", "1-2:1.0/ttyUSB4", "1-2:1.3/ttyUSB7", "1-2:1.1/ttyUSB5"));
        map.put("1-3", List.of("1-3:1.2/ttyUSB2", "1-3:1.0/ttyUSB0", "1-3:1.3/ttyUSB3", "1-3:1.1/ttyUSB1"));
        // scanner.createDevice(config.getUsbSysfsRoot(), "1-2", map.get("1-2"));
    }
}
