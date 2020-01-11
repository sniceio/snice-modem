package io.snice.modem.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.snice.modem.event.ClaimModem;
import io.snice.modem.event.ManagementEvent.List;
import io.snice.usb.event.UsbEvent;

import static io.snice.modem.fsm.ModemManagerState.DEVICE_ATTACHED;
import static io.snice.modem.fsm.ModemManagerState.RUNNNG;
import static io.snice.modem.fsm.ModemManagerState.TERMINATED;

public class ModemManagerFsm {

    public static final Definition<ModemManagerState, ModemManagerContext, ModemManagerData> definition;

    static {
        final var builder = FSM.of(ModemManagerState.class)
                .ofContextType(ModemManagerContext.class)
                .withDataType(ModemManagerData.class);

        final var running = builder.withInitialState(RUNNNG);
        final var attached = builder.withTransientState(DEVICE_ATTACHED);
        final var terminated = builder.withFinalState(TERMINATED);

        running.withInitialEnterAction((ctx, data) -> ctx.subscribe());

        running.transitionTo(DEVICE_ATTACHED).onEvent(UsbEvent.class)
                .withGuard(UsbEvent::isDeviceAttach)
                .withAction((evt, ctx, data) -> data.storeModem(evt.getUsbDeviceDescriptor()));

        running.transitionTo(RUNNNG).onEvent(ClaimModem.class).withAction(ModemManagerFsm::onClaimModem);
        running.transitionTo(RUNNNG)
                .onEvent(List.class)
                .withAction((evt, ctx, data) -> {
                    final var response = evt.createResponse().withModems(data.getAllAvailableModems()).build();
                    ctx.reply(response);
                });

        attached.transitionTo(RUNNNG).asDefaultTransition();

        running.transitionTo(TERMINATED).onEvent(String.class);

        definition = builder.build();
    }

    /**
     * When someone tries to claim the modem we will have to:
     *
     * <ol>
     *     <li>Make sure that no one else have claimed it, if so, it has to be released first
     *     TODO: may allow for forcing it... </li>
     *     <li></li>
     *     <li></li>
     * </ol>
     * @param event
     * @param ctx
     * @param data
     */
    private static void onClaimModem(final ClaimModem event, final ModemManagerContext ctx, final ModemManagerData data) {
        // ctx.reply();
    }


}
