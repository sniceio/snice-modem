package io.snice.modem.actors.messages.modem;

import io.snice.modem.actors.events.AtResponse;
import io.snice.modem.actors.messages.management.impl.TransactionMessageImpl;

import java.util.List;

public interface ModemResetResponse extends ModemResponse {

    /**
     * Obtain the list of reset commands that were used, or rather their responses, from which
     * you can get the actual commands via {@link AtResponse#getCommand()}.
     *
     * @return the list of reset commands that were used to reset the modem, or an empty list
     * if we were configured with zero reset commands.
     */
    List<AtResponse> getResetCommands();
}
