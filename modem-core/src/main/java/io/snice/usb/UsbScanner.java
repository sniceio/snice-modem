package io.snice.usb;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public interface UsbScanner {

    /**
     * Scan the system for all connected USB devices.
     *
     * This is the same as calling {@link #scan(BiPredicate)} with a filter that always returns true.
     *
     * @return a list of {@link UsbDeviceDescriptor} that represents the currently connected devices.
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    List<UsbDeviceDescriptor> scan() throws UsbException;

    /**
     * Scan the system for all connected USB devices that is matching the given filter.
     *
     * E.g., in order to list only the devices that matches Quectel (vendor id: 2c7c), do:
     * <pre>
     *     var scanner = ...; // initialize scanner
     *     var filter = vendorId -> "2c7c".equals(vendorId);
     *     var quectelDevices = scanner.scan(filter);
     * </pre>
     *
     * @param vendorFilter a filter that can be used to match against a given vendor ID.
     * @return a list of {@link UsbDeviceDescriptor}s that are matching the given vendor filter.
     */
    List<UsbDeviceDescriptor> scan(Predicate<String> vendorFilter) throws UsbException;

    /**
     * Scan the system for all connected USB devices that is matching the given filter.
     *
     * @param vendorProductFilter a filter that can be used to match against a given vendor and product ID.
     * @return a list of {@link UsbDeviceDescriptor}s that are matching the given vendor filter.
     */
    List<UsbDeviceDescriptor> scan(BiPredicate<String, String> vendorProductFilter) throws UsbException;

    /**
     * Based on the {@link UsbDeviceDescriptor}, find the actual {@link UsbDevice}.
     *
     * Note: even though perhaps {@link #scan()} returned a device it may have been removed before
     * you get around to actually lookup the details of that device.
     *
     * @param descriptor
     * @return
     */
    Optional<UsbDevice> find(UsbDeviceDescriptor descriptor) throws UsbException;
}
