package io.snice.usb;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public interface UsbScanner {

    List<UsbDevice> scan() throws IOException, ExecutionException, InterruptedException;

    /**
     * Based on the {@link UsbDeviceDescriptor}, find the actual {@link UsbDevice}.
     *
     * @param descriptor
     * @return
     */
    Optional<UsbDevice> find(UsbDeviceDescriptor descriptor) throws IOException;
}
