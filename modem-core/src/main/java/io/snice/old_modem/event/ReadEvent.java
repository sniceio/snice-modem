package io.snice.old_modem.event;

import io.snice.old_modem.Command;

import java.util.List;
import java.util.stream.Collectors;

public interface ReadEvent extends Event {

    default boolean isRead() {
        return true;
    }

    /**
     * Whether or not this read event is a result of a command that we issued.
     * Typically, we may initiate a command to have the modem do something, or return
     * some data we are interested in, such as the ATI command. This command will obtain
     * some all about the modem etc so the read event would be the result of that command.
     *
     * @return
     */
    default boolean isCommandResult() {
        return false;
    }

    default Command getCommand() {
        // TODO: perhaps some other exception stuff.
        throw new IllegalArgumentException("This is not a read event that was the result of a command being issued");
    }

    /**
     * Get the raw un-parsed data off of the serial port.
     *
     * @return
     */
    List<String> getLines();

    /**
     * Return the number of bytes we consumed off of the port
     * @return
     */
    int getBytesConsumed();

    default String getRawAsString() {
        return getLines().stream().collect(Collectors.joining());
    }
}
