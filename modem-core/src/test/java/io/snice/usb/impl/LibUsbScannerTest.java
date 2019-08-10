package io.snice.usb.impl;

import io.snice.usb.UsbTestBase;
import org.junit.Before;
import org.junit.Test;

import javax.usb.UsbHostManager;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LibUsbScannerTest extends UsbTestBase {

    private UsbServices usbServices;
    private LibUsbScanner scanner;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        usbServices = UsbHostManager.getUsbServices();
        scanner = LibUsbScanner.of(usbServices, config);
    }

    @Test
    public void testScan() throws Exception {
        scanner.scan();
    }

    @Test
    public void testWaitForEvent() throws Exception {
        final var latch = new CountDownLatch(30);
        usbServices.addUsbServicesListener(new UsbServicesListener() {
            @Override
            public void usbDeviceAttached(final UsbServicesEvent event) {
                LibUsbScanner.dumpDevice(event.getUsbDevice());
                latch.countDown();
            }

            @Override
            public void usbDeviceDetached(final UsbServicesEvent event) {
                LibUsbScanner.dumpDevice(event.getUsbDevice());
                latch.countDown();
            }
        });

        latch.await(3, TimeUnit.SECONDS);
    }


    @Test
    public void testCreateUsbDevice() throws Exception {
        final Map<String, List<String>> map = new HashMap<>();
        map.put("1-2", List.of("1-2:1.2/ttyUSB6", "1-2:1.0/ttyUSB4", "1-2:1.3/ttyUSB7", "1-2:1.1/ttyUSB5"));
        map.put("1-3", List.of("1-3:1.2/ttyUSB2", "1-3:1.0/ttyUSB0", "1-3:1.3/ttyUSB3", "1-3:1.1/ttyUSB1"));
        // scanner.createDevice(config.getDevicesFolder(), "1-2", map.get("1-2"));
    }
}
