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
        var config = LibUsbConfiguration.of().build();
        var scanner = LinuxLibUsbScanner.of(config, knownUsbVendors);
        scanner.scan();
    }


}
