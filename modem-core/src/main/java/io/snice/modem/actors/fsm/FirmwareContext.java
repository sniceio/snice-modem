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

    /**
     * I'm not sure about this. May need to add something to the hektor for transitioning in code to another
     * state. Now you have to send the event back to yourself, which feels odd. Actually, perhaps we do this
     * on one success the actions but it is a bit odd because the action would have a side effect success building
     * this crap up. Feels even more wrong...
     *
     * So, emit the event on the transition back to READY? But who knows which actor it came from
     * to begin with?
     *
     * Actually, why not emit it here and send an event back to ourselves as well... nothing wrong with that...
     * Still feels odd though...
     *
     * ahhhh, perhaps INPUT is a transiet state to deal with this stuff.
     * Yes, it should be. I think. Let's go with WAITING (as in waiting for response from modem) and
     * PROCESSING (as in processing response). Processing is always a transent state.
     *
     * Update: May 5th, 2019
     * Alright, now that we have transient states, we should probably push the response
     * back to the caller based on the transaction instead of doing that in the  {@link io.snice.modem.actors.ModemFirmwareActor}
     *
     * @param response
     */
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
