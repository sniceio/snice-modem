package io.snice.old_modem.command;

import io.snice.old_modem.Command;

public interface Echo extends Command {

    String CMD = "ATE";

    static Echo on() {
        return new EchoOn();
    }

    static Echo off() {
        return new EchoOff();
    }

    class EchoOn extends BaseCommand implements Echo {
        protected EchoOn() {
            super(CMD + 1);
        }

    }

    class EchoOff extends BaseCommand implements Echo {
        protected EchoOff() {
            super(CMD + 0);
        }
    }


}
