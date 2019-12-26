package io.snice.modem.fsm;

import io.hektor.fsm.Definition;
import io.hektor.fsm.FSM;

import static io.snice.modem.fsm.ModemManagerState.INIT;
import static io.snice.modem.fsm.ModemManagerState.RUNNNG;
import static io.snice.modem.fsm.ModemManagerState.TERMINATED;

public class ModemManagerFsm {

    public static final Definition<ModemManagerState, ModemManagerContext, ModemManagerData> definition;

    static {
        final var builder = FSM.of(ModemManagerState.class)
                .ofContextType(ModemManagerContext.class)
                .withDataType(ModemManagerData.class);

        final var init = builder.withInitialState(INIT);
        final var running = builder.withState(RUNNNG);
        final var terminated = builder.withFinalState(TERMINATED);

        init.withEnterAction((ctx, data) -> ctx.subscribe());
        init.transitionTo(RUNNNG).asDefaultTransition();

        running.transitionTo(TERMINATED).onEvent(String.class);

        definition = builder.build();
    }
}
