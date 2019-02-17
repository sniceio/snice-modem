package io.snice.old_modem.command;

import io.snice.old_modem.Command;

public interface PowerDown extends Command {

    String CMD = "AT!POWERDOWN";

    static PowerDown get() {
        return new DefaultCmd();
    }

    class DefaultCmd extends BaseCommand implements PowerDown {
        protected DefaultCmd() {
            super(CMD);
        }

    }
}
