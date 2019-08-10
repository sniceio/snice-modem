package io.snice.usb.impl;

import io.snice.usb.UsbTestBase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LinuxUsbInterfaceTest extends UsbTestBase {

    @Test
    public void testCreateUsbInterface() throws Exception {
        final var desc = LinuxUsbInterfaceDescriptor.of(devicesPath.resolve("1-3").resolve("1-3:1.1"));
        assertThat(desc.getUsbDevice(), is("1-3"));
        assertThat(desc.getUsbInterface(), is("1.1"));
    }
}
