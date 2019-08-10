package io.snice.usb.impl;

import io.snice.usb.UsbConfiguration;
import io.snice.usb.UsbTestBase;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
public class LinuxUsbScannerTest extends UsbTestBase {

    private UsbConfiguration config;
    private LinuxUsbScanner scanner;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();;
        // in our unit tests we'll use the saved dmesg.log
        // and instead of issuing the command dmesg and grep on it
        // we'll just do a cat instead...
        // TODO: this test will fail on systems that doesn't have cat, which makes it a bad
        // unit test...
        final var dmesgLog = findResource("dmesg_quectel.log");
        final var dmesgCmd = "cat " + dmesgLog.normalize().toString() + " | grep \"device number %s\"";

        final var lsusbLog = findResource("lsusb_quectel.log");
        final var lsusbCmd = "cat " + lsusbLog.normalize().toString();

        config = UsbConfiguration.of()
                .withDevicesPath(devicesPath)
                .withFindDeviceNoCmd(lsusbCmd)
                .withFindSysfsCmd(dmesgCmd).build();
        scanner = LinuxUsbScanner.of(config);
    }

    @Test
    public void testScan() throws Exception {
        scanner.scan();
    }

    @Test
    public void apa() throws Exception {
        // final var s = "[368992.707091] usb 1-2.4: new high-speed USB device number 67 using xhci_hcd";
        // final var s = "[221263.134136] usb 1-3: new high-speed USB device number 43 using xhci_hcd";
        final var lsUsbOutput = LinuxUsbScanner.executeShellCommandBlah("/bin/sh", "-c", String.format(config.getFindSysfsRoot(), "43"));
        System.out.println(lsUsbOutput);
        final var pattern = Pattern.compile("\\[.*\\] usb (\\d-\\d[\\.\\d]*): .*");
        final var matcher = pattern.matcher(lsUsbOutput.get());
        assertThat(matcher.matches(), is(true));
        assertThat(matcher.group(1), is("1-3"));
    }

    @Test
    public void testLoadDevice() throws Exception {
        scanner.createDevice(quectelDescriptor, "1-3", devicesPath);
    }

    @Test
    public void testLsUsbPatternMatching() throws Exception {
        final var device = "Bus 002 Device 002: ID 0451:8440 Texas Instruments, Inc";
        final var pattern = Pattern.compile("Bus.*ID\\s+(\\d+):(\\d+)\\s(.*)");
        final var matcher = pattern.matcher(device);
        if (matcher.matches()) {
            System.out.println(matcher.groupCount());
            System.out.println(matcher.group(1));
            System.out.println(matcher.group(2));
            System.out.println(matcher.group(3));
        }
    }

    @Test
    public void testForReal() throws Exception {
        config = UsbConfiguration.of().build();
        scanner = LinuxUsbScanner.of(config);

        System.out.println(scanner.scan());
    }

    @Test
    public void testFind() throws Exception {
        // note: the two log files are just dumps of dmesg and lsusb and must of course
        // contain whatever vendor and product id you'll choose... and the correct device number.
        final var dmesgLog = findResource("dmesg_quectel.log");
        final var dmesgCmd = "cat " + dmesgLog.normalize().toString() + " | grep \"device number %s\"";

        final var lsusbLog = findResource("lsusb_quectel.log");
        final var lsusbCmd = "cat " + lsusbLog.normalize().toString();

        config = UsbConfiguration.of()
                .withDevicesPath(devicesPath)
                .withFindDeviceNoCmd(lsusbCmd)
                .withFindSysfsCmd(dmesgCmd)
                .build();
        scanner = LinuxUsbScanner.of(config);

        scanner.find(quectelDescriptor);
    }
}
