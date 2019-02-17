package io.snice.old_modem.command;

import io.snice.old_modem.Command;

public interface Info extends Command {

    String CMD = "ATI";

    /**
     * Get all INFO
     *
     * @return
     */
    static Info all() {
        return new DefaultInfo();
    }

    class DefaultInfo extends BaseCommand implements Info {
        protected DefaultInfo() {
            super(CMD);
        }

    }
}
