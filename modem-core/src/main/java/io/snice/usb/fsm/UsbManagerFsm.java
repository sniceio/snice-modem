package io.snice.usb.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.usb.UsbManagementEvent.TerminateEvent;
import io.snice.usb.event.Scan;
import io.snice.usb.event.Subscribe;

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

        idle.transitionTo(SCAN).onEvent(Scan.class).withAction(UsbManagerFsm::scan);
        idle.transitionTo(IDLE).onEvent(Subscribe.class).withAction(UsbManagerFsm::subscribe);
        scan.transitionTo(IDLE).asDefaultTransition();

        idle.transitionTo(TERMINATED).onEvent(TerminateEvent.class);

        definition = builder.build();
    }

    /**
     * Process a {@link Subscribe} request and also send over all the known devices at this point in
     * time.
     */
    private static void subscribe(final Subscribe request, final UsbManagerContext ctx, final UsbManagerData data) {
        final var subscription = ctx.createSubscription(request);
        data.addSubscription(subscription);
        data.getAvailableDevices().stream().filter(request::accept).forEach(dev -> {
            ctx.deviceAttached(dev, subscription);
        });
    }

    private static void onScanExit(final UsbManagerContext ctx, final UsbManagerData data) {
        ctx.getScheduler().schedule(Scan.SCAN, ctx.getConfig().getScanInterval());
    }

    private static void scan(final Scan event, final UsbManagerContext ctx, final UsbManagerData data) {
        final var devices = ctx.getScanner().scan(ctx.getConfig()::processDevice).stream().collect(Collectors.toList());
        final var added = devices.stream().filter(data::isUnKnownDevice).collect(Collectors.toList());
        final var removed = data.getAvailableDevices().stream().filter(dev -> !devices.contains(dev)).collect(Collectors.toList());

        added.forEach(dev -> {
            data.deviceAttached(dev);
            if (data.hasSubscriptions()) {
                ctx.deviceAttached(dev, data.getSubscriptions());
            }
        });

        removed.forEach(dev -> {
            data.deviceDetached(dev);
            ctx.deviceDetached(dev);
        });
    }

}
