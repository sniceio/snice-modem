package io.snice.usb.linux;

import io.snice.usb.VendorDescriptor;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        final var devices = scanner.scan("2c7c"::equals).stream()
                .map(scanner::find).flatMap(Optional::stream).collect(Collectors.toList());
        devices.forEach(device -> {
            System.out.println(device);
        });
    }
}