package io.snice.modem.actors.fsm;

import io.hektor.actors.io.StreamToken;
import io.hektor.fsm.FSM;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.ModemReset;
import org.junit.Before;
import org.junit.Test;

import java.util.function.BiConsumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FirmwareFsmTest extends FsmTestBase<FirmwareState, FirmwareContext, FirmwareData> {

    private FSM<FirmwareState, FirmwareContext, FirmwareData> fsm;
    private CachingFsmScheduler cachingFsmScheduler;
    private FirmwareData data;
    private FirmwareContext ctx;
    private ModemConfiguration config;

    private final BiConsumer<FirmwareState, Object> unhandledEventHandler = mock(BiConsumer.class);

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        this.cachingFsmScheduler = new CachingFsmScheduler();
        init();
    }

    private void init() {
        init(AtCommand.of("ATZ"));
    }

    private void init(final AtCommand... resetCommands) {
        config = ModemConfiguration.of().withResetCommands(resetCommands).build();
        ctx = mockFirmwareContext(cachingFsmScheduler, config);
        init(ctx);
    }

    private void init(final FirmwareContext ctx) {
        this.data = new FirmwareData();
        this.fsm = FirmwareFsm.definition.newInstance("unit-test-123", ctx, data, unhandledEventHandler, this::onTransition);
        this.fsm.start();
    }

    private FirmwareContext mockFirmwareContext(final CachingFsmScheduler scheduler, final ModemConfiguration config) {
        final FirmwareContext ctx = mock(FirmwareContext.class);
        when(ctx.getConfiguration()).thenReturn(config);
        when(ctx.getScheduler()).thenReturn(scheduler);
        return ctx;
    }

    /**
     * Test and verify that the reset cycle is working.
     * @throws Exception
     */
    @Test
    public void testFirmwareFsmInit() throws Exception {
        init(AtCommand.of("ATZ"), AtCommand.of("ATV1"), AtCommand.of("ATI"));

        fsm.onEvent(ModemReset.of());

        verify(ctx).writeToModem(AtCommand.of("ATZ"));
        fsm.onEvent(StreamToken.of("ATZ\r\n"));
    }

}
