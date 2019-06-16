package io.snice.modem.actors.fsm;

import io.hektor.actors.io.StreamToken;
import io.hektor.core.LifecycleEvent;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.builder.StateBuilder;
import io.snice.buffer.Buffer;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.events.ModemDisconnect;
import io.snice.modem.actors.events.TransactionTimeout;
import io.snice.modem.actors.messages.modem.ModemResetRequest;

import java.util.Optional;

import static io.snice.modem.actors.fsm.FirmwareState.PROCESSING;
import static io.snice.modem.actors.fsm.FirmwareState.READY;
import static io.snice.modem.actors.fsm.FirmwareState.RESET;
import static io.snice.modem.actors.fsm.FirmwareState.TERMINATED;
import static io.snice.modem.actors.fsm.FirmwareState.WAIT;

public class FirmwareFsm {

    public static final Definition<FirmwareState, FirmwareContext, FirmwareData> definition;

    static {
        final var builder = FSM.of(FirmwareState.class).ofContextType(FirmwareContext.class).withDataType(FirmwareData.class);

        final var ready = builder.withInitialState(READY);
        stateDefinitionsReset(builder.withTransientState(RESET));
        stateDefinitionWait(builder.withState(WAIT));
        stateDefinitionProcessing(builder.withTransientState(PROCESSING));
        final var terminated = builder.withFinalState(TERMINATED);

        ready.transitionTo(RESET)
                .onEvent(ModemResetRequest.class)
                .withGuard(FirmwareFsm::isConfiguredWithResetCommands)
                .withAction((req, ctx, data) -> data.saveResetRequest(req));
        ready.transitionTo(READY).onEvent(ModemResetRequest.class).withAction(FirmwareFsm::onEmptyReset);
        ready.transitionTo(READY).onEvent(StreamToken.class).withAction(FirmwareFsm::processUnsolicitedData);
        ready.transitionTo(WAIT).onEvent(AtCommand.class).withAction(FirmwareFsm::writeToModem);
        ready.transitionTo(TERMINATED).onEvent(LifecycleEvent.Terminated.class).withAction(e -> System.err.println("Got Terminated for who while in READY state: " + e.getActor()));
        ready.transitionTo(TERMINATED).onEvent(ModemDisconnect.class);

        // terminated.transitionTo(TERMINATED).onEvent(LifecycleEvent.Terminated.class)
                // .withAction(e -> System.err.println("Got Terminated for who while in TERMINATED state: " + e.getActor()));


        definition = builder.build();
    }

    /**
     * We can get, at any point, unsolicited data from the modem. We need to try to interpret that data
     * and send it to the parent, who will decide what to do with it. If we are unable to intepret the data
     * we'll say so. We'd like to avoid the upper layers having to parse the data from the modem, since it really
     * should be in a single place.
     * @param token
     * @param ctx
     * @param data
     */
    private static final void processUnsolicitedData(final StreamToken token, final FirmwareContext ctx, final FirmwareData data) {
        System.err.println("Got StreamToken while in READY " + token.getBuffer().toString());
    }

    private static void stateDefinitionsReset(final StateBuilder<FirmwareState, FirmwareContext, FirmwareData> reset) {
        reset.withEnterAction(FirmwareFsm::onEnterReset);

        reset.transitionTo(WAIT)
                .onEvent(ModemResetRequest.class)
                .withGuard((r, ctx, data) -> data.isResetting())
                .withAction(FirmwareFsm::sendResetCommand);

        // this is because when we go from PROCESSING to RESET again we will still carry
        // the StreamToken event class. We don't necessarily care about that one since
        // it should have been converted into a AtResponse but Hektor doesn't support
        // that right now...
        // Note that the "conversion" of the StreamToken has already been done in the processing
        // step and has been saved away on the transition to RESET again. Trace: {@link FirmwareData#saveResetResponse}
        reset.transitionTo(WAIT)
                .onEvent(StreamToken.class)
                .withGuard((r, ctx, data) -> data.isResetting())
                .withAction(FirmwareFsm::sendResetCommand);

        reset.transitionTo(READY).onEvent(TransactionTimeout.class).withAction(e -> System.err.println("Timeout: " + e));

        // Once we are done sending all reset commands, we'll transition back to READY.
        reset.transitionTo(READY).asDefaultTransition().withAction(FirmwareFsm::onResetCompleted);

    }

    private static void onResetCompleted(final Object ignore, final FirmwareContext ctx, final FirmwareData data) {
        final var resetReq = data.consumeResetRequest()
                .orElseThrow(() -> new IllegalStateException("Expected a " + ModemResetRequest.class
                        + " to be present but seems there is a bug. Must have lost it."));
        final var resetResp = resetReq.createSuccessResponse(data.consumeResetResponses());
        ctx.dispatchResponse(resetResp);
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
    }

    /**
     * If we are requested to RESET the modem but we have not been configured with any reset commands, then just
     * return with a success response reset back to the caller.
     */
    private static void onEmptyReset(final ModemResetRequest cmd, final FirmwareContext ctx, final FirmwareData data) {
        ctx.dispatchResponse(cmd.createSuccessResponse());
    }

    private static void sendResetCommand(final Object ignore, final FirmwareContext ctx, final FirmwareData data) {
        final var optional = data.getNextResetCommand();
        data.isResetting(optional.isPresent());
        optional.ifPresent(cmd -> writeToModem(cmd, ctx, data));
    }

    private static void writeToModem(final AtCommand cmd, final FirmwareContext ctx, final FirmwareData data) {
        final var delay = ctx.getConfiguration().getCommandConfiguration().getTimeout(cmd);
        final var timeout = TransactionTimeout.of(cmd);

        final var timer = ctx.getScheduler().schedule(() -> timeout, delay);
        data.setTransactionTimer(timer);

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

        wait.transitionTo(PROCESSING).onEvent(StreamToken.class).withAction((token, ctx, data) -> data.saveStreamToken(token));
        wait.transitionTo(WAIT).onEvent(AtCommand.class).withAction((cmd, ctx, data) -> data.stashCommand(cmd));

        wait.transitionTo(READY)
                .onEvent(TransactionTimeout.class)
                .withGuard((timeout, ctx, data) -> data.isCurrentTransactionTimer(timeout))
                .withAction(FirmwareFsm::processTransactionTimeout);
    }

    private static void processTransactionTimeout(final TransactionTimeout timeout, final FirmwareContext ctx, final FirmwareData data) {
        data.cancelTransactionTimer();
        data.consumeCurrentCommand();
        ctx.dispatchResponse(timeout);
    }


    /**
     * While in the processing state we are just waiting for data from the modem and if that data now completes
     * the outstanding command then we have two choices, go to the RESET state (if we are in the reset loop) or back
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
        processing.transitionTo(RESET)
                .onEvent(StreamToken.class)
                .withGuard((token, ctx, data) -> data.hasFinalResponse() && data.isResetting())
                .withAction((token, ctx, data) -> {
                    data.cancelTransactionTimer();
                    data.saveResetResponse(data.consumeResponse());
                });

        // if we have stashed commands we have to send those out
        // and once again end up in the WAIT <--> PROCESSING loop
        processing.transitionTo(WAIT)
                .onEvent(StreamToken.class)
                .withGuard((token, ctx, data) -> data.hasFinalResponse() && data.hasStashedCommands())
                .withAction((token, ctx, data) -> {
                    ctx.dispatchResponse(data.consumeResponse());
                    data.getNextStashedCommand().ifPresent(cmd -> {
                        data.setCurrentCommand(cmd);
                        ctx.writeToModem(cmd);
                    });
                });

        processing.transitionTo(READY)
                .onEvent(StreamToken.class)
                .withGuard((token, ctx, data) -> data.hasFinalResponse())
                .withAction((token, ctx, data) -> {
                    data.cancelTransactionTimer();
                    ctx.dispatchResponse(data.consumeResponse());
                });

        processing.transitionTo(WAIT).asDefaultTransition();
    }

    private static boolean isConfiguredWithResetCommands(final ModemResetRequest request, final FirmwareContext ctx, final FirmwareData data){
        return ctx.getConfiguration().hasResetCommands();
    }

    private static void processStreamToken(final FirmwareContext ctx, final FirmwareData data) {

        // TODO: as mentioned earlier, would be nicer if we got the actual event here as well so we
        // TODO:  don't have to save it away and then "consumeStreamToken". Kind of annoying
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
                final AtResponse response = cmd.successResponse(data.consumeAllData());
                data.saveResponse(response);
            } else {
                data.stashBuffer(buffer);
            }
        } else {
            data.stashBuffer(buffer);
        }
    }

}
