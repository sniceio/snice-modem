package io.snice.modem.actors.fsm;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.builder.FSMBuilder;
import io.hektor.fsm.builder.StateBuilder;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.ModemConnectFailure;
import io.snice.modem.actors.events.ModemConnectSuccess;
import io.snice.modem.actors.events.ModemEvent;
import io.snice.modem.actors.events.ModemReset;

import java.time.Duration;

public class ModemFsm {

    public static final Definition<ModemState, ModemContext, ModemData> definition;

    static {
        final FSMBuilder<ModemState, ModemContext, ModemData> builder =
                FSM.of(ModemState.class).ofContextType(ModemContext.class).withDataType(ModemData.class);

        stateDefinitionsConnecting(builder.withInitialState(ModemState.CONNECTING));
        stateDefinitionsReset(builder.withState(ModemState.RESET));
        stateDefinitionsConnected(builder.withState(ModemState.CONNECTED));

        stateDefinitionsReady(builder.withState(ModemState.READY));

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
        connecting.withEnterAction(ModemFsm::onConnectingEnterAction);

        connecting.transitionTo(ModemState.RESET)
                .onEvent(ModemEvent.class)
                .withGuard(ModemEvent::isConnectSuccessEvent);

        // eventually we'll retry different baud rates and what not but for now, we'll just
        // give up right away because we have no patience...
        connecting.transitionTo(ModemState.DISCONNECTING).onEvent(ModemEvent.class).withGuard(ModemEvent::isConnectFailureEvent);
    }

    private static void stateDefinitionsReset(final StateBuilder<ModemState, ModemContext, ModemData> resetting) {
        resetting.withEnterAction((ctx, data) -> ctx.sendEvent(ModemReset.of()));
        resetting.transitionTo(ModemState.CONNECTED).onEvent(String.class);
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
    }

    /**
     * These are all the state transitions from the READY state.
     *
     * @param ready
     */
    private static void stateDefinitionsReady(final StateBuilder<ModemState, ModemContext, ModemData> ready) {
        ready.withEnterAction(ModemFsm::onReadyEnterAction);
        ready.transitionTo(ModemState.DISCONNECTING).onEvent(String.class).withGuard("TIMEOUT_READY"::equals);
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
