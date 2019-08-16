package io.snice.usb.impl;

import io.snice.usb.UsbTestBase;
import io.snice.usb.linux.LibUsbConfiguration;
import io.snice.usb.linux.LinuxLibUsbScanner;
import org.junit.Before;
import org.junit.Test;

public class LinuxLibUsbScannerTest extends UsbTestBase {

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
    }

    @Test
    public void testScan() {
        final var config = LibUsbConfiguration.of().build();
        final var scanner = LinuxLibUsbScanner.of(config, knownUsbVendors);
        // final var devices = scanner.scan("2c7c"::equals);
        final var devices = scanner.scan();
        devices.forEach(System.out::println);

        final var quectel = scanner.find(devices.get(0)).orElseThrow(RuntimeException::new);

    }


}
