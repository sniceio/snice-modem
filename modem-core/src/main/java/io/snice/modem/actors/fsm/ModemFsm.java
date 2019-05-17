package io.snice.modem.actors.fsm;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.builder.FSMBuilder;
import io.hektor.fsm.builder.StateBuilder;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.FirmwareCreatedEvent;
import io.snice.modem.actors.events.ModemConnectFailure;
import io.snice.modem.actors.events.ModemConnectSuccess;
import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.messages.modem.ModemRequest;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import io.snice.modem.actors.messages.modem.ModemResetResponse;
import io.snice.modem.actors.messages.modem.ModemResponse;

import java.time.Duration;

public class ModemFsm {

    public static final Definition<ModemState, ModemContext, ModemData> definition;

    static {
        final FSMBuilder<ModemState, ModemContext, ModemData> builder =
                FSM.of(ModemState.class).ofContextType(ModemContext.class).withDataType(ModemData.class);

        stateDefinitionsConnecting(builder.withInitialState(ModemState.CONNECTING));

        stateDefinitionsFirmware(builder.withState(ModemState.FIRMWARE));

        stateDefinitionsConnected(builder.withTransientState(ModemState.CONNECTED));

        stateDefinitionsReset(builder.withState(ModemState.RESET));

        stateDefinitionsReady(builder.withState(ModemState.READY));

        stateDefinitionsCmd(builder.withState(ModemState.CMD));

        stateDefinitionsDisconnecting(builder.withState(ModemState.DISCONNECTING));
        stateDefinitionsTerminated(builder.withFinalState(ModemState.TERMINATED));

        definition = builder.build();
    }

    /**
     * These are all the state transitions from the CONNECTING state.
     *
     * @param connecting
     */
    private static void stateDefinitionsConnecting(final StateBuilder<ModemState, ModemContext, ModemData> connecting) {

        // TODO: change, just call the context instead...
        connecting.withEnterAction(ModemFsm::onConnectingEnterAction);

        connecting.transitionTo(ModemState.FIRMWARE).onEvent(ModemMessage.class).withGuard(ModemMessage::isConnectSuccessEvent);

        // eventually we'll retry different baud rates and what not but for now, we'll just
        // give up right away because we have no patience...
        connecting.transitionTo(ModemState.DISCONNECTING).onEvent(ModemMessage.class).withGuard(ModemMessage::isConnectFailureEvent);
    }

    /**
     * The {@link ModemState#FIRMWARE} is for setting up the necessary underlying firmware FSM
     * and we may pick different ones depending on what we know about the underlying modem etc.
     *
     * Currently, it'll always be a generic modem.
     *
     * @param firmware
     */
    private static void stateDefinitionsFirmware(final StateBuilder<ModemState, ModemContext, ModemData> firmware) {
        firmware.withEnterAction((ctx, data) -> ctx.createFirmware(data.getDesiredFirmware()));
        firmware.transitionTo(ModemState.CONNECTED).onEvent(FirmwareCreatedEvent.class);
    }

    /**
     * These are all the state transitions from the connected state.
     *
     * TODO: not sure I really need the connected state. Perhaps we should send ATE1 and ATV1 here to ensure
     * we get all the echo stuff etc that we do expect? But then again, that is also what the RESET state
     * is for so not sure...
     *
     * @param connected
     */
    private static void stateDefinitionsConnected(final StateBuilder<ModemState, ModemContext, ModemData> connected) {
        connected.withEnterAction(ModemFsm::onConnectedEnterAction);
        connected.transitionTo(ModemState.READY).onEvent(String.class).withGuard("TIMEOUT_CONNECTED"::equals);

        connected.transitionTo(ModemState.RESET).asDefaultTransition();
    }


    /**
     * The RESET state simply issues a RESET to the modem and waits for a response. If there is a timeout,
     * it may decide to issue a PING to the modem, perhaps cut down on some of the RESET commands and
     * if necessary try and figure out what type of modem we're dealing with. Perhaps we need to go back to the
     * FIRMWARE state to pick a new type of underlying firmware.
     *
     * Note: I haven't added all the above. Right now, it'll just go to READY on ModemResetReponse
     *
     * @param resetting
     */
    private static void stateDefinitionsReset(final StateBuilder<ModemState, ModemContext, ModemData> resetting) {
        resetting.withEnterAction((ctx, data) -> ctx.send(ModemResetRequest.of()));
        resetting.transitionTo(ModemState.READY).onEvent(ModemResetResponse.class);

        resetting.transitionTo(ModemState.CMD).onEvent(ModemResetResponse.class);
    }

    /**
     * While in the {@link ModemState#READY} state, we will sit and wait for any kind of {@link ModemRequest}
     * and simply pass that onto the underlying modem. All these requests are done in transactions so we will
     * be waiting for this request to complete before we write anything else to the modem.
     *
     * Note that a request can timeout but that should be handed by the underlying {@link FirmwareFsm}. Also note
     * that we may receive unsolicited events from the modem while in any state.
     *
     * @param ready
     */
    private static void stateDefinitionsReady(final StateBuilder<ModemState, ModemContext, ModemData> ready) {
        ready.withEnterAction(ModemFsm::onReadyEnterAction);

        ready.transitionTo(ModemState.CMD).onEvent(ModemRequest.class).withAction(ModemFsm::processRequest);

        ready.transitionTo(ModemState.DISCONNECTING).onEvent(String.class).withGuard("TIMEOUT_READY"::equals);
    }

    /**
     * Whenever we are actually sending a request to the modem we will also take a note regarding the
     * outstanding transaction. We will also save some meta data about the time it was sent etc
     * so that we can later re-play the scenario if we want to.
     */
    private static void processRequest(final ModemRequest req, final ModemContext ctx, final ModemData data) {
        data.saveTransaction(req);
        ctx.send(req);
    }

    /**
     * While in the {@link ModemState#CMD} we will sit and wait for new responses to the current outstanding
     * transaction.
     *
     * @param command
     */
    private static void stateDefinitionsCmd(final StateBuilder<ModemState, ModemContext, ModemData> command) {

        command.transitionTo(ModemState.READY)
                .onEvent(ModemResponse.class)
                .withGuard((resp, ctx, data) -> data.matchTransaction(resp))
                .withAction(ModemFsm::processTransaction);

        // Note that this means that we received a response for a non matching transaction.
        command.transitionTo(ModemState.READY).onEvent(ModemResponse.class);
    }

    /**
     * Once we find a matching transaction we'll process it, which just means to reset the current
     * outstanding one (should be this one) and give it to the context.
     */
    private static void processTransaction(final ModemResponse resp, final ModemContext ctx, final ModemData data) {
        data.consumeTransaction();
        ctx.onResponse(resp);
    }

    /**
     * These are all the state transitions from the DISCONNECTING state.
     *
     * @param disconnecting
     */
    private static void stateDefinitionsDisconnecting(final StateBuilder<ModemState, ModemContext, ModemData> disconnecting) {
        disconnecting.withEnterAction(ModemFsm::onDisconnectingEnterAction);
        disconnecting.transitionTo(ModemState.TERMINATED).onEvent(String.class).withGuard("TIMEOUT"::equals);
    }

    /**
     * These are all the state transitions from the TERMINATED state.
     *
     * @param terminated
     */
    private static void stateDefinitionsTerminated(final StateBuilder<ModemState, ModemContext, ModemData> terminated) {
    }

    /**
     * Upon entering connecting we will actually submit a job to try and connect to
     * the actual modem.
     *
     * TODO: we shouldn't do this in the FSM. We will ask the ctx to e.g. connect/disconnect etc.
     *
     * @param ctx
     * @param data
     */

    private static void onConnectingEnterAction(final ModemContext ctx, final ModemData data) {
        final SerialPort port = ctx.getPort();
        final ModemConfiguration config = ctx.getConfig();
        ctx.runJob(() -> {
            port.setBaudRate(config.getBaudRate());
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, config.getReadTimeout(), 0);
            if (!port.openPort()) {
                // TODO: check if we have permissions etc... Need to use the new Java 9 Process API
                return ModemConnectFailure.of();
            }

            return ModemConnectSuccess.of();
        });
    }

    /**
     *
     * @param ctx
     * @param data
     */
    private static void onReadyEnterAction(final ModemContext ctx, final ModemData data) {
        ctx.getScheduler().schedule(() -> "READY_CONNECTED", Duration.ofMillis(100));
    }

    /**
     *
     * @param ctx
     * @param data
     */
    private static void onConnectedEnterAction(final ModemContext ctx, final ModemData data) {
        ctx.getScheduler().schedule(() -> "TIMEOUT_CONNECTED", Duration.ofMillis(100));
    }

    /**
     *
     * @param ctx
     * @param data
     */
    private static void onDisconnectingEnterAction(final ModemContext ctx, final ModemData data) {
        ctx.getScheduler().schedule(() -> "TIMEOUT", Duration.ofMillis(250));
    }

}
