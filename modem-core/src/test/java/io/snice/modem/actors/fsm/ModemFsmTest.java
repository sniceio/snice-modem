package io.snice.modem.actors.fsm;

import io.hektor.fsm.FSM;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.FirmwareCreatedEvent;
import io.snice.modem.actors.events.ModemConnectSuccess;
import io.snice.modem.actors.messages.modem.ModemRequest;
import io.snice.modem.actors.messages.modem.ModemResetRequest;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModemFsmTest extends FsmTestBase<ModemState, ModemContext, ModemData> {

    private FSM<ModemState, ModemContext, ModemData> fsm;

    private CachingFsmScheduler cachingFsmScheduler;
    private ModemData data;
    private ModemContext ctx;
    private ModemConfiguration config;

    @Override
    @Before
    public void setup() throws Exception {
        init();
    }

    private void init() {
        config = ModemConfiguration.of().build();
        cachingFsmScheduler = mock(CachingFsmScheduler.class);
        ctx = mockModemContext(cachingFsmScheduler, config);
        init(ctx);
    }

    private void init(final ModemContext ctx) {
        this.data = new ModemData();
        this.fsm = ModemFsm.definition.newInstance("unit-test-123", ctx, data, unhandledEventHandler, this::onTransition);
        this.fsm.start();
    }

    private ModemContext mockModemContext(final CachingFsmScheduler scheduler, final ModemConfiguration config) {
        final var ctx = mock(ModemContext.class);
        when(ctx.getConfig()).thenReturn(config);
        when(ctx.getScheduler()).thenReturn(scheduler);
        return ctx;
    }

    /**
     * When we enter the connecting state, we'll try and attach to the USB modem.
     * and when successful, we'll transition over to the firmware state, where we'll
     * try and create a corresponding firmware fsm (which will happen in the context,
     * outside of the ModemFsm)
     */
    @Test
    public void testConnectingState() {
        transitionToFirmware();
    }

    /**
     * When we have an outstanding {@link ModemRequest} we will sit in the {@link ModemState#CMD}
     * and wait for the transaction to finish.
     *
     */
    @Test
    public void testCmdFinishTransaction() {
        transitionToReady();

        final var cmd = AtCommand.of("ATI");
        final var transaction = mock(ModemContext.Transaction.class);
        when(transaction.getTransactionId()).thenReturn(cmd.getTransactionId());
        when(transaction.getRequest()).thenReturn(cmd);
        when(ctx.newTransaction(cmd)).thenReturn(transaction);

        fsm.onEvent(cmd);
        assertState(ModemState.CMD);
        verify(ctx).send(cmd);

        // and we "receive" a response
        final var resp = cmd.successResponse("hello");
        fsm.onEvent(resp);
        verify(ctx).onResponse(transaction, resp);
    }

    /**
     * We will transition to various states the happy path and once we reach that
     * state we'll do the proper testing of all variants out of that state.
     * This helper method takes us to the firmware state.
     */
    private void transitionToFirmware() {
        fsm.onEvent(ModemConnectSuccess.of());
        assertState(ModemState.FIRMWARE);
        verify(ctx).createFirmware(ModemData.FirmwareType.GENERIC);
    }

    /**
     * Note that as we transition to RESET we will pass the transient state
     * CONNECTING (still not sure we really need this state, connecting that is)
     */
    private void transitionToReset() {
        transitionToFirmware();
        fsm.onEvent(FirmwareCreatedEvent.of());
        assertEventClass(ModemResetRequest.class);

        // note that because the CONNECTED state is a transient state
        // we'll transition over to RESET right away...
        assertState(ModemState.RESET);
    }

    private void transitionToReady() {
        transitionToReset();
        fsm.onEvent(ModemResetRequest.of().createSuccessResponse());
        assertState(ModemState.READY);
    }

    private <T extends ModemRequest> void assertEventClass(final Class<T> clazz) {
        verify(ctx).send(isA(clazz));
    }

    private void assertState(final ModemState expected) {
        assertThat(fsm.getState(), CoreMatchers.is(expected));
    }

}
