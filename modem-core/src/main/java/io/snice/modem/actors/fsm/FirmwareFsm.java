package io.snice.modem.actors.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.builder.FSMBuilder;
import io.hektor.fsm.builder.StateBuilder;
import io.snice.modem.actors.events.AtResponse;

public class FirmwareFsm {

    private static final Definition<FirmwareState, FirmwareContext, FirmwareData> definition;

    static {

            final FSMBuilder<FirmwareState, FirmwareContext, FirmwareData> builder =
                    FSM.of(FirmwareState.class).ofContextType(FirmwareContext.class).withDataType(FirmwareData.class);

            stateDefinitionsInput(builder.withInitialState(FirmwareState.RESET));
            // stateDefinitionsConnecting(builder.withInitialState(ModemState.CONNECTING));
            // stateDefinitionsConnected(builder.withState(ModemState.CONNECTED));
            // stateDefinitionsDisconnecting(builder.withState(ModemState.DISCONNECTING));
            // stateDefinitionsTerminated(builder.withFinalState(ModemState.TERMINATED));

            definition = builder.build();
    }

    private static void stateDefinitionsReset(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> reset) {
    }

    /**
     * In the ready state we are just waiting for someone to give us a command to write
     * to the modem, which we'll do by giving the command to the Input state that will,
     * on its enter action, write it to the modem (by using the context object, which, depending
     * on implementation, will give it to the actor representing the inputstream wo the modem.
     *
     * @param ready
     */
    private static void stateDefinitionsReady(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> ready) {


    }

    /**
     * While in the input state we are waiting for the modem to return data to us, which we'll put together
     * into a, hopefully, final response that will then be "giving" back to the caller.
     *
     * @param input
     */
    private static void stateDefinitionsInput(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> input) {

        // if we are in the RESET flow then upon a final AT response
        // we'll go back to the REST state again since it may want to send more
        // AT commands to have the modem back into a good known state.
        input.transitionTo(FirmwareState.RESET).onEvent(AtResponse.class).withGuard((r, ctx, data) -> data.isResetting());

        // if we are not in resetting, then we go back to READY
        input.transitionTo(FirmwareState.READY).onEvent(AtResponse.class);
    }

    private static void stateDefinitionsTerminated(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> terminated) {
    }

}
