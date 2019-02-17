package io.snice.old_modem.command;

import io.snice.old_modem.Command;

/**
 * Command for returning the operational status of the modem.
 */
public interface Status extends Command {

    String CMD = "AT!GSTATUS?";

    /**
     * Get all INFO
     *
     * @return
     */
    static Status all() {
        return new DefaultStatus();
    }

    class DefaultStatus extends BaseCommand implements Status {
        protected DefaultStatus() {
            super(CMD);
        }

    }
}
