package io.snice.usb.linux;

import io.snice.usb.VendorDescriptor;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

public class LinuxLibUsbScannerTest {

    private static Map<String, VendorDescriptor> knownUsbVendors;
    private LibUsbConfiguration conf;

    @BeforeClass
    public static void beforeClass() throws Exception {
        knownUsbVendors = UsbIdLoader.load();
    }

    @Before
    public void setUp() throws Exception {
        conf = LibUsbConfiguration.of().build();
    }


    @Test
    public void testScan() throws Exception {
        final var scanner = LinuxLibUsbScanner.of(conf, knownUsbVendors);
        scanner.scan("2c7c"::equals).forEach(dev -> {
            System.out.println(dev);
        });
    }
}