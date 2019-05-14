package io.snice.modem.actors.fsm;

import io.hektor.actors.io.IoEvent;
import io.hektor.core.ActorRef;
import io.hektor.fsm.Context;
import io.hektor.fsm.Scheduler;
import io.snice.modem.actors.ModemActor;
import io.snice.modem.actors.ModemConfiguration;
import io.snice.modem.actors.ModemFirmwareActor;
import io.snice.modem.actors.ModemManagerActor;
import io.snice.modem.actors.events.AtCommand;
import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.modem.ModemMessage;
import io.snice.modem.actors.messages.modem.ModemResponse;

public class FirmwareContext implements Context {

    private final Scheduler scheduler;
    private final ModemConfiguration config;
    private final ActorRef parent;
    private final ActorRef self;
    private final ActorRef modemWriteStream;

    public FirmwareContext(final Scheduler scheduler, final ModemConfiguration config, final ActorRef parent, final ActorRef self, final ActorRef modemWriteStream) {
        this.scheduler = scheduler;
        this.config = config;
        this.parent = parent;
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

    /**
     * In general, the {@link ModemActor} will send the {@link ModemFirmwareActor} {@link ModemMessage}s that
     * will at some point be replied to and it is up to the {@link ModemActor} to dispatch them to the original
     * caller. The {@link FirmwareFsm} will dispatch those responses via this method.
     *
     * @param response
     */
    public void dispatchResponse(final ModemResponse response) {
        parent.tell(response, self);
    }

    public ModemConfiguration getConfiguration() {
        return config;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }
}
