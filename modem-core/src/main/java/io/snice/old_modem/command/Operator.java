package io.snice.old_modem.command;

import io.snice.old_modem.Command;

public interface Operator extends Command {

    String CMD = "AT+COPS";

    static Operator listAll() {
        return new Cops("=?");
    }

    static Operator current() {
        return new Cops("?");
    }

    class Cops extends BaseCommand implements Operator {
        private Cops(final String cmd) {
            super(CMD + cmd);
        }

    }
}
