package io.snice.old_modem.command;

import io.snice.old_modem.Command;

public interface AtCommand extends Command {

    static AtCommand of(final String cmd) throws IllegalArgumentException {
        if (cmd == null || cmd.isEmpty()) {
            throw new IllegalArgumentException("The command cannot be null or the empty string");
        }

        if (!cmd.toUpperCase().startsWith("AT")) {
            return new DefaultAtCommand("AT" + cmd);
        }

        return new DefaultAtCommand(cmd);
    }

    class DefaultAtCommand extends BaseCommand implements AtCommand {
        private DefaultAtCommand(String cmd) {
            super(cmd);
        }
    }
}
