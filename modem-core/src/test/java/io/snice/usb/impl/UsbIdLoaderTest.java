package io.snice.usb.impl;

import io.snice.usb.VendorDescriptor;
import io.snice.usb.VendorDeviceDescriptor;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class UsbIdLoaderTest {
    private Map<String, VendorDescriptor> vendors;

    @Test
    public void testLoad() throws Exception {
        vendors = UsbIdLoader.load();
        assertThat(vendors.size(), is(3010));

        // random checks that was manually verified
        var devs = ensureVendor("0001", "Fry's Electronics", 1);

        devs = ensureVendor("03f0", "HP, Inc", 522);
        devs = ensureVendor("03f1", "Genoa Technology", 0);

        devs = ensureVendor("1199", "Sierra Wireless, Inc.", 45);
        ensureDevice(devs, "6802", "MC8755 Device");

        devs = ensureVendor("2c7c", "Quectel Wireless Solutions Co., Ltd.", 7);
        ensureDevice(devs, "0125", "EC25 LTE modem");
        ensureDevice(devs, "0296", "BG96 CAT-M1/NB-IoT modem");

        // to verify a new do this:
        // On the command prompt;
        //
        // awk '/^[a-f,A-F,0-9]{4}(.*)/{print $1}' usb.ids > blah
        //
        // then load the blah file and you'll find out what awk think it is...
        /*
        final var url = UsbIdLoader.class.getClassLoader().getResource("blah").toURI();
        final var path = Path.of(url);
        Files.lines(path).forEach(line -> {
            if (!usbVendors.containsKey(line)) {
                System.err.println("Error: doesn't contain key " + line);
            }
        });
        */
    }

    private void ensureDevice(final Map<String, VendorDeviceDescriptor> devices, final String id, final String description) {
        final var device = devices.get(id);
        assertThat(device.getId(), is(id));
        assertThat(device.getDescription(), is(description));
    }

    private Map<String, VendorDeviceDescriptor> ensureVendor(final String id,
                                                             final String expectedName,
                                                             final int expectedDeviceCount) {
        final var vendor = vendors.get(id);
        assertThat(vendor.getId(), is(id));
        assertThat(vendor.getName(), is(expectedName));
        final var devices = vendor.getDevices();
        assertThat(devices.size(), is(expectedDeviceCount));
        return devices;
    }
}
