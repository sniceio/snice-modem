package io.snice.modem.actors.fsm;

import io.hektor.actors.io.IoEvent;
import io.hektor.core.ActorRef;
import io.hektor.fsm.Context;
import io.hektor.fsm.Scheduler;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;

public class FirmwareContext implements Context {

    private final Scheduler scheduler;
    private final ModemConfiguration config;
    private final ActorRef self;
    private final ActorRef modemWriteStream;

    public FirmwareContext(final Scheduler scheduler, final ModemConfiguration config, final ActorRef self, final ActorRef modemWriteStream) {
        this.scheduler = scheduler;
        this.config = config;
        this.self = self;
        this.modemWriteStream = modemWriteStream;
    }

    /**
     * Write the command to the actual modem.
     *
     * @param cmd
     */
    public void writeToModem(final AtCommand cmd) {
        modemWriteStream.tell(IoEvent.writeEvent(cmd.getCommand()), self);
    }

    public void processResponse(final AtResponse response) {
        self.tell(response, self);
    }

    public ModemConfiguration getConfiguration() {
        return config;
    }


    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
