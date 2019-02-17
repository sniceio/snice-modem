package io.snice.modem.actors.fsm;

import io.hektor.fsm.Context;
import io.hektor.fsm.Scheduler;

public class FirmwareContext implements Context {

    private final Scheduler scheduler;

    public FirmwareContext(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
