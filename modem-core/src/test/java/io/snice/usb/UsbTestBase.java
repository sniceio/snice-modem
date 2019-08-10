package io.snice.usb;

import org.junit.Before;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsbTestBase {

    protected Path devicesPath;
    protected UsbConfiguration config;

    protected UsbDeviceDescriptor quectelDescriptor;
    protected UsbDeviceDescriptor sierraDescriptor;

    @Before
    public void setup() throws Exception {
        devicesPath = findResource("devices");

        config = UsbConfiguration.of().build();

        // Quectel BG96
        quectelDescriptor = mock(UsbDeviceDescriptor.class);
        when(quectelDescriptor.getVendorId()).thenReturn("2c7c");
        when(quectelDescriptor.getDeviceId()).thenReturn("0296");

        // Sierra Wireless MC7455
        when(quectelDescriptor.getVendorId()).thenReturn("1199");
        when(quectelDescriptor.getDeviceId()).thenReturn("9071");
        sierraDescriptor = mock(UsbDeviceDescriptor.class);
    }


    public Path findResource(final String resource) throws URISyntaxException {
        final var url = UsbTestBase.class.getClassLoader().getResource(resource);
        return Path.of(url.toURI());
    }
}
