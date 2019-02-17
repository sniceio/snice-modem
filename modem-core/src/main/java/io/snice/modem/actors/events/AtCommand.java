package io.snice.modem.actors.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.snice.buffer.Buffer;
import io.snice.buffer.Buffers;

public class AtCommand extends ModemEvent {

    private final Buffer command;

    @JsonCreator
    public static AtCommand of(final String cmd) {
        return new AtCommand(Buffers.wrap(cmd));
    }

    public static AtCommand of(final Buffer cmd) {
        return new AtCommand(cmd);
    }

    @Override
    public boolean isAtCommand() {
        return true;
    }

    private AtCommand(final Buffer command) {
        this.command = command;
    }

    public Buffer getCommand() {
        return command;
    }

}
