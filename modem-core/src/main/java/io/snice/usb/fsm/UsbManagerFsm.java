package io.snice.usb.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.usb.UsbDevice;
import io.snice.usb.UsbDeviceDescriptor;
import io.snice.usb.UsbManagementEvent.TerminateEvent;
import io.snice.usb.linux.LinuxUsbDeviceDescriptor;

import java.util.stream.Collectors;

import static io.snice.usb.fsm.UsbManagerState.IDLE;
import static io.snice.usb.fsm.UsbManagerState.SCAN;
import static io.snice.usb.fsm.UsbManagerState.TERMINATED;

public class UsbManagerFsm {

    public static final Definition<UsbManagerState, UsbManagerContext, UsbManagerData> definition;

    private static final String SCAN_TIMEOUT = "SCAN TIMEOUT";

    static {
        final var builder = FSM.of(UsbManagerState.class)
                .ofContextType(UsbManagerContext.class)
                .withDataType(UsbManagerData.class);

        final var idle = builder.withInitialState(IDLE);
        final var scan = builder.withTransientState(SCAN).withExitAction(UsbManagerFsm::onScanExit);
        final var terminated = builder.withFinalState(TERMINATED);

        idle.transitionTo(SCAN).onEvent(String.class).withGuard("SCAN"::equals).withAction(UsbManagerFsm::scan);
        idle.transitionTo(SCAN).onEvent(String.class).withGuard(SCAN_TIMEOUT::equals).withAction(UsbManagerFsm::scan);
        scan.transitionTo(IDLE).asDefaultTransition();

        idle.transitionTo(TERMINATED).onEvent(TerminateEvent.class);

        definition = builder.build();
    }

    private static void onScanExit(final UsbManagerContext ctx, final UsbManagerData data) {
        System.err.println("onScanExit");
        // ctx.getScheduler().schedule(() -> SCAN_TIMEOUT, ctx.getConfig().getScanInterval());
    }

    private static void scan(final String event, final UsbManagerContext ctx, final UsbManagerData data) {
        final var devices = ctx.getScanner().scan(ctx.getConfig()::processDevice).stream().map(dev -> (LinuxUsbDeviceDescriptor)dev).collect(Collectors.toList());
        final var deviceIds = devices.stream().map(UsbDeviceDescriptor::getId).collect(Collectors.toList());
        final var currentDevices = data.getDevices().stream().map(UsbDevice::getId).collect(Collectors.toList());

        final var addDevices = deviceIds.stream().filter(id -> !currentDevices.contains(id)).collect(Collectors.toList());
        final var delDevices = currentDevices.stream().filter(id -> !deviceIds.contains(id)).collect(Collectors.toList());

        System.err.println("==== Added devicies: ");
        addDevices.forEach(System.err::println);

        System.err.println("==== Removed devicies: ");
        delDevices.forEach(System.err::println);

    }

}
