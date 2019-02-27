package io.snice.modem.actors.fsm;

import io.hektor.actors.io.StreamToken;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.builder.FSMBuilder;
import io.hektor.fsm.builder.StateBuilder;
import io.snice.buffer.Buffer;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.events.ModemDisconnect;
import io.snice.modem.actors.events.ModemEvent;
import io.snice.modem.actors.events.ModemReset;

import java.util.Optional;

public class FirmwareFsm {

    public static final Definition<FirmwareState, FirmwareContext, FirmwareData> definition;

    static {
        final FSMBuilder<FirmwareState, FirmwareContext, FirmwareData> builder =
                FSM.of(FirmwareState.class).ofContextType(FirmwareContext.class).withDataType(FirmwareData.class);

        stateDefinitionsReady(builder.withInitialState(FirmwareState.READY));
        stateDefinitionsReset(builder.withTransientState(FirmwareState.RESET));

        // may need a transient state which is PROCESS_INPUT and that checks
        // if we'll wait for more input from the modem or if we go back to
        // ready... Is then READY also a transiet state because if we have oustanding commands
        // we are going to need to handle them.... or do we actually push that up to the
        // modem instead.
        stateDefinitionsInput(builder.withState(FirmwareState.INPUT));
        stateDefinitionsTerminated(builder.withFinalState(FirmwareState.TERMINATED));
        definition = builder.build();
    }

    /**
     * In the ready state we're just waiting for two main events, a command to write to the
     * modem or a reset command, which forces us into the reset loop.
     *
     * In the ready state we are just waiting for someone to give us a command to write
     * to the modem, which we'll do by giving the command to the Input state that will,
     * on its enter action, write it to the modem (by using the context object, which, depending
     * on implementation, will give it to the actor representing the inputstream of the modem.
     *
     * @param ready
     */
    private static void stateDefinitionsReady(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> ready) {
        ready.transitionTo(FirmwareState.RESET).onEvent(ModemReset.class).withGuard(FirmwareFsm::isConfiguredWithResetCommands);
        ready.transitionTo(FirmwareState.INPUT).onEvent(AtCommand.class).withAction(FirmwareFsm::writeToModem);
        ready.transitionTo(FirmwareState.TERMINATED).onEvent(ModemDisconnect.class);
    }

    private static void stateDefinitionsReset(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> reset) {
        reset.withEnterAction(FirmwareFsm::onEnterReset);
        reset.transitionTo(FirmwareState.INPUT)
                .onEvent(ModemReset.class)
                .withGuard((r, ctx, data) -> data.isResetting())
                .withAction(FirmwareFsm::sendResetCommand);


        // last resort transition, which is mandatory and will bring us back
        // into the READY state. This means that we have either no reset commands to send
        // or we have sent them all and as such, we should move back to READY
        reset.transitionTo(FirmwareState.READY).asDefaultTransition();
    }

    private static void onEnterReset(final FirmwareContext ctx, final FirmwareData data){
        // new reset cycle
        if (!data.isResetting()) {
            data.resetTheResetCommands(ctx.getConfiguration().getResetCommands());
            data.isResetting(true);
        }
    }

    private static void sendResetCommand(final ModemEvent ignore, final FirmwareContext ctx, final FirmwareData data) {
        final Optional<AtCommand> optional = data.getNextResetCommand();
        data.isResetting(optional.isPresent());
        optional.ifPresent(cmd -> writeToModem(cmd, ctx, data));
    }

    private static void writeToModem(final AtCommand cmd, final FirmwareContext ctx, final FirmwareData data) {
        data.setCurrentCommand(cmd);
        ctx.writeToModem(cmd);
    }

    /**
     * While in the input state we are waiting for the modem to return data to us, which we'll put together
     * into a, hopefully, final response that will then be "giving" back to the caller.
     *
     * @param input
     */
    private static void stateDefinitionsInput(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> input) {
        input.transitionTo(FirmwareState.INPUT).onEvent(StreamToken.class).withAction(FirmwareFsm::processStreamToken);
        input.transitionTo(FirmwareState.INPUT).onEvent(AtCommand.class).withAction((cmd, ctx, data) -> data.stashCommand(cmd));

        // if we are in the RESET flow then upon a final AT response
        // we'll go back to the REST state again since it may want to send more
        // AT commands to have the modem back into a good known state.
        input.transitionTo(FirmwareState.RESET).onEvent(AtResponse.class).withGuard((r, ctx, data) -> data.isResetting());

        // if we are not in resetting, then we go back to READY
        input.transitionTo(FirmwareState.READY).onEvent(AtResponse.class);
    }

    private static void stateDefinitionsTerminated(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> terminated) {
    }

    private static boolean isConfiguredWithResetCommands(final ModemReset ignore, final FirmwareContext ctx, final FirmwareData data){
        return ctx.getConfiguration().hasResetCommands();
    }

    private static void processStreamToken(final StreamToken token, final FirmwareContext ctx, final FirmwareData data) {
        final Buffer buffer = token.getBuffer();
        final Optional<ItuResultCodes> optional = ctx.getConfiguration().matchResultCode(buffer);
        if (optional.isPresent()) {
            final ItuResultCodes code = optional.get();
            if (code.getCode().isFinal()) {
                final AtCommand cmd = data.consumeCurrentCommand();
                final AtResponse response = AtResponse.of(cmd, data.consumeAllData());
                ctx.processResponse(response);
            } else {
                data.stashBuffer(buffer);
            }
        } else {
            data.stashBuffer(buffer);
        }
    }

}
