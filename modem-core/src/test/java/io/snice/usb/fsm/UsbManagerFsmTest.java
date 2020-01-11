package io.snice.usb.fsm;

import io.hektor.fsm.FSM;
import io.snice.modem.actors.fsm.FsmTestBase;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbScanner;
import io.snice.usb.event.Scan;
import io.snice.usb.event.Subscribe;
import io.snice.usb.fsm.support.Subscription;
import io.snice.usb.linux.LibUsbConfiguration;
import io.snice.usb.linux.LinuxDeviceId;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class UsbManagerFsmTest extends FsmTestBase<UsbManagerState, UsbManagerContext, UsbManagerData> {

    private FSM<UsbManagerState, UsbManagerContext, UsbManagerData> fsm;
    private UsbManagerContext ctx;
    private UsbManagerData data;
    private UsbScanner scanner;
    private LibUsbConfiguration libUsbConf;

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
        init(List.of(devices));
    }

    private void init(final List<UsbDeviceDescriptor> devices) {
        scanner = mockUsbScanner(devices);
        final var config = LibUsbConfiguration.of().build();
        ctx = mockContext(scanner, config);
        init(ctx);
    }

    private void init(final UsbManagerContext ctx) {
        data = new UsbManagerData();
        fsm = UsbManagerFsm.definition.newInstance("unit-test-123", ctx, data, unhandledEventHandler, this::onTransition);
        fsm.start();
    }

    private UsbDeviceDescriptor createUsbDeviceDescriptor(final String vendorId, final String productId, final int devAddress) {
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
        return mockContext(scanner, LibUsbConfiguration.of().build());
    }

    private UsbManagerContext mockContext(final UsbScanner scanner, final LibUsbConfiguration config) {
        final var ctx = mock(UsbManagerContext.class);
        return mockContext(ctx, scanner, config);
    }

    private UsbManagerContext mockContext(final UsbManagerContext ctx, final UsbScanner scanner, final LibUsbConfiguration config) {
        when(ctx.getScanner()).thenReturn(scanner);
        when(ctx.getScheduler()).thenReturn(scheduler);
        when(ctx.getConfig()).thenReturn(config);
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

    /**
     * Convenience method for kicking off a subscribe request flow/event.
     *
     * Mock the {@link Subscribe} request, ensure that the {@link UsbManagerContext} returns
     * a {@link Subscription} object for that {@link Subscribe} request and then call the state
     * machine...
     *
     * @return the {@link Subscription} object
     */
    private Subscription kickOffSubscribeRequest(final Predicate<UsbDeviceDescriptor>filter) {
        final var subscribeRequest = Subscribe.from("alice").withFilter(filter).build();
        final var subscription = mock(Subscription.class);
        when(ctx.createSubscription(subscribeRequest)).thenReturn(subscription);
        fsm.onEvent(subscribeRequest);
        return subscription;
    }

    private Subscription kickOffSubscribeRequest() {
        return kickOffSubscribeRequest(desc -> true);
    }

    private void assertNoDeviceAttachedCalled(final UsbManagerContext ctx) {
        verify(ctx, never()).deviceAttached(any(), anyList());
        verify(ctx, never()).deviceAttached(any(), any(Subscription.class));
    }

    /**
     * When you subscribe, the subscriber should be getting the list of already attached
     * devices, if there are none, then the subscriber should get nothing.
     */
    @Test
    public void testSubscribeNoDevices() {
        init();
        kickOffSubscribeRequest();

        // and since there were no usb devices attached, there should not be any
        // events for the subscriber saying so...
        assertNoDeviceAttachedCalled(ctx);
    }

    /**
     * If there already are devices attached when a {@link Subscribe} request is processed,
     * the subscriber should be getting events for those already existing devices.
     */
    @Test
    public void testSubscribeTwoDevices() {
        init(sierraWirelessMC8765, sierraWirelessMC8775);
        fsm.onEvent(Scan.SCAN); // so the two devices are discovered
        final var subscription = kickOffSubscribeRequest();

        verify(ctx).deviceAttached(sierraWirelessMC8765, subscription);
        verify(ctx).deviceAttached(sierraWirelessMC8775, subscription);
    }

    /**
     * Make sure that if we have a filter on the {@link Subscribe} request
     * that it does correctly filter out unwanted devices.
     *
     * In this case, we only want quectel and no Sierra Wireless
     * devices.
     */
    @Test
    public void testSubscribeWithFilter() {
        init(quectelEC25A, sierraWirelessMC8765, quectelBg96, sierraWirelessMC8775);
        fsm.onEvent(Scan.SCAN);

        final var subscription = kickOffSubscribeRequest(desc -> desc.getVendorId().equals("2c7c"));

        verify(ctx).deviceAttached(quectelEC25A, subscription);
        verify(ctx).deviceAttached(quectelBg96, subscription);

        // and make sure the sierra ones did NOT happen
        verify(ctx, never()).deviceAttached(sierraWirelessMC8765, subscription);
        verify(ctx, never()).deviceAttached(sierraWirelessMC8775, subscription);
    }

    /**
     * Whenever we scan for devices and find more, we need to ensure that
     * we only call {@link UsbManagerContext#deviceAttached(UsbDeviceDescriptor, Subscription)}
     * for the newly discovered modems.
     */
    @Test
    public void testScanDevicesAttaches() throws Exception {
        init(quectelEC25A);
        kickOffSubscribeRequest();
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceAttached(eq(quectelEC25A), anyList());

        // then scan again but setup the mocks to know include
        // another modem as well.
        mockUsbScanner(scanner, quectelEC25A, sierraWirelessMC8755);
        fsm.onEvent(Scan.SCAN);

        // note that we should not call deviceAttached on the same
        // modem again, hence we still only called the deviceAttached on the
        // EC25 once.
        verify(ctx, times(1)).deviceAttached(eq(quectelEC25A), anyList());

        // and this is the new one...
        verify(ctx).deviceAttached(eq(sierraWirelessMC8755), anyList());
    }

    /**
     * Ensure that we correctly detect devices being detached again.
     *
     * @throws Exception
     */
    @Test
    public void testScanDevicesDetaching() throws Exception {
        init(quectelEC25A, quectelBg96, sierraWirelessMC8755);
        kickOffSubscribeRequest();
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceAttached(eq(quectelEC25A), anyList());
        verify(ctx).deviceAttached(eq(quectelBg96), anyList());
        verify(ctx).deviceAttached(eq(sierraWirelessMC8755), anyList());

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
    public void testDeviceAttachDetachAttach() {
        init(quectelEC25A);
        kickOffSubscribeRequest();
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceAttached(eq(quectelEC25A), anyList());

        // empty list, hence, the EC25 detached again
        mockUsbScanner(scanner, List.of());
        fsm.onEvent(Scan.SCAN);
        verify(ctx).deviceDetached(quectelEC25A);

        // And the EC25 attaches again...
        mockUsbScanner(scanner, quectelEC25A);
        fsm.onEvent(Scan.SCAN);

        // so we have now (the mock has) seen the EC25 beting attached twice.
        verify(ctx, times(2)).deviceAttached(eq(quectelEC25A), anyList());

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

    /**
     * You can configure the scan period and as such, every X seconds we will initiate a new
     * scan of the USB devices attached to the machine. Ensure we honor the configuration
     * and that we also actually schedule a timer.
     */
    @Test
    public void testScanPeriodScheduled() {
        final var config = LibUsbConfiguration.of().withScanInterval(Duration.ofMillis(654)).build();
        final var ctx = mockContext(scanner, config);
        init(ctx);
        fsm.onEvent(Scan.SCAN);
        verify(scheduler).schedule(Scan.SCAN, Duration.ofMillis(654));
    }

}