package io.snice.usb;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface UsbInterfaceDescriptor {

    List<UsbEndpointDescriptor> getEndpoints();
    Optional<Path> getSerialDevicePath();
}
