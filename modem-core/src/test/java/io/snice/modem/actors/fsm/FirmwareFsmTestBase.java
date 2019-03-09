package io.snice.modem.actors.fsm;

import io.hektor.fsm.FSM;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.AtCommand;
import org.junit.Before;

import java.util.Collections;
import java.util.function.BiConsumer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FirmwareFsmTestBase extends FsmTestBase<FirmwareState, FirmwareContext, FirmwareData> {

    protected FSM<FirmwareState, FirmwareContext, FirmwareData> fsm;
    protected CachingFsmScheduler cachingFsmScheduler;
    protected FirmwareData data;
    protected FirmwareContext ctx;
    protected ModemConfiguration config;

    protected final BiConsumer<FirmwareState, Object> unhandledEventHandler = mock(BiConsumer.class);

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        this.cachingFsmScheduler = new CachingFsmScheduler();
        init();
    }

    protected void init() {
        init(AtCommand.of("ATZ"));
    }

    protected void init(final AtCommand... resetCommands) {
        if (resetCommands == null) {
            config = ModemConfiguration.of().withResetCommands(Collections.EMPTY_LIST).build();
        } else {
            config = ModemConfiguration.of().withResetCommands(resetCommands).build();
        }
        ctx = mockFirmwareContext(cachingFsmScheduler, config);
        init(ctx);
    }

    protected void init(final FirmwareContext ctx) {
        this.data = new FirmwareData();
        this.fsm = FirmwareFsm.definition.newInstance("unit-test-123", ctx, data, unhandledEventHandler, this::onTransition);
        this.fsm.start();
    }

    protected FirmwareContext mockFirmwareContext(final CachingFsmScheduler scheduler, final ModemConfiguration config) {
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
    protected void onAtCommand(final String cmd) {
        fsm.onEvent(AtCommand.of(cmd));
        verify(ctx).writeToModem(AtCommand.of(cmd));
        assertThat(fsm.getState(), is(FirmwareState.WAIT));
    }

}
