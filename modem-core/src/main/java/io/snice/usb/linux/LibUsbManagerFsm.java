package io.snice.usb.linux;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.usb.UsbManagementEvent.TerminateEvent;
import io.snice.usb.fsm.UsbManagerContext;
import io.snice.usb.fsm.UsbManagerData;
import io.snice.usb.fsm.UsbManagerState;
import io.snice.usb.impl.LinuxUsbDeviceAttachEvent;
import io.snice.usb.impl.LinuxUsbDeviceDetachEvent;

import static io.snice.usb.fsm.UsbManagerState.ATTACH;
import static io.snice.usb.fsm.UsbManagerState.DETACH;
import static io.snice.usb.fsm.UsbManagerState.DMESG;
import static io.snice.usb.fsm.UsbManagerState.IDLE;
import static io.snice.usb.fsm.UsbManagerState.TERMINATED;

public class LibUsbManagerFsm {

    public static final Definition<UsbManagerState, UsbManagerContext, UsbManagerData> definition;

    static {
        final var builder = FSM.of(UsbManagerState.class)
                .ofContextType(UsbManagerContext.class)
                .withDataType(UsbManagerData.class);

        final var idle = builder.withInitialState(IDLE);
        final var dmesg = builder.withTransientState(DMESG);
        final var attach = builder.withTransientState(ATTACH);
        final var detach = builder.withTransientState(DETACH);
        final var terminated = builder.withFinalState(TERMINATED);

        idle.transitionTo(DMESG).onEvent(String.class).withAction(LibUsbManagerFsm::processDmesg);
        dmesg.transitionTo(IDLE).asDefaultTransition();

        idle.transitionTo(ATTACH).onEvent(LinuxUsbDeviceAttachEvent.class).withAction(LibUsbManagerFsm::onAttach).withGuard(LibUsbManagerFsm::processEvent);
        idle.transitionTo(IDLE).onEvent(LinuxUsbDeviceAttachEvent.class).withAction(evt -> System.err.println("Ignoring device"));

        idle.transitionTo(DETACH).onEvent(LinuxUsbDeviceDetachEvent.class).consume();

        idle.transitionTo(TERMINATED).onEvent(TerminateEvent.class);

        attach.transitionTo(IDLE).asDefaultTransition();
        detach.transitionTo(IDLE).asDefaultTransition();

        definition = builder.build();
    }

    /**
     * Process a line from DMESG. The entire Linux USB manager is relying to tailing dmesg and then reacting on
     * those messages. Hence, if there is a match, we'll just push that to the ctx that will deal with it.
     * In the scenario where this FSM is executing within an actor framework, this event will be pushed back
     * to the FSM.
     */
    private static void processDmesg(final String dmesg, final UsbManagerContext ctx, final UsbManagerData data) {
        ctx.getScanner().parseDmesg(dmesg)
                .filter(evt -> {
                    // ctx.getConfig().processDevice();
                    return true;
                }).ifPresent(ctx::processUsbEvent);
    }


    private static boolean processEvent(final LinuxUsbDeviceAttachEvent evt, final UsbManagerContext ctx, final UsbManagerData data) {
        return ctx.getConfig().processDevice(evt.getVendorId(), evt.getProductId());
    }

    private static final void onAttach(final LinuxUsbDeviceAttachEvent evt, final UsbManagerContext ctx, final UsbManagerData data) {
        System.err.println("Yeah, attaching...");
    }
}
