package io.snice.modem.actors.fsm;

import com.fazecast.jSerialComm.SerialPort;
import io.hektor.core.LifecycleEvent;
import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;
import io.hektor.fsm.builder.FSMBuilder;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.FirmwareCreatedEvent;
import io.snice.modem.actors.events.ModemConnectFailure;
import io.snice.modem.actors.events.ModemConnectSuccess;
import io.snice.modem.actors.events.TransactionTimeout;
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

        final var connecting = builder.withInitialState(ModemState.CONNECTING);
        final var firmware = builder.withState(ModemState.FIRMWARE);
        final var connected = builder.withTransientState(ModemState.CONNECTED);
        final var reset = builder.withState(ModemState.RESET);
        final var ready = builder.withState(ModemState.READY);
        final var command = builder.withState(ModemState.CMD);
        final var disconnecting = builder.withState(ModemState.DISCONNECTING);
        final var terminated = builder.withFinalState(ModemState.TERMINATED);

        connecting.withEnterAction(ModemFsm::onConnectingEnterAction); // TODO: change, just call the context instead...
        connecting.transitionTo(ModemState.FIRMWARE).onEvent(ModemMessage.class).withGuard(ModemMessage::isConnectSuccessEvent);
        connecting.transitionTo(ModemState.DISCONNECTING).onEvent(ModemMessage.class).withGuard(ModemMessage::isConnectFailureEvent);

        firmware.withEnterAction((ctx, data) -> ctx.createFirmware(data.getDesiredFirmware()));
        firmware.transitionTo(ModemState.CONNECTED).onEvent(FirmwareCreatedEvent.class);

        connected.transitionTo(ModemState.READY).onEvent(String.class).withGuard("TIMEOUT_CONNECTED"::equals);
        connected.transitionTo(ModemState.RESET).asDefaultTransition();

        reset.withEnterAction((ctx, data) -> ctx.send(ModemResetRequest.of()));
        reset.transitionTo(ModemState.READY).onEvent(ModemResetResponse.class);
        reset.transitionTo(ModemState.READY).onEvent(TransactionTimeout.class);

        ready.transitionTo(ModemState.CMD).onEvent(ModemRequest.class).withAction(ModemFsm::processRequest);
        ready.transitionTo(ModemState.DISCONNECTING).onEvent(LifecycleEvent.Terminated.class);
        ready.transitionTo(ModemState.DISCONNECTING).onEvent(String.class).withGuard("TIMEOUT_READY"::equals);

        command.transitionTo(ModemState.READY)
                .onEvent(ModemResponse.class)
                .withGuard((resp, ctx, data) -> data.matchTransaction(resp))
                .withAction(ModemFsm::processTransaction);
        command.transitionTo(ModemState.READY).onEvent(ModemResponse.class);

        disconnecting.withEnterAction((ctx, data) -> {
            ctx.shutdownPort();
            ctx.getScheduler().schedule(() -> "TIMEOUT", Duration.ofSeconds(5));

        });
        disconnecting.transitionTo(ModemState.TERMINATED).onEvent(String.class).withGuard("TIMEOUT"::equals);

        definition = builder.build();
    }

    /**
     * Whenever we are actually sending a request to the modem we will also take a note regarding the
     * outstanding transaction. We will also save some meta data about the time it was sent etc
     * so that we can later re-play the scenario if we want to.
     */
    private static void processRequest(final ModemRequest req, final ModemContext ctx, final ModemData data) {
        final var transaction = ctx.newTransaction(req);
        data.saveTransaction(transaction);
        ctx.send(req);
    }

    /**
     * Once we find a matching transaction we'll process it, which just means to reset the current
     * outstanding one (should be this one) and give it to the context.
     */
    private static void processTransaction(final ModemResponse resp, final ModemContext ctx, final ModemData data) {
        final var transaction = data.consumeTransaction();
        ctx.onResponse(transaction, resp);
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

}
