package io.snice.modem.actors.fsm;

import io.hektor.actors.io.StreamToken;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.builder.StateBuilder;
import io.snice.buffer.Buffer;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.events.ModemDisconnect;
import io.snice.modem.actors.events.ModemReset;

import java.util.Optional;

public class FirmwareFsm {

    public static final Definition<FirmwareState, FirmwareContext, FirmwareData> definition;

    static {
        final var builder = FSM.of(FirmwareState.class).ofContextType(FirmwareContext.class).withDataType(FirmwareData.class);

        stateDefinitionsReady(builder.withInitialState(FirmwareState.READY));
        stateDefinitionsReset(builder.withTransientState(FirmwareState.RESET));
        stateDefinitionWait(builder.withState(FirmwareState.WAIT));
        stateDefinitionProcessing(builder.withTransientState(FirmwareState.PROCESSING));
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
     * on implementation, will give it to the actor representing the inputstream success the modem.
     *
     * @param ready
     */
    private static void stateDefinitionsReady(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> ready) {

        // if we are asked to RESET the modem we will enter the reset loop, assuming
        // we have actually configured what AT commands we should use to reset ourselves.
        // If no "reset" commands have been configured, then just ignore it...
        ready.transitionTo(FirmwareState.RESET).onEvent(ModemReset.class).withGuard(FirmwareFsm::isConfiguredWithResetCommands);
        ready.transitionTo(FirmwareState.READY).onEvent(ModemReset.class).consume();

        ready.transitionTo(FirmwareState.WAIT).onEvent(AtCommand.class).withAction(FirmwareFsm::writeToModem);
        ready.transitionTo(FirmwareState.TERMINATED).onEvent(ModemDisconnect.class);
    }

    private static void stateDefinitionsReset(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> reset) {
        reset.withEnterAction(FirmwareFsm::onEnterReset);

        reset.transitionTo(FirmwareState.WAIT)
                .onEvent(ModemReset.class)
                .withGuard((r, ctx, data) -> data.isResetting())
                .withAction(FirmwareFsm::sendResetCommand);

        // this is because when we go from PROCESSING to RESET again we will still carry
        // the StreamToken event class. We don't necessarily care about that one since
        // it should have been converted into a AtResponse but Hektor doesn't support
        // that right now...
        reset.transitionTo(FirmwareState.WAIT)
                .onEvent(StreamToken.class)
                .withGuard((r, ctx, data) -> data.isResetting())
                .withAction(FirmwareFsm::sendResetCommand);


        // Once we are done sending all reset commands, we'll transition back to READY.
        reset.transitionTo(FirmwareState.READY).asDefaultTransition();
    }

    /**
     * When we enter the RESET state we are either already in a RESET cycle or we are about to start one over.
     *
     * Remember that the RESET state is transient so we will transition to something
     * else right away, which is either the WAIT or the READY state.
     */
    private static void onEnterReset(final FirmwareContext ctx, final FirmwareData data){
        // new reset cycle, assuming we are configured with any reset
        // commands at all. If we are not, we'll go back to the READY state
        if (!data.isResetting() && ctx.getConfiguration().hasResetCommands()) {
            data.resetTheResetCommands(ctx.getConfiguration().getResetCommands());
            data.isResetting(true);
        } else if (!data.hasMoreResetCommands()) {
            data.isResetting(false);
        }

        // TODD: we should stash this away and then perhaps return a RESET result
        // back to the caller eventually. For now we'll just consume it...
        data.consumeResponse();

    }

    private static void sendResetCommand(final Object ignore, final FirmwareContext ctx, final FirmwareData data) {
        final var optional = data.getNextResetCommand();
        data.isResetting(optional.isPresent());
        optional.ifPresent(cmd -> writeToModem(cmd, ctx, data));
    }

    private static void writeToModem(final AtCommand cmd, final FirmwareContext ctx, final FirmwareData data) {
        final var timeout = ctx.getConfiguration().getCommandConfiguration().getTimeout(cmd);
        ctx.getScheduler().schedule(() -> "timeout for " + cmd.getTransactionId(), timeout);

        data.setCurrentCommand(cmd);
        ctx.writeToModem(cmd);
    }

    /**
     * While in the WAIT state we are waiting for the modem to return data to us, which we'll put together
     * into a, hopefully, final response that will then be "giving" back to the caller.
     *
     * @param wait
     */
    private static void stateDefinitionWait(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> wait) {

        // TODO: we need to setup timeouts for this state depending on the command.
        // TODO: e.g. the AT+COPS=? will take some time but ATI will not.
        // TODO: if there is a timeout then we should

        // TODO: perhaps enchance Hektor so that the onEnterAction could also optionally
        // TODO: get the event that took it to that state. That way we don't have to save it
        // TODO: away like this...
        wait.transitionTo(FirmwareState.PROCESSING).onEvent(StreamToken.class).withAction((token, ctx, data) -> data.saveStreamToken(token));
        wait.transitionTo(FirmwareState.WAIT).onEvent(AtCommand.class).withAction((cmd, ctx, data) -> data.stashCommand(cmd));
    }

    /**
     * While in the processing state we are just waiting for data from the modem and if that data now completes
     * the oustanding command then we have two choices, go to the RESET state (if we are in the reset loop) or back
     * too READY.
     *
     * @param processing
     */
    private static void stateDefinitionProcessing(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> processing) {
        processing.withEnterAction(FirmwareFsm::processStreamToken);

        // TODO: when we go back to the RESET state it would be nice to transform that
        // TODO: stream token to an AT response. Perhaps some onEvent(..).withGuard(...).transform(evt -> evt.blah);
        // TODO: and then when we invoke the, in this case, the RESET state, you'll get the transformed event.
        // TODO: as it is now, the RESET state has to consume the outstanding
        processing.transitionTo(FirmwareState.RESET).onEvent(StreamToken.class).withGuard((token, ctx, data) -> data.hasFinalResponse() && data.isResetting());

        // if we have stashed commands we have to send those out
        // and once again end up in the WAIT <--> PROCESSING loop
        processing.transitionTo(FirmwareState.WAIT)
                .onEvent(StreamToken.class)
                .withGuard((token, ctx, data) -> data.hasFinalResponse() && data.hasStashedCommands())
                .withAction((token, ctx, data) -> {
                    ctx.processResponse(data.consumeResponse());
                    data.getNextStashedCommand().ifPresent(cmd -> {
                        data.setCurrentCommand(cmd);
                        ctx.writeToModem(cmd);
                    });
                });

        processing.transitionTo(FirmwareState.READY)
                .onEvent(StreamToken.class)
                .withGuard((token, ctx, data) -> data.hasFinalResponse())
                .withAction((token, ctx, data) -> ctx.processResponse(data.consumeResponse()));

        processing.transitionTo(FirmwareState.WAIT).asDefaultTransition();
    }

    private static void stateDefinitionsTerminated(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> terminated) {
    }

    private static boolean isConfiguredWithResetCommands(final ModemReset ignore, final FirmwareContext ctx, final FirmwareData data){
        return ctx.getConfiguration().hasResetCommands();
    }

    private static void processStreamToken(final FirmwareContext ctx, final FirmwareData data) {

        // TODO: as mentioned earlier, would be nicer if we got the actual event here as well so we
        // TODO:  don't have to save it away and then "consumeSreamToken". Kind of annoying
        final StreamToken token = data.consumeStreamToken();
        final Buffer buffer = token.getBuffer();
        final Optional<ItuResultCodes> optional = ctx.getConfiguration().matchResultCode(buffer);
        if (optional.isPresent()) {
            final ItuResultCodes code = optional.get();
            final Buffer trimmed = buffer.slice(buffer.capacity() - code.getEncodedSize());

            if (code.getCode().isFinal()) {
                final AtCommand cmd = data.consumeCurrentCommand();

                // TODO: this is stupid. We are saving the buffer because when we do
                // data.consumeAllData the latest data from the modem is what we are currently
                // processing and as such, it won't make it into the combined result.
                // For now, this is fine.
                data.stashBuffer(trimmed);

                // TODO: we also need to mark if the response is a success or not...
                final AtResponse response = AtResponse.success(cmd, data.consumeAllData());
                data.saveResponse(response);
            } else {
                data.stashBuffer(buffer);
            }
        } else {
            data.stashBuffer(buffer);
        }
    }

}
