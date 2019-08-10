package io.snice.usb.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.usb.UsbEvent;
import io.snice.usb.UsbEvent.UsbAttachEvent;
import io.snice.usb.UsbManagementEvent.ScanEvent;
import io.snice.usb.UsbManagementEvent.TerminateEvent;

import java.io.IOException;

import static io.snice.usb.fsm.UsbManagerState.ATTACH;
import static io.snice.usb.fsm.UsbManagerState.DETACH;
import static io.snice.usb.fsm.UsbManagerState.IDLE;
import static io.snice.usb.fsm.UsbManagerState.SCAN;
import static io.snice.usb.fsm.UsbManagerState.TERMINATED;

public class UsbManagerFsm {

    public static final Definition<UsbManagerState, UsbManagerContext, UsbManagerData> definition;

    static {
        final var builder = FSM.of(UsbManagerState.class)
                .ofContextType(UsbManagerContext.class)
                .withDataType(UsbManagerData.class);

        final var idle = builder.withInitialState(IDLE);
        final var scan = builder.withTransientState(SCAN);
        final var attach = builder.withTransientState(ATTACH);
        final var detach = builder.withTransientState(DETACH);
        final var terminated = builder.withFinalState(TERMINATED);

        idle.transitionTo(SCAN).onEvent(ScanEvent.class);

        idle.transitionTo(ATTACH).onEvent(UsbAttachEvent.class).withAction(UsbManagerFsm::onAttach).withGuard(UsbManagerFsm::processEvent);
        idle.transitionTo(IDLE).onEvent(UsbAttachEvent.class).consume();

        idle.transitionTo(DETACH).onEvent(UsbEvent.UsbDetachEvent.class);
        idle.transitionTo(TERMINATED).onEvent(TerminateEvent.class);

        scan.transitionTo(IDLE).asDefaultTransition();
        attach.transitionTo(IDLE).asDefaultTransition();
        detach.transitionTo(IDLE).asDefaultTransition();

        definition = builder.build();
    }

    private static boolean processEvent(final UsbAttachEvent evt, final UsbManagerContext ctx, final UsbManagerData data) {
        final var desc = evt.getUsbDeviceDescriptor();
        return ctx.getConfig().processDevice(desc.getVendorId(), desc.getDeviceId());
    }

    private static final void onAttach(final UsbAttachEvent evt, final UsbManagerContext ctx, final UsbManagerData data) {
        try {
            final var descriptor = evt.getUsbDeviceDescriptor();
            final var device = ctx.getUsbScanner().find(descriptor);
            device.ifPresent(dev -> ctx.deviceAttached(dev));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
