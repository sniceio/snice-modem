package io.snice.old_modem.event;

import io.snice.old_modem.Command;

public interface WriteEvent extends Event {

    default boolean isWrite() {
        return true;
    }

    /**
     * The command that we are trying to write.
     *
     * @return
     */
    Command getCommand();
}
