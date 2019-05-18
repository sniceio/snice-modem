package io.snice.modem.actors.fsm;

import io.hektor.fsm.FSM;
import io.hektor.fsm.Scheduler;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.AtCommand;
import org.junit.Before;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FirmwareFsmTestBase extends FsmTestBase<FirmwareState, FirmwareContext, FirmwareData> {

    protected FSM<FirmwareState, FirmwareContext, FirmwareData> fsm;
    protected Scheduler scheduler;
    protected FirmwareData data;
    protected FirmwareContext ctx;
    protected ModemConfiguration config;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        init();
    }

    protected void init() {
        init(AtCommand.of("ATZ"));
    }

    protected void init(final AtCommand... resetCommands) {
        config = ModemConfiguration.of().withResetCommands(resetCommands).build();
        init(config);
    }

    protected void init(final List<AtCommand> resetCommands) {
        config = ModemConfiguration.of().withResetCommands(resetCommands).build();
        init(config);
    }

    protected void init(final ModemConfiguration config) {
        scheduler = mock(Scheduler.class);
        ctx = mockFirmwareContext(scheduler, config);
        init(ctx);
    }

    protected void init(final FirmwareContext ctx) {
        this.data = new FirmwareData();
        this.fsm = FirmwareFsm.definition.newInstance("unit-test-123", ctx, data, unhandledEventHandler, this::onTransition);
        this.fsm.start();
    }

    protected FirmwareContext mockFirmwareContext(final Scheduler scheduler, final ModemConfiguration config) {
        final FirmwareContext ctx = mock(FirmwareContext.class);
        when(ctx.getConfiguration()).thenReturn(config);
        when(ctx.getScheduler()).thenReturn(scheduler);
        return ctx;
    }

    /**
     * Convenience method for sending an AT command to the FSM and
     * expect that it gets written to the modem and that we end
     * up in the WAIT state.
     * @param cmd
     */
    protected AtCommand onAtCommand(final String cmd) {
        final var at = AtCommand.of(cmd);
        fsm.onEvent(at);
        verify(ctx).writeToModem(at);
        assertState(FirmwareState.WAIT);
        return at;
    }

    protected void assertState(final FirmwareState expected) {
        assertThat(fsm.getState(), is(expected));
    }

    /**
     * Whenever we write a command to the modem we will also ALWAYS be scheduling a transaction timeout
     * for that command, in case it never actually finish. Ensure this is done correctly
     * always
     * @param cmd
     * @param timeout
     */
    protected void ensureCommandProcessing(final AtCommand cmd, final Duration timeout) {
        verify(ctx).writeToModem(cmd);

    }

    protected void ensureCommandProcessing(final String cmd, final Duration timeout) {
        ensureCommandProcessing(AtCommand.of(cmd), timeout);
    }

    protected void ensureCommandProcessing(final String cmd, final int timeout) {
        ensureCommandProcessing(AtCommand.of(cmd), Duration.ofMillis(timeout));
    }

}
