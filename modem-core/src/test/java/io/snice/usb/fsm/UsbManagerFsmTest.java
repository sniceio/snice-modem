package io.snice.usb.fsm;

import io.hektor.fsm.FSM;
import io.snice.modem.actors.fsm.FsmTestBase;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbScanner;
import io.snice.usb.event.Scan;
import io.snice.usb.linux.LibUsbConfiguration;
import io.snice.usb.linux.LinuxDeviceId;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class UsbManagerFsmTest extends FsmTestBase<UsbManagerState, UsbManagerContext, UsbManagerData> {

    private FSM<UsbManagerState, UsbManagerContext, UsbManagerData> fsm;
    private UsbManagerContext ctx;
    private UsbManagerData data;
    private UsbScanner scanner;

    private UsbDeviceDescriptor quectelBg96;
    private UsbDeviceDescriptor quectelEC25A;

    private UsbDeviceDescriptor sierraWirelessMC8755;
    private UsbDeviceDescriptor sierraWirelessMC8765;
    private UsbDeviceDescriptor sierraWirelessMC8775;

    @Before
    public void setUp() throws Exception {
        super.setup();

        // create a few devices that we will use for adding/removing
        // devices from the "system"
        quectelBg96 = createUsbDeviceDescriptor("2c7c", "0296", 2);
        quectelEC25A = createUsbDeviceDescriptor("2c7c", "0125", 3);

        sierraWirelessMC8755 = createUsbDeviceDescriptor("1199", "6802", 4);
        sierraWirelessMC8765 = createUsbDeviceDescriptor("1199", "6803", 5);
        sierraWirelessMC8775 = createUsbDeviceDescriptor("1199", "6812", 6);

        init(quectelBg96, quectelEC25A);
    }

    /**
     * Convenience method for initializing the usb sub system mocks to return
     * the given set of devices.
     *
     */
    private void init(final UsbDeviceDescriptor... devices) {
        scanner = mockUsbScanner(List.of(devices));
        ctx = mockContext(scanner);
        init(ctx);
    }

    private UsbDeviceDescriptor createUsbDeviceDescriptor(final String productId, final String vendorId, final int devAddress) {
        final var devDesc = mock(UsbDeviceDescriptor.class);
        final var devId = LinuxDeviceId.withUsbSysfs("/tmp").withBusNo(2).withDeviceAddress(devAddress).isRootHub(false);
        when(devDesc.getProductId()).thenReturn(productId);
        when(devDesc.getVendorId()).thenReturn(vendorId);
        when(devDesc.getInterfaces()).thenReturn(List.of());
        when(devDesc.getVendorDescription()).thenReturn(Optional.of("Unit test made up USB Device"));
        when(devDesc.getId()).thenReturn(devId);
        return devDesc;

    }

    private UsbManagerContext mockContext(final UsbScanner scanner) {
        final var ctx = mock(UsbManagerContext.class);
        return mockContext(ctx, scanner);
    }

    private UsbManagerContext mockContext(final UsbManagerContext ctx, final UsbScanner scanner) {
        when(ctx.getScanner()).thenReturn(scanner);
        when(ctx.getScheduler()).thenReturn(scheduler);
        final var libUsbConf = LibUsbConfiguration.of().build();
        when(ctx.getConfig()).thenReturn(libUsbConf);
        return ctx;
    }

    /**
     * Mocking the underlying {@link UsbScanner}, which is a crucial part of the overall usb sub-system.
     * By changing the devices that are "found" when scanning the system, you will control most
     * of the behavior of the {@link UsbManagerFsm}.
     *
     * @return
     */
    private UsbScanner mockUsbScanner(final List<UsbDeviceDescriptor> devices) {
        final var usbScanner = mock(UsbScanner.class);
        return mockUsbScanner(usbScanner, devices);
    }

    private UsbScanner mockUsbScanner(final UsbScanner scanner, final List<UsbDeviceDescriptor> devices) {
        when(scanner.scan((Predicate)any())).thenReturn(devices);
        when(scanner.scan((BiPredicate)any())).thenReturn(devices);
        return scanner;
    }

    private UsbScanner mockUsbScanner(final UsbScanner scanner, final UsbDeviceDescriptor... devices) {
        return mockUsbScanner(scanner, List.of(devices));
    }

    private void init(final UsbManagerContext ctx) {
        data = new UsbManagerData();
        fsm = UsbManagerFsm.definition.newInstance("unit-test-123", ctx, data, unhandledEventHandler, this::onTransition);
        fsm.start();
    }

    /**
     * Whenever we scan for devices and find more, we need to ensure that
     * we only call {@link UsbManagerContext#deviceAttached(UsbDeviceDescriptor)}
     * for the newly discovered modems.
     */
    @Test
    public void testScanDevicesAttaches() throws Exception {
        init(quectelEC25A);
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceAttached(quectelEC25A);

        // then scan again but setup the mocks to know include
        // another modem as well.
        mockUsbScanner(scanner, quectelEC25A, sierraWirelessMC8755);
        fsm.onEvent(Scan.SCAN);

        // note that we should not call deviceAttached on the same
        // modem again, hence we still only called the deviceAttached on the
        // EC25 once.
        verify(ctx, times(1)).deviceAttached(quectelEC25A);

        // and this is the new one...
        verify(ctx).deviceAttached(sierraWirelessMC8755);
    }

    /**
     * Ensure that we correctly detect devices being detached again.
     *
     * @throws Exception
     */
    @Test
    public void testScanDevicesDetaching() throws Exception {
        init(quectelEC25A, quectelBg96, sierraWirelessMC8755);
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceAttached(quectelEC25A);
        verify(ctx).deviceAttached(quectelBg96);
        verify(ctx).deviceAttached(sierraWirelessMC8755);

        // now setup the mocked scanner to only return the BG96
        // and as such, the other ones should have been
        // detected as removed...
        mockUsbScanner(scanner, quectelBg96);

        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceDetached(quectelEC25A);
        verify(ctx).deviceDetached(sierraWirelessMC8755);
    }

    /**
     * Ensure that we correctly detect the device being attached, then
     * detatched and then attached again.
     * @throws Exception
     */
    @Test
    public void testDeviceAttachDetachAttach() throws Exception {
        init(quectelEC25A);
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceAttached(quectelEC25A);

        // empty list, hence, the EC25 detached again
        mockUsbScanner(scanner, List.of());
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceDetached(quectelEC25A);

        // And the EC25 attaches again...
        mockUsbScanner(scanner, quectelEC25A);
        fsm.onEvent(Scan.SCAN);

        // so we have now (the mock has) seen the EC25 beting attached twice.
        verify(ctx, times(2)).deviceAttached(quectelEC25A);

        // and for good measure, it was only (still) detached once
        // note that times(1) is actually by default but just making
        // it super explicit here...
        verify(ctx, times(1)).deviceDetached(quectelEC25A);
    }

    /**
     * Simple test where we find no attached USB devices.
     *
     */
    @Test
    public void testScanNoDevicesAdded() throws Exception {
        // mock the scanner to return zero USB devices.
        scanner = mockUsbScanner(List.of());
        ctx = mockContext(scanner);
        init(ctx);

        fsm.onEvent(Scan.SCAN);
    }

}