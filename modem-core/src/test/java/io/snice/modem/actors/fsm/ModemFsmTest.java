package io.snice.modem.actors.fsm;

import io.hektor.fsm.FSM;
import io.snice.modem.actors.ModemConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
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
        this.cachingFsmScheduler = new CachingFsmScheduler();
        init();
    }

    private void init() {
        config = ModemConfiguration.of().build();
        ctx = mockModemContext(cachingFsmScheduler, config);
        init(ctx);
    }

    private void init(final ModemContext ctx) {
        this.data = new ModemData();
        this.fsm = ModemFsm.definition.newInstance("unit-test-123", ctx, data, unhandledEventHandler, this::onTransition);
        this.fsm.start();
    }

    private ModemContext mockModemContext(final CachingFsmScheduler scheduler, final ModemConfiguration config) {
        final ModemContext ctx = mock(ModemContext.class);
        // when(ctx.getConfiguration()).thenReturn(config);
        when(ctx.getScheduler()).thenReturn(scheduler);
        return ctx;
    }

    @Test
    public void testInit() {

    }

}
